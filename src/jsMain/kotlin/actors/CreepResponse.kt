package actors

import actor.message.Response

sealed class CreepResponse<T> : Response<T> {
    data class Status(override val result: CreepStatus) : CreepResponse<CreepStatus>()
}
