package room

import Root
import screeps.api.*
import screeps.api.structures.*
import utils.cache.CachedArrayInstance

open class RoomStructures(
    val room: Room,
    val initial: FindConstant<Structure>
) {
    val structuresCache = CachedArrayInstance<Structure>(lifetime = Root.LOCAL_TIME_MAX)

    val all: Array<Structure>
        get() = structuresCache.getOrPut(firstKey = room.name, secondKey = initial) {
            room.find(findConstant = initial)
        }

    inline fun <reified T : Structure> getStructures(type: StructureConstant): Array<T> =
        structuresCache.getOrPutTyped(firstKey = room.name, secondKey = type) {
            all.filter { it.structureType == type }.toTypedArray().unsafeCast<Array<T>>()
        }

    inline fun <reified T : Structure> getStructure(type: StructureConstant): T? =
        structuresCache.getOrPutTyped(firstKey = room.name, secondKey = type) {
            arrayOf(all.firstOrNull { it.structureType == type }).unsafeCast<Array<T>>()
        }.firstOrNull()

    val spawns: Array<StructureSpawn> get() = getStructures(STRUCTURE_SPAWN)
    val extensions: Array<StructureExtension> get() = getStructures(STRUCTURE_EXTENSION)
    val roads: Array<StructureRoad> get() = getStructures(STRUCTURE_ROAD)
    val walls: Array<StructureWall> get() = getStructures(STRUCTURE_WALL)
    val ramparts: Array<StructureRampart> get() = getStructures(STRUCTURE_RAMPART)
    val keeperLairs: Array<StructureKeeperLair> get() = getStructures(STRUCTURE_KEEPER_LAIR)
    val portals: Array<StructurePortal> get() = getStructures(STRUCTURE_PORTAL)
    val controllers: Array<StructureController> get() = getStructures(STRUCTURE_CONTROLLER)
    val links: Array<StructureLink> get() = getStructures(STRUCTURE_LINK)
    val storage: StructureStorage? get() = getStructure(STRUCTURE_STORAGE)
    val towers: Array<StructureTower> get() = getStructures(STRUCTURE_TOWER)
    val observers: Array<StructureObserver> get() = getStructures(STRUCTURE_OBSERVER)
    val powerBanks: Array<StructurePowerBank> get() = getStructures(STRUCTURE_POWER_BANK)
    val powerSpawns: Array<StructurePowerSpawn> get() = getStructures(STRUCTURE_POWER_SPAWN)
    val extractors: Array<StructureExtractor> get() = getStructures(STRUCTURE_EXTRACTOR)
    val labs: Array<StructureLab> get() = getStructures(STRUCTURE_LAB)
    val terminal: StructureTerminal? get() = getStructure(STRUCTURE_TERMINAL)
    val containers: Array<StructureContainer> get() = getStructures(STRUCTURE_CONTAINER)
    val nukers: Array<StructureNuker> get() = getStructures(STRUCTURE_NUKER)
    val factories: Array<StructureFactory> get() = getStructures(STRUCTURE_FACTORY)
    val invaderCores: Array<StructureInvaderCore> get() = getStructures(STRUCTURE_INVADER_CORE)
}