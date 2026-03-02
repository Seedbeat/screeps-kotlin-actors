package actors

import actor.message.ICommand

sealed class HarvesterCommand : ICommand {

    data class Harvest(
        val sourceId: String
    ) : HarvesterCommand()

    data class Transfer(
        val targetId: String
    ) : HarvesterCommand()
}