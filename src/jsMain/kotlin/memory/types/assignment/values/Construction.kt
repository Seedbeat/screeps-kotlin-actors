package memory.types.assignment.values

import creep.CreepAssignment
import creep.enums.CreepAssignmentPhase
import memory.base.ObjectMemoryNode
import memory.delegates.memoryNodeEnum
import memory.delegates.memoryNodeValue
import screeps.api.MemoryMarker

class Construction(
    parent: MemoryMarker,
    selfKey: String
) : ObjectMemoryNode<CreepAssignment.Construction>(parent, selfKey) {
    var roomName: String? by memoryNodeValue()
    var constructionSiteId: String? by memoryNodeValue()
    var phase: CreepAssignmentPhase? by memoryNodeEnum()

    override fun read(): CreepAssignment.Construction? = CreepAssignment.Construction(
        roomName = roomName ?: return null,
        constructionSiteId = constructionSiteId ?: return null,
        phase = phase ?: return null
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