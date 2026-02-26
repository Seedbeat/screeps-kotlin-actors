package actor

import actor.enums.QueueMessageResult
import actor.message.IMessage
import actor.message.IResponse
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

object ActorKernel : ILogging by Logging<ActorKernel>(LogLevel.WARN) {
    private val actors = mutableMapOf<String, Actor>()

    private val readyActorIdsQueue = ArrayDeque<String>()
    private val readyActorIdsSet = mutableSetOf<String>()

    private val sleeping = mutableMapOf<String, Continuation<IMessage>>()

    private val pendingWaiting = mutableMapOf<String, Continuation<Any?>>()
    private val pendingScheduled = ArrayDeque<() -> Unit>()

    fun contains(actorId: String) = actors.containsKey(actorId)

    fun isActorSleeping(actorId: String): Boolean = sleeping.containsKey(actorId)

    fun getActor(actorId: String): Actor? = actors[actorId]

    fun registerActor(actor: Actor): Boolean {
        if (actors.containsKey(actor.id)) return false
        actors[actor.id] = actor
        return true
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
        clearTransientRuntimeState()
        if (snapshot.actors.isNotEmpty()) {
            log.warn("[Persistence] Actor snapshots present (${snapshot.actors.size}), but actor rehydration is not implemented yet")
        }
    }

    fun removeActor(actorId: String) {
        readyActorIdsSet.remove(actorId)
        sleeping.remove(actorId)
        actors.remove(actorId)
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

    fun onActorWaitingReceive(actor: Actor, continuation: Continuation<IMessage>) {
        sleeping[actor.id] = continuation
        enqueueReadyActor(actor)
    }

    fun putPendingResponse(messageId: String, continuation: Continuation<Any?>) {
        log.info("[${messageId}] Set request continuation (waiting=${pendingWaiting.size + 1})")
        pendingWaiting[messageId] = continuation
    }

    fun removePendingResponse(messageId: String) {
        pendingWaiting.remove(messageId)
        log.info("[${messageId}] Remove request continuation (waiting=${pendingWaiting.size})")
    }

    fun flushOnePendingResponse(): Boolean {
        if (pendingScheduled.isEmpty()) return false

        log.info("[Tick] Flush scheduled response continuation (scheduled=${pendingScheduled.size - 1})")
        val action = pendingScheduled.removeFirst()
        action.invoke()
        return true
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
            continuation.resume(message)
            return true
        }

        return false
    }

    private fun trySchedulePendingResponse(msg: IMessage): Boolean {
        val response = msg.payload as? IResponse<*> ?: return false
        val continuation = pendingWaiting.remove(msg.messageId) ?: return false

        log.info("[${msg.messageId}] Schedule response continuation (scheduled=${pendingScheduled.size + 1}, waiting=${pendingWaiting.size})")
        pendingScheduled.addLast {
            continuation.resumeWith(Result.success(response.result))
        }
        return true
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
        pendingScheduled.clear()
    }
}
