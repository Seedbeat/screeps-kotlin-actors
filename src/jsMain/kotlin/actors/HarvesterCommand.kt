package actors

import actor.message.Command

sealed class HarvesterCommand : Command {

    data class Harvest(
        val sourceId: String
    ) : HarvesterCommand()

    data class Transfer(
        val targetId: String
    ) : HarvesterCommand()
}