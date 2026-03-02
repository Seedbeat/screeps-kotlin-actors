package actors

import actor.message.ICommand
import creep.enums.Role

sealed class SpawnCommand : ICommand {

    data class EnsurePopulation(
        val role: Role,
        val targetCount: Int
    ) : SpawnCommand()
}
