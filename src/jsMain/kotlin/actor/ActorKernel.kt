package actor

import actor.enums.QueueMessageResult
import actor.message.IMessage
import actor.message.IResponse
import kotlinx.coroutines.CancellableContinuation
import screeps.api.Game
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging
import kotlin.coroutines.resume

object ActorKernel : ILogging by Logging<ActorKernel>(LogLevel.WARN) {
    private val actors = mutableMapOf<String, Actor>()
    private val readyActorIdsQueue = ArrayDeque<String>()
    private val readyActorIdsSet = mutableSetOf<String>()

    private val sleeping = mutableMapOf<String, CancellableContinuation<IMessage>>()

    private val pendingWaiting = mutableMapOf<String, PendingResponseWaiter>()
    private val scheduledContinuations = ArrayDeque<ScheduledContinuation>()
    private val waitingNextTick = mutableMapOf<String, TickWaiter>()

    private data class TickWaiter(
        val wakeTick: Int,
        val continuation: CancellableContinuation<Unit>
    )

    private data class PendingResponseWaiter(
        val actorId: String,
        val continuation: CancellableContinuation<Any?>
    )

    private data class ScheduledContinuation(
        val actorId: String?,
        val action: () -> Unit
    )

    fun actors(): Map<String, Actor> = actors

    fun contains(actorId: String) = actors.containsKey(actorId)

    fun isActorSleeping(actorId: String): Boolean = sleeping.containsKey(actorId)

    fun getActor(actorId: String): Actor? = actors[actorId]

    fun registerActor(actor: Actor): Boolean {
        if (actors.containsKey(actor.id)) return false
        actors[actor.id] = actor
        return true
    }

    fun removeActor(actorId: String) {
        actors[actorId]?.destroy()
        readyActorIdsSet.remove(actorId)
        sleeping.remove(actorId)
        waitingNextTick.remove(actorId)
        pendingWaiting
            .filterValues { it.actorId == actorId }
            .keys
            .forEach { messageId -> pendingWaiting.remove(messageId) }
        actors.remove(actorId)
    }

    fun snapshot(): KernelSnapshot = KernelSnapshot(
        actors = actors.values.map { actor ->
            KernelSnapshot.ActorSnapshot(
                id = actor.id,
                type = actor::class.simpleName ?: "UnknownActor"
            )
        }
    )

    fun restore(snapshot: KernelSnapshot) {
        if (snapshot.actors.isNotEmpty()) {
            log.warn("[Persistence] Actor snapshots present (${snapshot.actors.size}), but actor rehydration is not implemented yet")
            // clearTransientRuntimeState()
        }
    }

    fun queueActorMessage(actorId: String, msg: IMessage): QueueMessageResult {
        if (trySchedulePendingResponse(msg))
            return QueueMessageResult.SCHEDULED_RESPONSE

        val actor = actors[actorId]
            ?: return QueueMessageResult.ACTOR_NOT_FOUND

        actor.queueMessage(msg)
        enqueueReadyActor(actor)

        return QueueMessageResult.QUEUED_TO_MAILBOX
    }

    fun onActorWaitingReceive(actor: Actor, continuation: CancellableContinuation<IMessage>) {
        sleeping[actor.id] = continuation
        enqueueReadyActor(actor)
    }

    fun removeActorReceiveWaiter(actorId: String) {
        sleeping.remove(actorId)
    }

    fun onActorWaitingNextTick(actor: Actor, ticks: Int, continuation: CancellableContinuation<Unit>) {
        waitingNextTick[actor.id] = TickWaiter(
            wakeTick = Game.time + ticks,
            continuation = continuation
        )
    }

    fun removeActorNextTickWaiter(actorId: String) {
        waitingNextTick.remove(actorId)
    }

