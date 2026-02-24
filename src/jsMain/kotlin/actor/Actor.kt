package actor

import actor.message.IMessage
import actor.message.IPayload
import actor.message.IRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import utils.log.ILogging
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.createCoroutine
import kotlin.coroutines.resume

abstract class Actor(
    val id: String,
    val rootContext: CoroutineContext = EmptyCoroutineContext,
    val onFinish: () -> Unit = {}
) : ILogging {
    private val continuation: Continuation<Unit> = ::run.createCoroutine(object : Continuation<Unit> {
        override val context = rootContext
        override fun resumeWith(result: Result<Unit>) {
            ActorSystem.remove(id)
            SleepingActors.remove(id)
            log.info("[Lifecycle] Finish: $result")
            onFinish.invoke()
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
            log.info("[Scheduler] '$id' waiting receive() (mailboxSize=${mailbox.size})")
        }

        @Suppress("UNCHECKED_CAST")
        SleepingActors.update(id, continuation as Continuation<IMessage>)
    }

    fun sendTo(actorId: String, payload: IPayload, messageId: String? = null) {
        ActorSystem.send(actorId, id, payload, messageId)
    }

    suspend fun <T> requestFrom(actorId: String, payload: IRequest): T {
        return ActorSystem.request(id, actorId, payload)
    }

    fun queueMessage(message: IMessage): Boolean {
        val result = mailbox.add(message)
        log.info("[${message.messageId}] Queue message for '$id' (mailboxSize=${mailbox.size})")
        return result
    }

    fun haveMessages() = mailbox.isNotEmpty()
    fun mailboxSize() = mailbox.size

    fun pollMessage(): IMessage? {
        if (mailbox.isEmpty()) return null

        val message = mailbox.removeAt(0)
        log.info("[${message.messageId}] Poll message for '$id' (mailboxSize=${mailbox.size})")
        return message
    }
}
