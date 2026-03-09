package actors

import actor.message.ICommand

sealed class CreepCommand : ICommand {
    data object Noop : CreepCommand()

    data class Assign(
        val assignment: CreepAssignment
    ) : CreepCommand()

    data class SetLockedResourceId(
        val resourceId: String?
    ) : CreepCommand()

    data object ClearAssignment : CreepCommand()
}
