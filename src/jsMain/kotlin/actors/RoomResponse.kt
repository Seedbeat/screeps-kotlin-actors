package actors

import actor.message.IResponse

sealed class RoomResponse<T> : IResponse<T> {
    data class StatusResponse(override val result: String) : RoomResponse<String>()
    data class TryAcquireResourceResponse(override val result: Boolean?) : RoomResponse<Boolean?>()
    data class ReleaseResourceResponse(override val result: Boolean?) : RoomResponse<Boolean?>()
}
