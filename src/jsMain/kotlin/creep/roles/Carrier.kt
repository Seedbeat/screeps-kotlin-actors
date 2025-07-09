package creep.roles

import screeps.api.Creep
import screeps.api.RESOURCE_ENERGY

fun Creep.delivery() = containerWorkerBase(
    targetSearch = {
        room.storage?.let { if (it.store.getUsedCapacity(RESOURCE_ENERGY)!! < 500_000) it else null }
    },
    targetAction = { target -> transfer(target, RESOURCE_ENERGY) },
    preAction = { _, _ -> renewCheck() }
)