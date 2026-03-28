package actors.memory.types

import actors.RoomPlanningCache
import actors.memory.base.ObjectMemoryNode
import actors.memory.delegates.memoryNodeEnum
import actors.memory.delegates.memoryNodeValue
import room.RoomStage
import screeps.api.MemoryMarker

class RoomPlanningCacheMemory(
    parent: MemoryMarker,
    selfKey: String
) : ObjectMemoryNode<RoomPlanningCache>(parent, selfKey) {

    var updatedAt: Int? by memoryNodeValue()
    var stage: RoomStage by memoryNodeEnum { RoomStage.Uninitialized }
    var controllerLevel: Int? by memoryNodeValue()
    var energyCapacityAvailable: Int? by memoryNodeValue()
    var spawnCount: Int? by memoryNodeValue()
    var extensionCount: Int? by memoryNodeValue()
    var towerCount: Int? by memoryNodeValue()
    var hasStorage: Boolean? by memoryNodeValue()
    var hasTerminal: Boolean? by memoryNodeValue()
    var constructionSiteCount: Int? by memoryNodeValue()
    var remainingConstructionWork: Int? by memoryNodeValue()

    override fun read(): RoomPlanningCache? = RoomPlanningCache(
        updatedAt = updatedAt ?: return null,
        stage = stage,
        controllerLevel = controllerLevel ?: return null,
        energyCapacityAvailable = energyCapacityAvailable ?: return null,
        spawnCount = spawnCount ?: return null,
        extensionCount = extensionCount ?: return null,
        towerCount = towerCount ?: return null,
        hasStorage = hasStorage ?: return null,
        hasTerminal = hasTerminal ?: return null,
        constructionSiteCount = constructionSiteCount ?: return null,
        remainingConstructionWork = remainingConstructionWork ?: return null
    )

    override fun write(value: RoomPlanningCache) {
        updatedAt = value.updatedAt
        stage = value.stage
        controllerLevel = value.controllerLevel
        energyCapacityAvailable = value.energyCapacityAvailable
        spawnCount = value.spawnCount
        extensionCount = value.extensionCount
        towerCount = value.towerCount
        hasStorage = value.hasStorage
        hasTerminal = value.hasTerminal
        constructionSiteCount = value.constructionSiteCount
        remainingConstructionWork = value.remainingConstructionWork
    }

    override fun clear() {
        deleteNodeValue(::updatedAt.name)
        deleteNodeValue(::stage.name)
        deleteNodeValue(::controllerLevel.name)
        deleteNodeValue(::energyCapacityAvailable.name)
        deleteNodeValue(::spawnCount.name)
        deleteNodeValue(::extensionCount.name)
        deleteNodeValue(::towerCount.name)
        deleteNodeValue(::hasStorage.name)
        deleteNodeValue(::hasTerminal.name)
        deleteNodeValue(::constructionSiteCount.name)
        deleteNodeValue(::remainingConstructionWork.name)
    }
}
