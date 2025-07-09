package creep.roles

import screeps.api.Creep
import screeps.api.RESOURCE_ENERGY

fun Creep.carry() = stagedContainerWorkerBase(
    targetSearch = { findClosestEnergyLack() },
    targetAction = { target -> transfer(target, RESOURCE_ENERGY) },
    preAction = { _, _ -> renewCheck() }
)
