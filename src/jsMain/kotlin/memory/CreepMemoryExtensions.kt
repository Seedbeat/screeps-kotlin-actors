package memory

import actors.CreepAssignment
import actors.memory.delegates.memoryNodeObject
import actors.memory.delegates.memoryValue
import actors.memory.types.creep.assignment.CreepAssignmentMemory
import screeps.api.CreepMemory
import screeps.utils.unsafe.jsObject

var CreepMemory.homeRoom: String by memoryValue { "" }
var CreepMemory.lockedObjectId: String? by memoryValue()
var CreepMemory.assignment: CreepAssignment? by memoryNodeObject(::CreepAssignmentMemory)

fun createCreepMemory(
    block: CreepMemory.() -> Unit = {}
) = jsObject(block)
