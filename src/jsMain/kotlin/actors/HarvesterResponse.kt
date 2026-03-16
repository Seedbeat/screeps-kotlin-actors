package actors

import actor.message.Response

sealed class HarvesterResponse<T> : Response<T> {

    data class EnergyResponse(override val result: Int) : HarvesterResponse<Int>()
}

