package actors

import actor.message.IRequest

sealed class RoomRequest : IRequest {
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
