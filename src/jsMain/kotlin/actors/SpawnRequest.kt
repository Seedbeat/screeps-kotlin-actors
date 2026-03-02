package actors

import actor.message.IRequest
import creep.enums.Role

sealed class SpawnRequest : IRequest {

    data class PopulationRequest(
        val role: Role
    ) : SpawnRequest()
}
