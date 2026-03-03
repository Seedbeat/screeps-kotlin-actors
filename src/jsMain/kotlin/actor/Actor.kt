package actor

import actor.message.IMessage
import actor.message.IPayload
import actor.message.IRequest
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import utils.log.ILogging
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.resume

abstract class Actor(val id: String) : ILogging {
    private val continuation: Continuation<Unit> = ::run.createCoroutine(object : Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {
            ActorSystem.remove(id)
            log.info("[Lifecycle] Finish: $result")
        }
    })
    private val mailbox = mutableListOf<IMessage>()

    fun start() {
        log.info("[Lifecycle] Start")
        continuation.resume(Unit)
    }

    abstract suspend fun run()

    suspend fun <T : IMessage> receive(): T = suspendCancellableCoroutine { continuation ->
        if (mailbox.isNotEmpty()) {
            log.debug("[Scheduler] waiting receive() (mailboxSize=${mailbox.size})")
        }

        @Suppress("UNCHECKED_CAST")
        ActorSystem.onActorWaitingReceive(this, continuation as CancellableContinuation<IMessage>)
        continuation.invokeOnCancellation {
            ActorSystem.removeActorReceiveWaiter(id)
        }
    }

    protected suspend fun nextTick(ticks: Int = 1) {
        if (ticks <= 0) return

        suspendCancellableCoroutine { continuation ->
            ActorSystem.onActorWaitingNextTick(this, ticks, continuation)
            continuation.invokeOnCancellation {
                ActorSystem.removeActorNextTickWaiter(id)
            }
        }
    }

    protected suspend fun yieldNow() {
        suspendCancellableCoroutine { continuation ->
            ActorSystem.onActorYield(this, continuation)
        }
    }

    protected suspend fun checkpoint() {
        if (!ActorSystem.shouldYieldAtCheckpoint()) return
        nextTick(1)
    }

    fun sendTo(actorId: String, payload: IPayload, messageId: String? = null) {
        ActorSystem.send(actorId, id, payload, messageId)
    }

    suspend fun <T> requestFrom(actorId: String, payload: IRequest): T {
        return ActorSystem.request(actorId, id, payload)
    }

    fun queueMessage(message: IMessage): Boolean {
        val result = mailbox.add(message)
        log.debug("[${message.messageId}] Queued new message (mailboxSize=${mailbox.size})")
        return result
    }

    fun haveMessages() = mailbox.isNotEmpty()

    fun pollMessage(): IMessage? {
        if (mailbox.isEmpty()) return null

        val message = mailbox.removeAt(0)
        log.debug("[${message.messageId}] Poll message (mailboxSize=${mailbox.size})")
        return message
    }
}
