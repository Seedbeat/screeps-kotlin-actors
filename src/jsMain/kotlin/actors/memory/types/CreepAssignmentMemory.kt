package actors.memory.types

import actors.CreepAssignment
import actors.assignments.ControllerUpkeepPhase
import actors.assignments.CreepAssignmentKind
import actors.memory.base.ObjectMemoryNode
import actors.memory.delegates.memoryNodeEnum
import actors.memory.delegates.memoryNodeValue
import screeps.api.MemoryMarker

class CreepAssignmentMemory(
    parent: MemoryMarker,
    key: String
) : ObjectMemoryNode<CreepAssignment>(parent, key) {

    var kind: CreepAssignmentKind by memoryNodeEnum { CreepAssignmentKind.NONE }
    var roomName: String by memoryNodeValue { "" }
    var controllerId: String by memoryNodeValue { "" }
    var sourceId: String by memoryNodeValue { "" }
    var phase: ControllerUpkeepPhase by memoryNodeEnum { ControllerUpkeepPhase.HARVEST }

    override fun read(): CreepAssignment? = when (kind) {
        CreepAssignmentKind.NONE -> null
        CreepAssignmentKind.CONTROLLER_UPKEEP -> {
            if (roomName.isBlank() || controllerId.isBlank() || sourceId.isBlank()) {
                null
            } else {
                CreepAssignment.ControllerUpkeep(
                    roomName = roomName,
                    controllerId = controllerId,
                    sourceId = sourceId,
                    phase = phase
                )
            }
        }
    }

    override fun write(value: CreepAssignment) = when (value) {
        is CreepAssignment.ControllerUpkeep -> {
            kind = CreepAssignmentKind.CONTROLLER_UPKEEP
            roomName = value.roomName
            controllerId = value.controllerId
            sourceId = value.sourceId
            phase = value.phase
        }
    }

    fun clear() {
        kind = CreepAssignmentKind.NONE
        roomName = ""
        controllerId = ""
        sourceId = ""
        phase = ControllerUpkeepPhase.HARVEST
    }
}