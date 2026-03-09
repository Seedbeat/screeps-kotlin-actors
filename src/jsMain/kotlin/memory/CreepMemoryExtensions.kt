package memory

import creep.enums.CreepType
import creep.enums.Role
import creep.enums.State
import screeps.api.CreepMemory
import screeps.utils.memory.memory
import screeps.utils.unsafe.jsObject

var CreepMemory.pause: Int by memory { 0 }
var CreepMemory.type by memory(CreepType.None)
var CreepMemory.role by memory(Role.UNASSIGNED)
var CreepMemory.state by memory(State.UNASSIGNED)
var CreepMemory.homeRoom: String by memory { "" }
var CreepMemory.assignmentRoom: String by memory { "" }
var CreepMemory.lockedObjectId: String by memory { "" }
var CreepMemory.workObjectId: String by memory { "" }

fun createCreepMemory(
    type: CreepType, role: Role, block: CreepMemory.() -> Unit = {}
) = jsObject<CreepMemory> {
    this.type = type
    this.role = role
    this.state = State.UNASSIGNED
    this.homeRoom = ""
    this.assignmentRoom = ""
    this.lockedObjectId = ""
}.also(block)
