package actor

import actor.message.IMessage
import actor.message.IResponse
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging
import kotlin.coroutines.Continuation

object PendingResponses : ILogging by Logging<PendingResponses>(LogLevel.DEBUG) {
    private val waiting = mutableMapOf<String, Continuation<Any?>>()
    private val scheduled = mutableListOf<() -> Unit>()

    fun put(messageId: String, continuation: Continuation<Any?>) {
        log.info("[${messageId}] Set request continuation (waiting=${waiting.size + 1})")
        waiting[messageId] = continuation
    }

    fun remove(messageId: String) {
        waiting.remove(messageId)
        log.info("[${messageId}] Remove request continuation (waiting=${waiting.size})")
    }

    fun trySchedule(msg: IMessage): Boolean {
        val correlationId = msg.messageId
        val response = msg.payload as? IResponse<*> ?: return false
        val continuation = waiting.remove(correlationId) ?: return false

        log.info("[${msg.messageId}] Schedule response continuation (scheduled=${scheduled.size + 1}, waiting=${waiting.size})")
        scheduled.add {
            continuation.resumeWith(Result.success(response.result))
        }
        return true
    }

    fun flushOne(): Boolean {
        if (scheduled.isEmpty())
            return false

        log.info("[Tick] Flush scheduled response continuation (scheduled=${scheduled.size - 1})")
        val action = scheduled.removeAt(0)
        action.invoke()
        return true
    }
}
