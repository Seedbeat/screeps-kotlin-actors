package actors

import actor.message.IRequest
import creep.enums.Role

sealed class SystemRequest : IRequest {
    data class CountCreeps(
        val homeRoom: String? = null,
        val currentRoom: String? = null,
        val assignmentRoom: String? = null,
        val role: Role? = null
    ) : SystemRequest()

    data class QueryCreeps(
        val homeRoom: String? = null,
        val currentRoom: String? = null,
        val assignmentRoom: String? = null
    ) : SystemRequest()
}
