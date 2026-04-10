package memory.types.assignment.goal.values

import creep.CreepAssignment
import memory.base.ObjectMemoryNode
import memory.delegates.memoryNodeValue
import screeps.api.MemoryMarker

class Percent(
    parent: MemoryMarker,
    selfKey: String
) : ObjectMemoryNode<CreepAssignment.EnergyTransfer.Goal.Percent>(parent, selfKey) {

    private var percent: Int? by memoryNodeValue()

    override fun read(): CreepAssignment.EnergyTransfer.Goal.Percent? {
        return CreepAssignment.EnergyTransfer.Goal.Percent(
            percent ?: return null
        )
    }

    override fun write(value: CreepAssignment.EnergyTransfer.Goal.Percent) {
        percent = value.percentage
    }

    override fun clear() {
        deleteNodeValue(::percent.name)
    }
}