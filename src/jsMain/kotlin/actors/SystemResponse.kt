package actors

import actor.message.IResponse

sealed class SystemResponse<T> : IResponse<T> {
    data class CountCreepsResponse(
        override val result: Int
    ) : SystemResponse<Int>()

    data class QueryCreepsResponse(
        override val result: List<CreepStatus>
    ) : SystemResponse<List<CreepStatus>>()
}
