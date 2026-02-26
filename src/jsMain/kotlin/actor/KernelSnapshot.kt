package actor

import screeps.api.Game

data class KernelSnapshot(
    val time: Int = Game.time,
    val actors: List<ActorSnapshot> = emptyList()
) {
    data class ActorSnapshot(
        val id: String,
        val type: String
    )
}
