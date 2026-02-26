package actor

import actor.enums.QueueMessageResult
import actor.message.IMessage
import actor.message.IPayload
import actor.message.IRequest
import actor.message.Message
import kotlinx.coroutines.suspendCancellableCoroutine
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging
import kotlin.coroutines.Continuation

object ActorSystem : ILogging by Logging<ActorSystem>(LogLevel.DEBUG) {
    fun contains(actorId: String) = ActorKernel.contains(actorId)
    fun remove(actorId: String) = ActorKernel.removeActor(actorId)

    fun <T : Actor> spawn(actorId: String, create: (actorId: String) -> T) {
        if (contains(actorId)) {
            log.warn("Already spawned actor $actorId, return")
            return
        }

        log.info("Spawn actor: $actorId")

        val actor = create(actorId)
        ActorKernel.registerActor(actor)
        actor.start()
    }

    fun send(actorId: String, msg: IMessage) {
        log.info("[${msg.messageId}] '${msg.from}' -> '$actorId': ${msg.payload}")

        when (ActorKernel.queueActorMessage(actorId, msg)) {
            QueueMessageResult.SCHEDULED_RESPONSE ->
                log.debug("[${msg.messageId}] response -> scheduled continuation")

            QueueMessageResult.QUEUED_TO_MAILBOX ->
                log.debug("[${msg.messageId}] queued to mailbox")

            QueueMessageResult.ACTOR_NOT_FOUND ->
                log.warn("[${msg.messageId}] Actor '$actorId' not found")
        }
    }

    fun send(toActorId: String, fromActorId: String, payload: IPayload, messageId: String? = null) {
        val id = messageId ?: MessageUtils.generateMessageId()
        send(toActorId, Message(id, fromActorId, payload))
    }

    suspend fun <T> request(from: String, to: String, payload: IRequest): T {
        val messageId = MessageUtils.generateMessageId()

        @Suppress("UNCHECKED_CAST")
        return suspendCancellableCoroutine { cont ->
            ActorKernel.putPendingResponse(messageId, cont as Continuation<Any?>)
            cont.invokeOnCancellation { ActorKernel.removePendingResponse(messageId) }
            send(to, Message(messageId, from, payload))
        }
    }

    fun onActorWaitingReceive(actor: Actor, continuation: Continuation<IMessage>) {
        ActorKernel.onActorWaitingReceive(actor, continuation)
    }

    fun tick(maxSteps: Int = 100, cpuReserve: Double = 3.0) {

        MessageUtils.resetSeed()
        val state = TickState(maxSteps = maxSteps, cpuReserve = cpuReserve)

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
    }

    private fun runTickRound(state: TickState) {
        flushOnePendingResponse(state)
        deliverMailboxMessagesToReadyActors(state)
    }

    private fun flushOnePendingResponse(state: TickState) {
        if (ActorKernel.flushOnePendingResponse()) {
            state.increaseFlushedResponses()
        }
    }

    private fun deliverMailboxMessagesToReadyActors(state: TickState) {
        while (true) {
            if (state.checkIsStopped())
                break

            if (!ActorKernel.deliverOneReadyActorMessage())
                break

            state.increaseDeliveredMessages()
        }
    }
}
