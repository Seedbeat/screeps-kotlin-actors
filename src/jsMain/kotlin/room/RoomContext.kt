package room

import creep.enums.Role
import creep.isResourceLocked
import creep.roles.setUnassigned
import map.health
import map.mapAround
import memory.lockedObjectId
import memory.resourceSemaphore
import memory.role
import memory.stage
import screeps.api.*
import screeps.api.structures.*
import screeps.utils.contains
import screeps.utils.lazyPerTick
import store.firstNonFilledOrNull
import store.isNonFilled
import utils.*
import utils.log.ILogging
import utils.log.Logging

class RoomContext(
    room: Room,
) : ILogging by Logging("${RoomContext::class.simpleName}:${room.name}") {
    val room: Room by lazyPerTick { Game.rooms[room.name]!! }
    val stage by lazyPerTick { room.memory.stage }
    val semaphore by lazyPerTick { room.memory.resourceSemaphore }

    fun initializeSemaphore(id: String, max: Int) = semaphore.run {
        if (!contains(id)) {
            log.info("Creating semaphore for $id")
            create(id, 0, max)
        } else {
            Game.creeps.values.filter {
                contains(it.memory.lockedObjectId) && it.isResourceLocked
            }.forEach {
                log.info("${it.name} release resource")
                it.setUnassigned(room)
            }

            log.info("Recreating semaphore for $id")
            recreate(id, 0, max)
        }
    }

    val creeps: Array<Creep> by lazyPerTick {
        room.find(FIND_MY_CREEPS)
    }

    val damagedCreeps: Array<Creep> by lazyPerTick {
        creeps.filter { it.hits != it.hitsMax }.sortedBy { it.hits }.toTypedArray()
    }

    val spawns: Array<StructureSpawn> by lazyPerNthTick(99) {
        room.find(FIND_MY_SPAWNS)
    }

    val sources: Array<Source> by lazyPerNthTick(-1) {
        room.find(FIND_SOURCES)
    }

    val availableSourcePoints: Int by lazyPerNthTick(-1) {
        sources.sumOf { room.memory.resourceSemaphore.maximum(it.id) ?: 1 }
    }

    val sourcesContainers: Array<StructureContainer> by lazyPerNthTick(10) {
        val containersMap = containers.associateBy { (it.pos.x to it.pos.y) }
        sources.flatMap { it.mapAround { x, y -> containersMap[x to y] } }.toTypedArray()
    }

    val availableContainersPoints: Int by lazyPerTick {
        sourcesContainers.sumOf { room.memory.resourceSemaphore.maximum(it.id) ?: 1 }
    }

    val constructionSites: Array<ConstructionSite> by lazyPerTick {
        room.find(FIND_MY_CONSTRUCTION_SITES)
    }

    val structuresAll: Array<Structure> by lazyPerTick {
        room.find(FIND_STRUCTURES)
    }

    val structuresMy: Array<Structure> by lazyPerTick {
        structuresAll.filter { it.my }.toTypedArray()
    }

    val hostileStructures: Array<Structure> by lazyPerTick {
        room.find(FIND_HOSTILE_STRUCTURES)
    }

    val hostileCreeps: Array<GenericCreep> by lazyPerTick {
        arrayOf(*room.find(FIND_HOSTILE_CREEPS), *room.find(FIND_HOSTILE_POWER_CREEPS))
    }

    val damagedStructures by lazyPerNthTick(5) {
        structuresAll
            .filter {
                it.structureType != STRUCTURE_CONTROLLER
                        && it.hits > 0 && it.hitsMax > 0
                        && it.health < 95
            }
            .sortedBy { it.health }
            .toTypedArray()
    }

    val damagedTowers: Array<StructureTower> by lazyPerNthTick(5) {
        damagedStructures[STRUCTURE_TOWER]
    }

    val extensions: Array<StructureExtension> by lazyPerNthTick(5) {
        structuresMy[STRUCTURE_EXTENSION]
    }

    val containers: Array<StructureContainer> by lazyPerNthTick(5) {
        structuresAll[STRUCTURE_CONTAINER]
    }

    val roads: Array<StructureRoad> by lazyPerTick {
        structuresAll[STRUCTURE_ROAD]
    }

    val towers: Array<StructureTower> by lazyPerTick {
        structuresAll[STRUCTURE_TOWER]
    }

    val droppedResources: Array<Resource> by lazyPerTick {
        room.find(FIND_DROPPED_RESOURCES)
    }

    val ruinResources: Array<Ruin> by lazyPerTick {
        room.find(FIND_RUINS, options { filter = { it.store.getUsedCapacity() > 0 } })
    }

    val tombstoneResources: Array<Tombstone> by lazyPerTick {
        room.find(FIND_TOMBSTONES, options { filter = { it.store.getUsedCapacity() > 0 } })
    }

    val isSourcePresent get() = sources.isNotEmpty() || sourcesContainers.isNotEmpty()

    val isEnergyFull get() = room.energyCapacityAvailable == room.energyAvailable

    val isStorageExist get() = room.storage != null

    val isSourcesCarriedByContainers by lazyPerNthTick(10) {
        sourcesContainers.count() >= sources.sumOf { room.memory.resourceSemaphore.maximum(it.id) ?: 0 }
    }


    operator fun Array<Creep>.get(role: Role) = count { it.memory.role == role }

    private operator fun <T> Array<Structure>.get(type: StructureConstant) =
        filter { it.structureType == type }.unsafeCast<List<T>>().toTypedArray()

    private operator fun <T> Iterable<Structure>.get(type: StructureConstant) =
        filter { it.structureType == type }.unsafeCast<List<T>>().toTypedArray()

    //

    fun <T : RoomObject> HasPosition.findClosestBase(targets: Array<T>, byRange: Boolean): T? {
        if (targets.isEmpty())
            return null

        return if (byRange) pos.findClosestByRange(targets) else pos.findClosestByPath(targets)
    }

    fun HasPosition.findClosestAvailableSource(): Source? {
        return pos.findClosestByPath(sources, options {
            filter = { foundObject -> semaphore.isAvailable(foundObject.id) ?: true }
        })
    }

    fun HasPosition.findClosestDamagedStructure(byRange: Boolean = false): Structure? {
        return findClosestBase(damagedStructures.take(3).toTypedArray(), byRange)
    }

    fun HasPosition.findClosestDamagedTower(byRange: Boolean = false): Structure? {
        return findClosestBase(damagedTowers.take(3).toTypedArray(), byRange)
    }

    fun HasPosition.findClosestDamagedCreep(byRange: Boolean = false): GenericCreep? {
        return findClosestBase(damagedCreeps.take(3).toTypedArray(), byRange)
    }

    fun HasPosition.findClosestEnemy(byRange: Boolean = false): GenericCreep? {
        return findClosestBase(hostileCreeps, byRange)
    }

    fun <T : StoreOwner> HasPosition.findClosestEnergyLack(): T? {
        val structure = if (extensions.any { it.store.isNonFilled(RESOURCE_ENERGY) })
            pos.findClosestByPath(extensions, options {
                filter = { it.store.isNonFilled(RESOURCE_ENERGY) }
            })
        else {
            towers.firstNonFilledOrNull(RESOURCE_ENERGY)
                ?: spawns.firstNonFilledOrNull(RESOURCE_ENERGY)
        }

        return structure.unsafeCast<T?>()
    }

    fun HasPosition.findClosestNonEmptyContainer(resource: ResourceConstant): StructureContainer? {
        return pos.findClosestByPath(sourcesContainers, options {
            filter = { it.store.getUsedCapacity(resource) >= 50 && semaphore.isAvailable(it.id) ?: true }
        })
    }
}
