package actors

import actor.message.Command

sealed class CreepCommand : Command {
    data object Noop : CreepCommand()

    data class Assign(
        val assignment: CreepAssignment
    ) : CreepCommand()

    data class SetLockedResourceId(
        val resourceId: String?
    ) : CreepCommand()

    data object ClearAssignment : CreepCommand()
}
