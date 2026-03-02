package actors

import actor.message.IResponse

sealed class HarvesterResponse<T> : IResponse<T> {

    data class EnergyResponse(override val result: Int) : HarvesterResponse<Int>()
}

