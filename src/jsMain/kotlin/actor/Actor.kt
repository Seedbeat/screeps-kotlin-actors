package actor

import actor.message.Message
import actor.message.Payload
import actor.message.Request
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import utils.log.ILogging
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.resume

abstract class Actor(val id: String) : MessagingApi, ILogging {
    private var isDestroyed = false

    private val continuation: Continuation<Unit> = ::run.createCoroutine(object : Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {
            destroy()
            ActorSystem.remove(id)
            log.info("[Lifecycle] Finished: $result")
        }
    })
    private val mailbox = mutableListOf<Message>()

    fun start() {
        log.info("[Lifecycle] Started")
        continuation.resume(Unit)
    }

    abstract suspend fun run()

    open fun onDestroy() = Unit

    internal fun destroy() {
        if (isDestroyed)
            return

        try {
            onDestroy()
        } catch (exception: Exception) {
            log.error("[Lifecycle] onDestroy failed", exception)
        } finally {
            isDestroyed = true
        }
    }

    suspend fun <T : Message> receive(): T = suspendCancellableCoroutine { continuation ->
        if (mailbox.isNotEmpty()) {
            log.debug("[Scheduler] waiting receive() (mailboxSize=${mailbox.size})")
        }

        @Suppress("UNCHECKED_CAST")
        ActorSystem.onActorWaitingReceive(this, continuation as CancellableContinuation<Message>)
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

    override fun sendTo(actorId: String, payload: Payload) = ActorSystem.send(
        toActorId = actorId,
        fromActorId = id,
        payload = payload,
        messageId = null
    )

    override suspend fun <T> requestFrom(actorId: String, payload: Request<T>): T = ActorSystem.request(
        toActorId = actorId,
        fromActorId = id,
        payload = payload
    )

    override fun reply(msg: Message, response: Payload) = ActorSystem.send(
        toActorId = msg.from,
        fromActorId = id,
        payload = response,
        messageId = msg.messageId
    )

    override fun selfSend(payload: Payload) =
        sendTo(actorId = id, payload = payload)

    override suspend fun <T> selfRequest(payload: Request<T>): T =
        requestFrom(actorId = id, payload = payload)

    fun queueMessage(message: Message): Boolean {
        val result = mailbox.add(message)
        log.debug("[${message.messageId}] Queued new message (mailboxSize=${mailbox.size})")
        return result
    }

    fun haveMessages() = mailbox.isNotEmpty()

    fun pollMessage(): Message? {
        if (mailbox.isEmpty()) return null

        val message = mailbox.removeAt(0)
        log.debug("[${message.messageId}] Poll message (mailboxSize=${mailbox.size})")
        return message
    }
}
