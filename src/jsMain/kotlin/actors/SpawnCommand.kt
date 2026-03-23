package actors

import actor.message.Command

sealed class SpawnCommand : Command {
    data class TrySpawnControllerSurvivalWorker(
        val roomName: String,
        val controllerId: String
    ) : SpawnCommand()
}
