package actors

import actor.message.IRequest

sealed class CreepRequest : IRequest {
    data object StatusRequest : CreepRequest()
    data object Unassign : CreepRequest()
}
