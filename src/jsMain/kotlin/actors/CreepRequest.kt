package actors

import actor.message.Request

sealed class CreepRequest<T> : Request<T> {
    data object StatusRequest : CreepRequest<CreepStatus>()
    data object Unassign : CreepRequest<Boolean>()
}
