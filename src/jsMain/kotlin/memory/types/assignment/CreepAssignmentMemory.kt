package memory.types.assignment

import creep.CreepAssignment
import memory.base.SealedMemoryNode
import memory.types.assignment.values.Construction
import memory.types.assignment.values.ControllerProgress
import memory.types.assignment.values.ControllerUpkeep
import memory.types.assignment.values.EnergyTransfer
import screeps.api.MemoryMarker

class CreepAssignmentMemory(
    parent: MemoryMarker,
    selfKey: String
) : SealedMemoryNode<CreepAssignment>(parent, selfKey) {
    override val mapping = mapOf(
        sealedNode(ControllerUpkeep(parent, selfKey)),
        sealedNode(ControllerProgress(parent, selfKey)),
        sealedNode(Construction(parent, selfKey)),
        sealedNode(EnergyTransfer(parent, selfKey))
    )
}
