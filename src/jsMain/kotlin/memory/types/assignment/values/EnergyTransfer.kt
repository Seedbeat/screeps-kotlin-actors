package memory.types.assignment.values

import creep.CreepAssignment
import creep.enums.CreepAssignmentPhase
import memory.base.ObjectMemoryNode
import memory.delegates.memoryNodeEnum
import memory.delegates.memoryNodeObject
import memory.delegates.memoryNodeValue
import memory.types.assignment.goal.GoalMemory
import screeps.api.MemoryMarker

class EnergyTransfer(
    parent: MemoryMarker,
    selfKey: String
) : ObjectMemoryNode<CreepAssignment.EnergyTransfer>(parent, selfKey) {
    var roomName: String? by memoryNodeValue()
    var targetId: String? by memoryNodeValue()
    var goal: CreepAssignment.EnergyTransfer.Goal? by memoryNodeObject(::GoalMemory)
    var phase: CreepAssignmentPhase? by memoryNodeEnum()

    override fun read(): CreepAssignment.EnergyTransfer? = CreepAssignment.EnergyTransfer(
        roomName = roomName ?: return null,
        targetId = targetId ?: return null,
        goal = goal ?: return null,
        phase = phase ?: return null
    )

    override fun write(value: CreepAssignment.EnergyTransfer) {
        roomName = value.roomName
        targetId = value.targetId
        goal = value.goal
        phase = value.phase
    }

    override fun clear() {
        deleteNodeValue(::roomName.name)
        deleteNodeValue(::targetId.name)
        deleteNodeValue(::goal.name)
        deleteNodeValue(::phase.name)
    }
}