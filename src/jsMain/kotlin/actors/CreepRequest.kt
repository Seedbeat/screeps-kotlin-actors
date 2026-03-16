package actors

import actor.message.Request

sealed class CreepRequest : Request {
    data object StatusRequest : CreepRequest()
    data object Unassign : CreepRequest()
}
