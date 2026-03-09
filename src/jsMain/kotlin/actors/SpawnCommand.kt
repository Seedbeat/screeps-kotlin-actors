package actors

import actor.message.ICommand
import creep.enums.Role

sealed class SpawnCommand : ICommand {
    data class TrySpawn(
        val role: Role
    ) : SpawnCommand()
}
