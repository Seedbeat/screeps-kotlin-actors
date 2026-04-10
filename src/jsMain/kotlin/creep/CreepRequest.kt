package creep

import actor.message.Request

sealed interface CreepRequest<T> : Request<T>
