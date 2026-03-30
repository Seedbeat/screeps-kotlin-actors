package actors.memory.types.creep.assignment

import actors.CreepAssignment
import actors.memory.base.SealedMemoryNode
import actors.memory.types.creep.assignment.values.Construction
import actors.memory.types.creep.assignment.values.ControllerUpkeep
import actors.memory.types.creep.assignment.values.EnergyTransfer
import screeps.api.MemoryMarker

class CreepAssignmentMemory(
    parent: MemoryMarker,
    selfKey: String
) : SealedMemoryNode<CreepAssignment>(parent, selfKey) {
    override val mapping = mapOf(
        sealedNode(ControllerUpkeep(parent, selfKey)),
        sealedNode(Construction(parent, selfKey)),
        sealedNode(EnergyTransfer(parent, selfKey))
    )
}