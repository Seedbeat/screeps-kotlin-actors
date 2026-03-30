package actors.memory.types.creep.assignment.goal.values

import actors.CreepAssignment
import actors.memory.base.EmptyMemoryNode
import screeps.api.MemoryMarker

class UntilFull(
    parent: MemoryMarker,
    selfKey: String
) : EmptyMemoryNode<CreepAssignment.EnergyTransfer.Goal.UntilFull>(parent, selfKey) {
    override fun read() = CreepAssignment.EnergyTransfer.Goal.UntilFull
}