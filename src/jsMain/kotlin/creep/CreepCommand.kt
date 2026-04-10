package creep

import actor.message.Command

sealed interface CreepCommand : Command {
    data class Assign(val assignment: CreepAssignment) : CreepCommand
    data object ClearAssignment : CreepCommand
    data class SetLockedResourceId(val resourceId: String?) : CreepCommand
}
