package actors

import actor.message.Request
import screeps.api.RoomPosition

sealed class RoomRequest<T> : Request<T> {
    data class TryAcquireResourceById(
        val ownerId: String,
        val resourceId: String
    ) : RoomRequest<Boolean?>()

    data class TryAcquireResourceByType(
        val ownerId: String,
        val near: RoomPosition,
        val type: RoomResourceType
    ) : RoomRequest<String?>()

    data class ReleaseResourceById(
        val ownerId: String,
        val resourceId: String
    ) : RoomRequest<Boolean?>()
}
