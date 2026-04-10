package memory.types.assignment.goal

import creep.CreepAssignment
import memory.base.ObjectMemoryNode
import memory.base.SealedMemoryNode
import memory.types.assignment.goal.values.Amount
import memory.types.assignment.goal.values.Percent
import memory.types.assignment.goal.values.UntilFull
import screeps.api.MemoryMarker

class GoalMemory(
    parent: MemoryMarker,
    selfKey: String
) : SealedMemoryNode<CreepAssignment.EnergyTransfer.Goal>(parent, selfKey) {
    override val mapping: Map<String, ObjectMemoryNode<out CreepAssignment.EnergyTransfer.Goal>> = mapOf(
        sealedNode(UntilFull(parent, selfKey)),
        sealedNode(Amount(parent, selfKey)),
        sealedNode(Percent(parent, selfKey))
    )
}