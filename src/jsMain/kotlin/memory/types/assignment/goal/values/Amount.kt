package memory.types.assignment.goal.values

import creep.CreepAssignment
import memory.base.ObjectMemoryNode
import memory.delegates.memoryNodeValue
import screeps.api.MemoryMarker

class Amount(
    parent: MemoryMarker,
    selfKey: String
) : ObjectMemoryNode<CreepAssignment.EnergyTransfer.Goal.Amount>(parent, selfKey) {

    private var amount: Int? by memoryNodeValue()

    override fun read(): CreepAssignment.EnergyTransfer.Goal.Amount? {
        return CreepAssignment.EnergyTransfer.Goal.Amount(
            amount ?: return null
        )
    }

    override fun write(value: CreepAssignment.EnergyTransfer.Goal.Amount) {
        amount = value.amount
    }

    override fun clear() {
        deleteNodeValue(::amount.name)
    }
}