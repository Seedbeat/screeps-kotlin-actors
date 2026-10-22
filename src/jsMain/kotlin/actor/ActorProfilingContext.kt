package actor

import actor.message.Command
import actor.message.Payload
import actor.message.Request
import actors.base.Lifecycle
import utils.trace.CpuTrace

class ActorProfilingContext(
    private val actorId: String
) {
    private val trace = CpuTrace()

    fun beginMessage(payload: Payload) {
        trace.begin(actorId, payloadCategory(payload), payloadType(payload))
    }

    fun push(label: String) {
        trace.push(label)
    }

    internal fun isMessageScope(label: String): Boolean = trace.isMessageScope(label)

    fun pop() {
        trace.pop()
    }

    fun onSliceStart(now: Double) {
        trace.onSliceStart(now)
    }

    fun onSliceEnd(now: Double) {
        trace.onSliceEnd(now)
    }

    private fun payloadCategory(payload: Payload): String = when (payload) {
        is Lifecycle -> Lifecycle::class.simpleName!!
        is Request<*> -> Request::class.simpleName!!
        is Command -> Command::class.simpleName!!
        else -> payload::class.simpleName ?: "Payload"
    }

    private fun payloadType(payload: Payload): String = payload::class.simpleName ?: "Payload"
}
