package actors.memory.types.creep.assignment.values

import actors.CreepAssignment
import actors.assignments.CreepAssignmentPhase
import actors.memory.base.ObjectMemoryNode
import actors.memory.delegates.memoryNodeEnum
import actors.memory.delegates.memoryNodeValue
import screeps.api.MemoryMarker

class ControllerProgress(
    parent: MemoryMarker,
    selfKey: String
) : ObjectMemoryNode<CreepAssignment.ControllerProgress>(parent, selfKey) {
    var roomName: String? by memoryNodeValue()
    var controllerId: String? by memoryNodeValue()
    var phase: CreepAssignmentPhase? by memoryNodeEnum()

    override fun read(): CreepAssignment.ControllerProgress? = CreepAssignment.ControllerProgress(
        roomName = roomName ?: return null,
        controllerId = controllerId ?: return null,
        phase = phase ?: return null
    )

    override fun write(value: CreepAssignment.ControllerProgress) {
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
