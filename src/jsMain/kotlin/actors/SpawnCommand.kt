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
}
