package actors

import actor.message.Command

sealed interface SpawnCommand : Command {
    data class TrySpawnControllerSurvivalWorker(
        val roomName: String,
        val controllerId: String
    ) : SpawnCommand

    data class TrySpawnConstructionWorker(
        val roomName: String,
        val constructionSiteId: String
    ) : SpawnCommand

    data class TrySpawnEnergyTransferWorker(
        val roomName: String,
        val targetId: String
    ) : SpawnCommand
}
