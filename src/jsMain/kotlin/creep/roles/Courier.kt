package creep.roles

import screeps.api.Creep
import screeps.api.RESOURCE_ENERGY
import store.isNonFilled

fun Creep.carry() = stagedContainerWorkerBase(
    targetSearch = { findClosestEnergyLack() },
    targetCheck = { target -> target.store.isNonFilled(RESOURCE_ENERGY) },
    targetAction = { target -> transfer(target, RESOURCE_ENERGY) },
    preAction = { _, _ -> renewCheck() }
)
