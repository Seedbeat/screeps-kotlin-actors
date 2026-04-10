package memory.types.assignment.goal.values

import creep.CreepAssignment
import memory.base.EmptyMemoryNode
import screeps.api.MemoryMarker

class UntilFull(
    parent: MemoryMarker,
    selfKey: String
) : EmptyMemoryNode<CreepAssignment.EnergyTransfer.Goal.UntilFull>(parent, selfKey) {
    override fun read() = CreepAssignment.EnergyTransfer.Goal.UntilFull
}