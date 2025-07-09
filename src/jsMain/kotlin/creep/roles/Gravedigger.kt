package creep.roles

import creep.wait
import screeps.api.Creep
import screeps.api.RESOURCES_ALL

fun Creep.dig() = workerBase(
    sourceSearch = {
        (tombstoneResources.firstOrNull() ?: ruinResources.firstOrNull()).also { if (it == null) wait() }
    },
    sourceAction = { source ->
        RESOURCES_ALL.map { withdraw(source, it) }.sortedBy { it.hashCode() }.distinct().first()
    },
    targetSearch = { room.storage },
    targetAction = { target ->
        RESOURCES_ALL.map { transfer(target, it) }.sortedBy { it.hashCode() }.distinct().first()
    }
)