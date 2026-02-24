package actor

import actor.enums.StopReasonType
import actor.message.IMessage
import actor.message.IPayload
import actor.message.IRequest
import actor.message.Message
import kotlinx.coroutines.suspendCancellableCoroutine
import screeps.api.Game
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging
import kotlin.coroutines.Continuation
import kotlin.random.Random

object ActorSystem : ILogging by Logging<ActorSystem>(LogLevel.DEBUG) {
    private data class TickState(
        val maxSteps: Int,
        val cpuReserve: Double,
        var rounds: Int = 0,
        var steps: Int = 0,
        var flushedResponses: Int = 0,
        var deliveredMessages: Int = 0,
        var stopReason: StopReasonType = StopReasonType.NONE
    ) {
        fun hasWork() = flushedResponses > 0 || deliveredMessages > 0
        fun isStopped() = stopReason != StopReasonType.NONE

        fun increaseFlushedResponses() {
            flushedResponses++
            steps++
        }

        fun increaseDeliveredMessages() {
            deliveredMessages++
            steps++
        }

        fun checkIsStopped(): Boolean {
            if (steps >= maxSteps) {
                stopReason = StopReasonType.MAX_STEPS
                return true
            }

            if (isCpuBudgetExceeded(cpuReserve)) {
                stopReason = StopReasonType.CPU_RESERVE
                return true
            }

            return false
        }

        private fun isCpuBudgetExceeded(cpuReserve: Double): Boolean {
            if (cpuReserve <= 0.0) return false
            return (Game.cpu.limit - Game.cpu.getUsed()) <= cpuReserve
        }
    }

    private val actors = mutableMapOf<String, Actor>()

    fun contains(actorId: String) = actors.containsKey(actorId)

    fun remove(actorId: String) = actors.remove(actorId)

    fun <T : Actor> spawn(actorId: String, create: (actorId: String) -> T) {
        if (actors.containsKey(actorId)) {
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

    fun tick(maxSteps: Int = 100, cpuReserve: Double = 3.0) {

        val state = TickState(maxSteps = maxSteps, cpuReserve = cpuReserve)

        while (true) {
            if (state.checkIsStopped())
                break

            state.rounds++

            val lastSteps = state.steps
            runTickRound(state)

            if (state.isStopped())
                break

            if (lastSteps == state.steps && state.hasWork()) {
                log.info("[Tick] Scheduler idle. Status: $state")
                break
            }
        }

        if (state.isStopped() && state.hasWork()) {
            log.info("[Tick] Scheduler stop. Status: $state")
        }
    }

    private fun runTickRound(state: TickState) {
        flushOnePendingResponse(state)
        deliverMailboxMessagesToSleepingActors(state)
    }

    private fun flushOnePendingResponse(state: TickState) {
        if (PendingResponses.flushOne()) {
            state.increaseFlushedResponses()
        }
    }

    private fun deliverMailboxMessagesToSleepingActors(state: TickState) {
        for ((actorId, actor) in actors) {
            if (state.checkIsStopped())
                break

            if (!SleepingActors.isSleeping(actorId)) {
                if (!actor.haveMessages())
                    continue

                log.warn("[Tick] Actor '$actorId' has queued messages (${actor.mailboxSize()}), but is not waiting receive()")
                continue
            }

            val message = actor.pollMessage() ?: continue
            log.info("[${message.messageId}] [Tick] Deliver mailbox message to sleeping actor '$actorId'")

            SleepingActors.wake(actorId, message)
            state.increaseDeliveredMessages()
        }
    }
}
