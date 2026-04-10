package memory.types

import memory.base.ObjectMemoryNode
import memory.delegates.memoryNodeEnum
import memory.delegates.memoryNodeValue
import room.enums.RoomStage
import room.planning.RoomPlanningCache
import screeps.api.MemoryMarker

class RoomPlanningCacheMemory(
    parent: MemoryMarker,
    selfKey: String
) : ObjectMemoryNode<RoomPlanningCache>(parent, selfKey) {

    var updatedAt: Int? by memoryNodeValue()
    var stage: RoomStage by memoryNodeEnum { RoomStage.Uninitialized }
    var controllerLevel: Int? by memoryNodeValue()
    var energyCapacityAvailable: Int? by memoryNodeValue()
    var sourceCount: Int? by memoryNodeValue()
    var totalSourceOpenSides: Int? by memoryNodeValue()
    var sustainableIncome: Int? by memoryNodeValue()
    var bufferedEnergy: Int? by memoryNodeValue()
    var spawnCount: Int? by memoryNodeValue()
    var extensionCount: Int? by memoryNodeValue()
    var towerCount: Int? by memoryNodeValue()
    var spawnEnergyDeficit: Int? by memoryNodeValue()
    var extensionEnergyDeficit: Int? by memoryNodeValue()
    var towerEnergyDeficit: Int? by memoryNodeValue()
    var hasStorage: Boolean? by memoryNodeValue()
    var hasTerminal: Boolean? by memoryNodeValue()
    var controllerTicksToDowngrade: Int? by memoryNodeValue()
    var constructionSiteCount: Int? by memoryNodeValue()
    var remainingConstructionWork: Int? by memoryNodeValue()
    var weightedRemainingConstructionWork: Int? by memoryNodeValue()
    var criticalConstructionSiteCount: Int? by memoryNodeValue()
    var criticalConstructionWork: Int? by memoryNodeValue()
    var economyConstructionSiteCount: Int? by memoryNodeValue()
    var economyConstructionWork: Int? by memoryNodeValue()
    var lowPriorityConstructionSiteCount: Int? by memoryNodeValue()
    var lowPriorityConstructionWork: Int? by memoryNodeValue()

    override fun read(): RoomPlanningCache? = RoomPlanningCache(
        updatedAt = updatedAt ?: return null,
        stage = stage,
        controllerLevel = controllerLevel ?: return null,
        energyCapacityAvailable = energyCapacityAvailable ?: return null,
        sourceCount = sourceCount ?: return null,
        totalSourceOpenSides = totalSourceOpenSides ?: return null,
        sustainableIncome = sustainableIncome ?: return null,
        bufferedEnergy = bufferedEnergy ?: return null,
        spawnCount = spawnCount ?: return null,
        extensionCount = extensionCount ?: return null,
        towerCount = towerCount ?: return null,
        spawnEnergyDeficit = spawnEnergyDeficit ?: return null,
        extensionEnergyDeficit = extensionEnergyDeficit ?: return null,
        towerEnergyDeficit = towerEnergyDeficit ?: return null,
        hasStorage = hasStorage ?: return null,
        hasTerminal = hasTerminal ?: return null,
        controllerTicksToDowngrade = controllerTicksToDowngrade ?: return null,
        constructionSiteCount = constructionSiteCount ?: return null,
        remainingConstructionWork = remainingConstructionWork ?: return null,
        weightedRemainingConstructionWork = weightedRemainingConstructionWork ?: return null,
        criticalConstructionSiteCount = criticalConstructionSiteCount ?: return null,
        criticalConstructionWork = criticalConstructionWork ?: return null,
        economyConstructionSiteCount = economyConstructionSiteCount ?: return null,
        economyConstructionWork = economyConstructionWork ?: return null,
        lowPriorityConstructionSiteCount = lowPriorityConstructionSiteCount ?: return null,
        lowPriorityConstructionWork = lowPriorityConstructionWork ?: return null
    )

    override fun write(value: RoomPlanningCache) {
        updatedAt = value.updatedAt
        stage = value.stage
        controllerLevel = value.controllerLevel
        energyCapacityAvailable = value.energyCapacityAvailable
        sourceCount = value.sourceCount
        totalSourceOpenSides = value.totalSourceOpenSides
        sustainableIncome = value.sustainableIncome
        bufferedEnergy = value.bufferedEnergy
        spawnCount = value.spawnCount
        extensionCount = value.extensionCount
        towerCount = value.towerCount
        spawnEnergyDeficit = value.spawnEnergyDeficit
        extensionEnergyDeficit = value.extensionEnergyDeficit
        towerEnergyDeficit = value.towerEnergyDeficit
        hasStorage = value.hasStorage
        hasTerminal = value.hasTerminal
        controllerTicksToDowngrade = value.controllerTicksToDowngrade
        constructionSiteCount = value.constructionSiteCount
        remainingConstructionWork = value.remainingConstructionWork
        weightedRemainingConstructionWork = value.weightedRemainingConstructionWork
        criticalConstructionSiteCount = value.criticalConstructionSiteCount
        criticalConstructionWork = value.criticalConstructionWork
        economyConstructionSiteCount = value.economyConstructionSiteCount
        economyConstructionWork = value.economyConstructionWork
        lowPriorityConstructionSiteCount = value.lowPriorityConstructionSiteCount
        lowPriorityConstructionWork = value.lowPriorityConstructionWork
    }

    override fun clear() {
        deleteNodeValue(::updatedAt.name)
        deleteNodeValue(::stage.name)
        deleteNodeValue(::controllerLevel.name)
        deleteNodeValue(::energyCapacityAvailable.name)
        deleteNodeValue(::sourceCount.name)
        deleteNodeValue(::totalSourceOpenSides.name)
        deleteNodeValue(::sustainableIncome.name)
        deleteNodeValue(::bufferedEnergy.name)
        deleteNodeValue(::spawnCount.name)
        deleteNodeValue(::extensionCount.name)
        deleteNodeValue(::towerCount.name)
        deleteNodeValue(::spawnEnergyDeficit.name)
        deleteNodeValue(::extensionEnergyDeficit.name)
        deleteNodeValue(::towerEnergyDeficit.name)
        deleteNodeValue(::hasStorage.name)
        deleteNodeValue(::hasTerminal.name)
        deleteNodeValue(::controllerTicksToDowngrade.name)
        deleteNodeValue(::constructionSiteCount.name)
        deleteNodeValue(::remainingConstructionWork.name)
        deleteNodeValue(::weightedRemainingConstructionWork.name)
        deleteNodeValue(::criticalConstructionSiteCount.name)
        deleteNodeValue(::criticalConstructionWork.name)
        deleteNodeValue(::economyConstructionSiteCount.name)
        deleteNodeValue(::economyConstructionWork.name)
        deleteNodeValue(::lowPriorityConstructionSiteCount.name)
        deleteNodeValue(::lowPriorityConstructionWork.name)
    }
}
