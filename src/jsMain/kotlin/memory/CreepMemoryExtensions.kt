package memory

import actors.memory.delegates.memoryEnum
import actors.memory.delegates.memoryNode
import actors.memory.delegates.memoryValue
import actors.memory.types.CreepAssignmentMemory
import creep.enums.CreepType
import creep.enums.Role
import creep.enums.State
import screeps.api.CreepMemory
import screeps.utils.unsafe.jsObject

var CreepMemory.pause: Int by memoryValue { 0 }
var CreepMemory.type: CreepType by memoryEnum { CreepType.None }
var CreepMemory.role: Role by memoryEnum { Role.UNASSIGNED }
var CreepMemory.state: State by memoryEnum { State.UNASSIGNED }
var CreepMemory.homeRoom: String by memoryValue { "" }
var CreepMemory.lockedObjectId: String by memoryValue { "" }

// Legacy-only field still used by old scheduler code.
var CreepMemory.workObjectId: String by memoryValue { "" }

val CreepMemory.assignment: CreepAssignmentMemory by memoryNode(::CreepAssignmentMemory)

fun createCreepMemory(
    type: CreepType, role: Role, block: CreepMemory.() -> Unit = {}
) = jsObject<CreepMemory> {
    this.type = type
    this.role = role
    this.state = State.UNASSIGNED
    this.homeRoom = ""
    this.lockedObjectId = ""
    this.workObjectId = ""
}.also(block)