    fun onActorYield(actor: Actor, continuation: CancellableContinuation<Unit>) {
        log.info("[Tick] Schedule yield continuation for actor '${actor.id}' (scheduled=${scheduledContinuations.size + 1})")
        scheduleContinuation(actor.id) {
            if (continuation.isActive)
                continuation.resume(Unit)
        }
    }

    fun wakeActorsReadyByTick(now: Int = Game.time): Int {
        val ready = waitingNextTick
            .filterValues { it.wakeTick <= now }

        ready.forEach { (actorId, waiter) ->
            waitingNextTick.remove(actorId)
            log.info("[Tick] Schedule wake-up continuation for actor '$actorId' at tick $now")
            scheduleContinuation(actorId) {
                if (waiter.continuation.isActive)
                    waiter.continuation.resume(Unit)
            }
        }

        return ready.size
    }

    fun putPendingResponse(messageId: String, actorId: String, continuation: CancellableContinuation<Any?>) {
        log.info("[${messageId}] Set request continuation (waiting=${pendingWaiting.size + 1})")
        pendingWaiting[messageId] = PendingResponseWaiter(actorId, continuation)
    }

    fun removePendingResponse(messageId: String) {
        pendingWaiting.remove(messageId)
        log.info("[${messageId}] Remove request continuation (waiting=${pendingWaiting.size})")
    }

    fun flushScheduledContinuations(): Boolean {
        while (scheduledContinuations.isNotEmpty()) {
            log.info("[Tick] Flush scheduled continuation (scheduled=${scheduledContinuations.size - 1})")
            val scheduled = scheduledContinuations.removeFirst()

            if (scheduled.actorId != null && !actors.containsKey(scheduled.actorId)) {
                log.info("[Tick] Drop scheduled continuation for removed actor '${scheduled.actorId}'")
                continue
            }

            scheduled.action.invoke()
            return true
        }

        return false
    }

    fun deliverOneReadyActorMessage(): Boolean {
        while (readyActorIdsQueue.isNotEmpty()) {
            val actorId = readyActorIdsQueue.removeFirst()
            readyActorIdsSet.remove(actorId)

            val actor = actors[actorId] ?: continue
            val continuation = sleeping[actorId] ?: continue
            val message = actor.pollMessage() ?: continue

            log.info("[${message.messageId}] [Tick] Deliver mailbox message to sleeping actor '$actorId'")
            sleeping.remove(actorId)
            if (!continuation.isActive) {
                log.info("[${message.messageId}] Drop mailbox message for inactive continuation of actor '$actorId'")
                continue
            }
            continuation.resume(message)
            return true
        }

        return false
    }

    private fun trySchedulePendingResponse(msg: IMessage): Boolean {
        val response = msg.payload as? IResponse<*> ?: return false
        val waiter = pendingWaiting.remove(msg.messageId) ?: return false

        log.info("[${msg.messageId}] Schedule response continuation (scheduled=${scheduledContinuations.size + 1}, waiting=${pendingWaiting.size})")
        scheduleContinuation(waiter.actorId) {
            if (waiter.continuation.isActive)
                waiter.continuation.resumeWith(Result.success(response.result))
        }
        return true
    }

    private fun scheduleContinuation(actorId: String? = null, action: () -> Unit) {
        scheduledContinuations.addLast(ScheduledContinuation(actorId, action))
    }

    private fun enqueueReadyActor(actor: Actor) {
        if (!sleeping.containsKey(actor.id)) return
        if (!actor.haveMessages()) return
        if (!readyActorIdsSet.add(actor.id)) return

        readyActorIdsQueue.addLast(actor.id)
        log.info("[Tick] Enqueue ready actor '${actor.id}' (readyQueueSize=${readyActorIdsQueue.size})")
    }

    private fun clearTransientRuntimeState() {
        readyActorIdsQueue.clear()
        readyActorIdsSet.clear()
        sleeping.clear()
        pendingWaiting.clear()
        scheduledContinuations.clear()
        waitingNextTick.clear()
    }
}
