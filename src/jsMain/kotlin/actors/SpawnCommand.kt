package actors

import actor.message.ICommand

sealed class SpawnCommand : ICommand {
    data class TrySpawnControllerSurvivalWorker(
        val roomName: String,
        val controllerId: String,
        val sourceId: String
    ) : SpawnCommand()
}
