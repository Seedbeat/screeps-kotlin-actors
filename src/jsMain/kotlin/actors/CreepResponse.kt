package actors

import actor.message.Response

sealed interface CreepResponse<T> : Response<T>
