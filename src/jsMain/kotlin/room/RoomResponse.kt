package room

import actor.message.Response

sealed interface RoomResponse<T> : Response<T> {
    data class TryAcquireResource(override val result: Boolean?) : RoomResponse<Boolean?>
    data class TryAcquireAnyResource(override val result: String?) : RoomResponse<String?>
    data class ReleaseResource(override val result: Boolean?) : RoomResponse<Boolean?>
}
