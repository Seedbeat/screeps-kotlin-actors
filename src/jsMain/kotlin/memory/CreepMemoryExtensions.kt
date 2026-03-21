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

@Deprecated("Legacy")
var CreepMemory.pause: Int by memoryValue { 0 }

@Deprecated("Legacy")
var CreepMemory.type: CreepType by memoryEnum { CreepType.None }

@Deprecated("Legacy")
var CreepMemory.role: Role by memoryEnum { Role.UNASSIGNED }

@Deprecated("Legacy")
var CreepMemory.state: State by memoryEnum { State.UNASSIGNED }

@Deprecated("Legacy")
var CreepMemory.workObjectId: String by memoryValue { "" }


var CreepMemory.homeRoom: String by memoryValue { "" }
var CreepMemory.lockedObjectId: String by memoryValue { "" }
val CreepMemory.assignment: CreepAssignmentMemory by memoryNode(::CreepAssignmentMemory)

fun createCreepMemory(
    type: CreepType, role: Role = Role.UNASSIGNED, block: CreepMemory.() -> Unit = {}
) = jsObject<CreepMemory> {
    this.type = type
    this.role = role
    this.state = State.UNASSIGNED
    this.homeRoom = ""
    this.lockedObjectId = ""
    this.workObjectId = ""
}.also(block)

fun createCreepMemory(
    block: CreepMemory.() -> Unit = {}
) = jsObject<CreepMemory> {
    this.homeRoom = ""
    this.lockedObjectId = ""
}.also(block)
