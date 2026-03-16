package actors

import actor.message.Request
import creep.enums.Role

sealed class SpawnRequest : Request {

    data class PopulationRequest(
        val role: Role
    ) : SpawnRequest()
}
