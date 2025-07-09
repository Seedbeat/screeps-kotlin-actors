package creep.roles

import memory.workObjectId
import screeps.api.*
import screeps.api.structures.StructureContainer
import store.isFull

fun Creep.mine() = workerBase(
    sourceSearch = { findClosestAvailableSource() },
    sourceAction = { source ->
        val container = if (memory.workObjectId.isEmpty()) {
            val look = room.lookAt(pos).firstOrNull { it.structure?.structureType == STRUCTURE_CONTAINER }

            if (look != null) {
                memory.workObjectId = look.structure!!.id
                Game.getObjectById<StructureContainer>(memory.workObjectId)
            } else {
                null
            }
        } else {
            Game.getObjectById(memory.workObjectId)
        }

        if (container?.store?.isFull(RESOURCE_ENERGY) == true)
            OK
        else
            harvest(source)
    },
    targetSearch = { null },
    targetAction = { ERR_INVALID_TARGET }
)