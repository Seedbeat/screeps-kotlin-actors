package actors

import actor.message.IResponse

sealed class CreepResponse<T> : IResponse<T> {
    data class StatusResponse(override val result: CreepStatus) : CreepResponse<CreepStatus>()
    data class UnassignResponse(override val result: Boolean) : CreepResponse<Boolean>()
}
