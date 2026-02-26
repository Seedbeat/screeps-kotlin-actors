package actors

import actor.message.IResponse

sealed class HarvesterResponse<T> : IResponse<T>

data class HarvesterGetEnergyResponse(override val result: Int) : HarvesterResponse<Int>()
