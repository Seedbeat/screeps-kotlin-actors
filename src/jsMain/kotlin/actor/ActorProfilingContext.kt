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
    private var trace: CpuTrace? = null
    private var session = -1

    @PublishedApi
    internal val isActive: Boolean
        get() = CpuLogger.isEnabled && session == CpuLogger.session

    fun beginMessage(payload: Payload) {
        if (!CpuLogger.isEnabled) {
            session = -1
            return
        }
        val currentTrace = trace ?: CpuTrace().also { trace = it }
        currentTrace.begin(actorId, payloadCategory(payload), payloadType(payload))
        session = CpuLogger.session
    }

    fun push(label: String) {
        if (!isActive) return
        trace!!.push(label)
    }

    @PublishedApi
    internal fun isMessageScope(label: String): Boolean {
        if (!isActive) return false
        return trace!!.isMessageScope(label)
    }

    fun pop() {
        if (!isActive) return
        trace!!.pop()
    }

    fun onSliceStart(now: Double) {
        if (!isActive) return
        trace!!.onSliceStart(now)
    }

    fun onSliceEnd(now: Double) {
        if (!isActive) return
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
