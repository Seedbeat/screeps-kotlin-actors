package actors

import actor.message.IResponse

sealed class RoomResponse<T> : IResponse<T> {

    data class StatusResponse(override val result: String) : RoomResponse<String>()
}