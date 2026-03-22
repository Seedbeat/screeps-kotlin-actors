package actor

import Root
import actor.enums.QueueMessageResult
import actor.message.BaseMessage
import actor.message.Message
import actor.message.Payload
import actor.message.Request
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import memory.actorKernelSnapshot
import screeps.api.Memory
import utils.CpuLogger
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

object ActorSystem : ILogging by Logging<ActorSystem>(LogLevel.WARN) {
    private val selfMarkId = this::class.simpleName!!
    private var currentTickState: TickState? = null

    fun actors(): Map<String, Actor> = ActorKernel.actors()

    fun contains(actorId: String) = ActorKernel.contains(actorId)
    fun remove(actorId: String) = ActorKernel.removeActor(actorId)
    fun snapshot(): KernelSnapshot = ActorKernel.snapshot()
    fun restore(snapshot: KernelSnapshot) {
        ActorKernel.restore(snapshot)
    }


    fun <T : Actor> spawn(actorId: String, create: (actorId: String) -> T): String? {
        if (contains(actorId)) {
            log.warn("Already spawned actor $actorId, return")
            return null
        }

        log.info("Spawn actor: $actorId")

        val actor = create(actorId)
        ActorKernel.registerActor(actor)
        actor.start()
        return actor.id
    }

    fun send(actorId: String, msg: Message): Boolean {
        log.info("[${msg.messageId}] '${msg.from}' -> '$actorId': ${msg.payload}")

        return when (ActorKernel.queueActorMessage(actorId, msg)) {
            QueueMessageResult.SCHEDULED_RESPONSE -> {
                log.debug("[${msg.messageId}] response -> scheduled continuation")
                true
            }

            QueueMessageResult.QUEUED_TO_MAILBOX -> {
                log.debug("[${msg.messageId}] queued to mailbox")
                true
            }

            QueueMessageResult.ACTOR_NOT_FOUND -> {
                log.warn("[${msg.messageId}] Actor '$actorId' not found")
                false
            }
        }
    }

    fun send(toActorId: String, fromActorId: String, payload: Payload, messageId: String? = null): Boolean {
        val id = messageId ?: MessageId.next()
        return send(toActorId, BaseMessage(id, fromActorId, payload))
    }

    suspend fun <T> request(toActorId: String, fromActorId: String, payload: Request): T {

        if (!contains(toActorId)) {
            throw ActorRequestException(toActorId, "target actor does not exist")
        }

        val messageId = MessageId.next()
        val message = BaseMessage(messageId, fromActorId, payload)

        @Suppress("UNCHECKED_CAST")
        return suspendCancellableCoroutine { cont ->
            ActorKernel.putPendingResponse(
                messageId = messageId,
                requesterActorId = fromActorId,
                targetActorId = toActorId,
                continuation = cont as CancellableContinuation<Any?>
            )
            cont.invokeOnCancellation { ActorKernel.removePendingResponse(messageId) }

            when (ActorKernel.queueActorMessage(toActorId, message)) {
                QueueMessageResult.SCHEDULED_RESPONSE ->
                    log.debug("[${messageId}] response -> scheduled continuation")

                QueueMessageResult.QUEUED_TO_MAILBOX ->
                    log.debug("[${messageId}] queued to mailbox")

                QueueMessageResult.ACTOR_NOT_FOUND -> {
                    log.warn("[${messageId}] Actor '$toActorId' not found")
                    ActorKernel.failPendingResponse(
                        messageId,
                        ActorRequestException(toActorId, "target actor disappeared before request was queued")
                    )
                }
            }
        }
    }

    fun onActorWaitingReceive(actor: Actor, continuation: CancellableContinuation<Message>) {
        ActorKernel.onActorWaitingReceive(actor, continuation)
    }

    fun removeActorReceiveWaiter(actorId: String) {
        ActorKernel.removeActorReceiveWaiter(actorId)
    }

    fun onActorWaitingNextTick(actor: Actor, ticks: Int, continuation: CancellableContinuation<Unit>) {
        ActorKernel.onActorWaitingNextTick(actor, ticks, continuation)
    }

    fun removeActorNextTickWaiter(actorId: String) {
        ActorKernel.removeActorNextTickWaiter(actorId)
    }

    fun onActorYield(actor: Actor, continuation: CancellableContinuation<Unit>) {
        ActorKernel.onActorYield(actor, continuation)
    }

    fun shouldYieldAtCheckpoint(): Boolean {
        val state = currentTickState ?: return false
        return state.shouldYieldSoon()
    }

    fun tick(maxSteps: Int = 100, cpuReserve: Double = 3.0) {

        CpuLogger.markStart(selfMarkId, parentId = null)

        if (Root.wasReset) {
            if (Memory.actorKernelSnapshot != null) {
                CpuLogger.mark(::restore) {
                    restore(Memory.actorKernelSnapshot!!)
                }
            } else {
                log.warn("No kernel snapshot available")
            }
        }

        MessageId.resetSeed()
        val state = TickState(maxSteps = maxSteps, cpuReserve = cpuReserve)
        currentTickState = state


        try {
            while (true) {
                if (state.checkIsStopped())
                    break

                state.rounds++

                val lastStepNumber = state.steps
                runTickRound(state)

                if (state.isStopped())
                    break

                if (state.steps == lastStepNumber) {
                    if (state.hasWork()) {
                        log.info("[Tick] Scheduler idle. Status: $state")
                    }
                    break
                }
            }

            if (state.isStopped() && state.hasWork()) {
                log.info("[Tick] Scheduler stop. Status: $state")
            }


            CpuLogger.mark(::snapshot) {
                Memory.actorKernelSnapshot = snapshot()
            }
        } finally {
            currentTickState = null
            CpuLogger.markEnd(selfMarkId)
        }
    }

    private fun runTickRound(state: TickState): Unit = CpuLogger.mark(::runTickRound) {
        wakeUpScheduledActors(state)
        flushScheduledContinuations(state)
        deliverMailboxMessages(state)
    }

    private fun wakeUpScheduledActors(state: TickState): Unit = CpuLogger.mark(::wakeUpScheduledActors) {
        state.increaseScheduledWakeUps(ActorKernel.wakeActorsReadyByTick())
    }

    private fun flushScheduledContinuations(state: TickState): Unit = CpuLogger.mark(::flushScheduledContinuations) {
        if (ActorKernel.flushScheduledContinuations()) {
            state.increaseFlushedContinuations()
        }
    }

    private fun deliverMailboxMessages(state: TickState): Unit = CpuLogger.mark(::deliverMailboxMessages) {
        while (true) {
            if (state.checkIsStopped())
                break

            if (!ActorKernel.deliverOneReadyActorMessage())
                break

            state.increaseDeliveredMessages()
        }
    }
}
