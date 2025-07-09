package creep.roles

import creep.wait
import screeps.api.Creep
import screeps.api.RESOURCES_ALL

fun Creep.sweep() = workerBase(
    sourceSearch = { droppedResources.firstOrNull().also { if (it == null) wait() } },
    sourceAction = { pickup(it) },
    targetSearch = { room.storage },
    targetAction = { target ->
        RESOURCES_ALL.map { transfer(target, it) }.sortedBy { it.hashCode() }.distinct().first()
    }
)