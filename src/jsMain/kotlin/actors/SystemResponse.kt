package actors

import actor.message.Response

sealed class SystemResponse<T> : Response<T> {
    data class CountCreepsResponse(
        override val result: Int
    ) : SystemResponse<Int>()

    data class QueryCreepsResponse(
        override val result: List<CreepStatus>
    ) : SystemResponse<List<CreepStatus>>()
}
