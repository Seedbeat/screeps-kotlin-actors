package actors

import actor.message.Request

sealed class RoomRequest : Request {
    data object StatusRequest : RoomRequest()

    data class TryAcquireResource(
        val ownerId: String,
        val resourceId: String
    ) : RoomRequest()

    data class ReleaseResource(
        val ownerId: String,
        val resourceId: String
    ) : RoomRequest()
}
