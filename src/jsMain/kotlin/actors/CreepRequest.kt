package actors

import actor.message.Request

sealed class CreepRequest<T> : Request<T> {
    data object Status : CreepRequest<CreepStatus>()
}
