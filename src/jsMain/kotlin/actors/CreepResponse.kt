package actors

import actor.message.Response

sealed class CreepResponse<T> : Response<T> {
    data class StatusResponse(override val result: CreepStatus) : CreepResponse<CreepStatus>()
    data class UnassignResponse(override val result: Boolean) : CreepResponse<Boolean>()
}
