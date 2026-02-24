package actor

import actor.message.IMessage
import actor.message.IPayload
import actor.message.IRequest
import actor.message.Message
import kotlinx.coroutines.suspendCancellableCoroutine
import screeps.api.Game
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging
import kotlin.collections.ArrayDeque
import kotlin.coroutines.Continuation
import kotlin.random.Random

object ActorSystem : ILogging by Logging<ActorSystem>(LogLevel.DEBUG) {
    private val actors = mutableMapOf<String, Actor>()
    private val readyActorIdsQueue = ArrayDeque<String>()
    private val readyActorIdsSet = mutableSetOf<String>()

    fun contains(actorId: String) = actors.containsKey(actorId)

    fun remove(actorId: String) {
        readyActorIdsSet.remove(actorId)
        actors.remove(actorId)
        SleepingActors.remove(actorId)
    }

    fun <T : Actor> spawn(actorId: String, create: (actorId: String) -> T) {
        if (contains(actorId)) {
            log.warn("Already spawned actor $actorId, return")
            return
        }

        log.info("Spawn actor: $actorId")
        val actor = create(actorId)
        actors[actorId] = actor
        actor.start()
    }

    fun send(actorId: String, msg: IMessage) {
        log.info("[${msg.messageId}] ('${msg.from}' -> '$actorId'): ${msg.payload}")

        if (PendingResponses.trySchedule(msg)) {
            log.info("[${msg.messageId}] response -> scheduled continuation")
            return
        }

        log.info("[${msg.messageId}] queued to mailbox")
        val actor = actors[actorId] ?: run {
            log.warn("[${msg.messageId}] Actor '$actorId' not found")
            return
        }

        actor.queueMessage(msg)
        enqueueReadyActor(actorId, actor)
    }

    fun send(toActorId: String, fromActorId: String, payload: IPayload, messageId: String? = null) {
        val id = messageId ?: generateMessageId()
        send(toActorId, Message(id, fromActorId, payload))
    }

    suspend fun <T> request(from: String, to: String, payload: IRequest): T {
        val messageId = generateMessageId()

        @Suppress("UNCHECKED_CAST")
        return suspendCancellableCoroutine { cont ->
            PendingResponses.put(messageId, cont as Continuation<Any?>)
            cont.invokeOnCancellation { PendingResponses.remove(messageId) }
            send(to, Message(messageId, from, payload))
        }
    }

    fun generateMessageId() = "${Game.time}|${Random.nextInt(1000, 9999)}"

    fun onActorWaitingReceive(actor: Actor, continuation: Continuation<IMessage>) {
        SleepingActors.update(actor.id, continuation)
        enqueueReadyActor(actor.id, actor)
    }

    fun tick(maxSteps: Int = 100, cpuReserve: Double = 3.0) {

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
        if (PendingResponses.flushOne()) {
            state.increaseFlushedResponses()
        }
    }

    private fun deliverMailboxMessagesToReadyActors(state: TickState) {
        while (readyActorIdsQueue.isNotEmpty()) {
            if (state.checkIsStopped())
                break

            val actorId = readyActorIdsQueue.removeFirst()
            readyActorIdsSet.remove(actorId)

            val actor = actors[actorId] ?: continue
            if (!SleepingActors.isSleeping(actorId))
                continue

            val message = actor.pollMessage() ?: continue
            log.info("[${message.messageId}] [Tick] Deliver mailbox message to sleeping actor '$actorId'")

            SleepingActors.wake(actorId, message)
            state.increaseDeliveredMessages()
        }
    }

    private fun enqueueReadyActor(actorId: String, actor: Actor? = actors[actorId]) {
        val targetActor = actor ?: return

        if (!SleepingActors.isSleeping(actorId))
            return

        if (!targetActor.haveMessages())
            return

        if (!readyActorIdsSet.add(actorId))
            return

        readyActorIdsQueue.addLast(actorId)
        log.info("[Tick] Enqueue ready actor '$actorId' (readyQueueSize=${readyActorIdsQueue.size})")
    }
}
