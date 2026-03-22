package actors

import actor.message.Request
import creep.enums.Role

sealed class SpawnRequest<T> : Request<T> {

    data class PopulationRequest(
        val role: Role
    ) : SpawnRequest<Int>()
}
