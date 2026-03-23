package actors

import actor.message.Request
import screeps.api.RoomPosition

sealed class RoomRequest<T> : Request<T> {
    data object StatusRequest : RoomRequest<String>()

    data class TryAcquireResource(
        val ownerId: String,
        val resourceId: String
    ) : RoomRequest<Boolean?>()

    data class TryAcquireAnyResource(
        val ownerId: String,
        val near: RoomPosition,
        val type: RoomResourceType
    ) : RoomRequest<String?>()

    data class ReleaseResource(
        val ownerId: String,
        val resourceId: String
    ) : RoomRequest<Boolean?>()
}
