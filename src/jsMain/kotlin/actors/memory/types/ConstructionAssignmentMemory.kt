package actors.memory.types

import actors.CreepAssignment
import actors.assignments.CreepAssignmentPhase
import actors.memory.base.ObjectMemoryNode
import actors.memory.delegates.memoryNodeEnum
import actors.memory.delegates.memoryNodeValue
import screeps.api.MemoryMarker

class ConstructionAssignmentMemory(
    parent: MemoryMarker,
    selfKey: String
) : ObjectMemoryNode<CreepAssignment.Construction>(parent, selfKey) {
    var roomName: String? by memoryNodeValue()
    var constructionSiteId: String? by memoryNodeValue()
    var phase: CreepAssignmentPhase by memoryNodeEnum { CreepAssignmentPhase.HARVEST }

    override fun read(): CreepAssignment.Construction? = CreepAssignment.Construction(
        roomName = roomName ?: return null,
        constructionSiteId = constructionSiteId ?: return null,
        phase = phase
    )

    override fun write(value: CreepAssignment.Construction) {
        roomName = value.roomName
        constructionSiteId = value.constructionSiteId
        phase = value.phase
    }

    override fun clear() {
        deleteNodeValue(::roomName.name)
        deleteNodeValue(::constructionSiteId.name)
        deleteNodeValue(::phase.name)
    }
}