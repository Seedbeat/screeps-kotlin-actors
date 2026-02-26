package actors

import actor.message.ICommand

sealed class HarvesterCommand: ICommand

data class HarvesterHarvest(
    val sourceId: String
) : HarvesterCommand()

data class HarvesterTransfer(
    val targetId: String
) : HarvesterCommand()