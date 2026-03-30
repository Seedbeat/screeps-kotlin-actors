package actors

import actor.message.Command

sealed class CreepCommand : Command {
    data class Assign(
        val assignment: CreepAssignment
    ) : CreepCommand()

    data class SetLockedResourceId(
        val resourceId: String?
    ) : CreepCommand()

    data object ClearAssignment : CreepCommand()
}
