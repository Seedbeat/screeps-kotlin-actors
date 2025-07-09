package creep.roles

import screeps.api.Creep
import screeps.api.OK
import screeps.api.RESOURCES_ALL
import screeps.api.RESOURCE_ENERGY

fun Creep.sweep() = workerBase(
    sourceSearch = { droppedResources.firstOrNull() },
    sourceAction = { pickup(it) },
    targetSearch = { room.storage },
    targetAction = { target ->
        RESOURCES_ALL.map { transfer(target, it) }.sortedBy { it.hashCode() }.distinct().first()
    }
)