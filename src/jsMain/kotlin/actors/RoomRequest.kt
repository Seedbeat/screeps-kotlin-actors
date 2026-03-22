package actors

import actor.message.Request

sealed class RoomRequest<T> : Request<T> {
    data object StatusRequest : RoomRequest<String>()

    data class TryAcquireResource(
        val ownerId: String,
        val resourceId: String
    ) : RoomRequest<Boolean?>()

    data class ReleaseResource(
        val ownerId: String,
        val resourceId: String
    ) : RoomRequest<Boolean?>()
}
