package creep

import creep.body.CreepCapabilities
import memory.homeRoom
import memory.lockedObjectId
import screeps.api.Creep

data class CreepStatus(
    val actorId: String,
    val homeRoom: String,
    val currentRoom: String,
    val assignment: CreepAssignment?,
    val capabilities: CreepCapabilities,
    val lockedResourceId: String?
) {
    constructor(name: String, creep: Creep, assignment: CreepAssignment?) : this(
        actorId = name,
        homeRoom = creep.memory.homeRoom,
        currentRoom = creep.room.name,
        assignment = assignment,
        capabilities = creep.capabilities,
        lockedResourceId = creep.memory.lockedObjectId
    )
}