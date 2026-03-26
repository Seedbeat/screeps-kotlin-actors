package actors.memory.types

import actors.CreepAssignment
import actors.assignments.CreepAssignmentPhase
import actors.memory.base.ObjectMemoryNode
import actors.memory.delegates.memoryNodeEnum
import actors.memory.delegates.memoryNodeValue
import screeps.api.MemoryMarker

class ControllerUpkeepAssignmentMemory(
    parent: MemoryMarker,
    selfKey: String
) : ObjectMemoryNode<CreepAssignment.ControllerUpkeep>(parent, selfKey) {
    var roomName: String? by memoryNodeValue()
    var controllerId: String? by memoryNodeValue()
    var phase: CreepAssignmentPhase by memoryNodeEnum { CreepAssignmentPhase.HARVEST }

    override fun read(): CreepAssignment.ControllerUpkeep? = CreepAssignment.ControllerUpkeep(
        roomName = roomName ?: return null,
        controllerId = controllerId ?: return null,
        phase = phase
    )

    override fun write(value: CreepAssignment.ControllerUpkeep) {
        roomName = value.roomName
        controllerId = value.controllerId
        phase = value.phase
    }

    override fun clear() {
        deleteNodeValue(::roomName.name)
        deleteNodeValue(::controllerId.name)
        deleteNodeValue(::phase.name)
    }
}