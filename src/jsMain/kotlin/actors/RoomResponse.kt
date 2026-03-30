package actors

import actor.message.Response

sealed interface RoomResponse<T> : Response<T> {
    data class TryAcquireResourceResponse(override val result: Boolean?) : RoomResponse<Boolean?>
    data class TryAcquireAnyResourceResponse(override val result: String?) : RoomResponse<String?>
    data class ReleaseResourceResponse(override val result: Boolean?) : RoomResponse<Boolean?>
}
