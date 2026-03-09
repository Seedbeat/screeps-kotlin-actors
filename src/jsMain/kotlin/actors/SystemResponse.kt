package actors

import actor.message.IResponse

sealed class SystemResponse<T> : IResponse<T> {
    data class CountCreepsResponse(
        override val result: Int
    ) : SystemResponse<Int>()
}
