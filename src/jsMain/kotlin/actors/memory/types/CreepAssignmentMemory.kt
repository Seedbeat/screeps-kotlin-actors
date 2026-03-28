package actors.memory.types

import actors.CreepAssignment
import actors.assignments.CreepAssignmentType
import actors.memory.base.ObjectMemoryNode
import actors.memory.delegates.memoryNodeEnum
import screeps.api.MemoryMarker

class CreepAssignmentMemory(
    parent: MemoryMarker,
    selfKey: String
) : ObjectMemoryNode<CreepAssignment>(parent, selfKey) {

    private var kind: CreepAssignmentType by memoryNodeEnum { CreepAssignmentType.NONE }

    private val controllerUpkeep = ControllerUpkeepAssignmentMemory(parent, selfKey)
    private val construction = ConstructionAssignmentMemory(parent, selfKey)


    override fun read(): CreepAssignment? = when (kind) {
        CreepAssignmentType.NONE -> null
        CreepAssignmentType.CONTROLLER_UPKEEP -> controllerUpkeep.value
        CreepAssignmentType.CONSTRUCTION -> construction.value
    }

    override fun write(value: CreepAssignment) = when (value) {
        is CreepAssignment.ControllerUpkeep -> {
            kind = CreepAssignmentType.CONTROLLER_UPKEEP
            controllerUpkeep.value = value
        }

        is CreepAssignment.Construction -> {
            kind = CreepAssignmentType.CONSTRUCTION
            construction.value = value
        }

        is CreepAssignment.EnergyTransfer ->
            error("CreepAssignment.EnergyKeep persistence is not implemented yet")
    }

    override fun clear() = when (kind) {
        CreepAssignmentType.NONE -> null
        CreepAssignmentType.CONTROLLER_UPKEEP -> controllerUpkeep
        CreepAssignmentType.CONSTRUCTION -> construction
    }.let { memoryNode ->
        memoryNode?.value = null
        kind = CreepAssignmentType.NONE
    }

}
