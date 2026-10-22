package actor

import actor.message.Command
import actor.message.Payload
import actor.message.Request
import actors.base.Lifecycle
import utils.trace.CpuLogger
import utils.trace.CpuTrace

class ActorProfilingContext(
    private val actorId: String
) {
    private val trace = if (CpuLogger.isEnabled) CpuTrace() else null

    fun beginMessage(payload: Payload) {
        if (!CpuLogger.isEnabled) return
        trace!!.begin(actorId, payloadCategory(payload), payloadType(payload))
    }

    fun push(label: String) {
        if (!CpuLogger.isEnabled) return
        trace!!.push(label)
    }

    @PublishedApi
    internal fun isMessageScope(label: String): Boolean {
        if (!CpuLogger.isEnabled) return false
        return trace!!.isMessageScope(label)
    }

    fun pop() {
        if (!CpuLogger.isEnabled) return
        trace!!.pop()
    }

    fun onSliceStart(now: Double) {
        if (!CpuLogger.isEnabled) return
        trace!!.onSliceStart(now)
    }

    fun onSliceEnd(now: Double) {
        if (!CpuLogger.isEnabled) return
        trace!!.onSliceEnd(now)
    }

    private fun payloadCategory(payload: Payload): String = when (payload) {
        is Lifecycle -> Lifecycle::class.simpleName!!
        is Request<*> -> Request::class.simpleName!!
        is Command -> Command::class.simpleName!!
        else -> payload::class.simpleName ?: "Payload"
    }

    private fun payloadType(payload: Payload): String = payload::class.simpleName ?: "Payload"
}
