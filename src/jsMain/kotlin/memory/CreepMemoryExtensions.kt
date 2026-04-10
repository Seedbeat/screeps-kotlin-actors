package memory

import creep.CreepAssignment
import memory.delegates.memoryNodeObject
import memory.delegates.memoryValue
import memory.types.assignment.CreepAssignmentMemory
import screeps.api.CreepMemory
import screeps.utils.unsafe.jsObject

var CreepMemory.homeRoom: String by memoryValue { "" }
var CreepMemory.lockedObjectId: String? by memoryValue()
var CreepMemory.assignment: CreepAssignment? by memoryNodeObject(::CreepAssignmentMemory)

fun createCreepMemory(
    block: CreepMemory.() -> Unit = {}
) = jsObject(block)
