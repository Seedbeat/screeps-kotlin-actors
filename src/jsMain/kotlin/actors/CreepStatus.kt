package actors

data class CreepStatus(
    val actorId: String,
    val homeRoom: String,
    val assignmentRoom: String,
    val currentRoom: String,
    val assignment: CreepAssignment?,
    val capabilities: CreepCapabilities,
    val lockedResourceId: String?
)
