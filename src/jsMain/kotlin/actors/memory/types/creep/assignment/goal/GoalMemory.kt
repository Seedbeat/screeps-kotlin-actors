package actors.memory.types.creep.assignment.goal

import actors.CreepAssignment
import actors.memory.base.ObjectMemoryNode
import actors.memory.base.SealedMemoryNode
import actors.memory.types.creep.assignment.goal.values.Amount
import actors.memory.types.creep.assignment.goal.values.Percent
import actors.memory.types.creep.assignment.goal.values.UntilFull
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