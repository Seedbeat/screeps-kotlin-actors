package memory

import actors.CreepAssignment
import actors.assignments.ControllerUpkeepPhase
import actors.assignments.CreepAssignmentKind
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
var CreepMemory.assignmentKind: String by memory { "" }
var CreepMemory.assignmentControllerId: String by memory { "" }
var CreepMemory.assignmentSourceId: String by memory { "" }
var CreepMemory.assignmentPhase: String by memory { "" }

fun CreepMemory.assignmentOrNull(): CreepAssignment? = when (assignmentKind) {
    CreepAssignmentKind.CONTROLLER_UPKEEP.name -> {
        if (assignmentRoom.isBlank() || assignmentControllerId.isBlank() || assignmentSourceId.isBlank()) {
            null
        } else {
            CreepAssignment.ControllerUpkeep(
                roomName = assignmentRoom,
                controllerId = assignmentControllerId,
                sourceId = assignmentSourceId
            )
        }
    }

    else -> null
}

fun CreepMemory.setAssignment(assignment: CreepAssignment) = when (assignment) {
    is CreepAssignment.ControllerUpkeep -> {
        assignmentKind = CreepAssignmentKind.CONTROLLER_UPKEEP.name
        assignmentRoom = assignment.roomName
        assignmentControllerId = assignment.controllerId
        assignmentSourceId = assignment.sourceId
        assignmentPhase = ControllerUpkeepPhase.HARVEST.name
        workObjectId = assignment.controllerId
    }
}

fun CreepMemory.clearAssignment() {
    assignmentKind = ""
    assignmentControllerId = ""
    assignmentSourceId = ""
    assignmentPhase = ""
    workObjectId = ""
}

fun CreepMemory.controllerUpkeepPhase(): ControllerUpkeepPhase {
    return ControllerUpkeepPhase.entries.firstOrNull { phase -> phase.name == assignmentPhase }
        ?: ControllerUpkeepPhase.HARVEST
}

fun CreepMemory.controllerUpkeepPhase(phase: ControllerUpkeepPhase) {
    assignmentPhase = phase.name
}

fun createCreepMemory(
    type: CreepType, role: Role, block: CreepMemory.() -> Unit = {}
) = jsObject<CreepMemory> {
    this.type = type
    this.role = role
    this.state = State.UNASSIGNED
    this.homeRoom = ""
    this.assignmentRoom = ""
    this.lockedObjectId = ""
    this.workObjectId = ""
    this.assignmentKind = ""
    this.assignmentControllerId = ""
    this.assignmentSourceId = ""
    this.assignmentPhase = ""
}.also(block)
