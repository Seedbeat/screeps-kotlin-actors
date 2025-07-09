package creep.roles

import screeps.api.Creep
import screeps.api.RESOURCE_ENERGY

fun Creep.harvest() = sourceWorkerBase(
    targetSearch = { findClosestEnergyLack() },
    targetAction = { target -> transfer(target, RESOURCE_ENERGY) }
)