package creep.roles

import screeps.api.Creep
import screeps.api.RESOURCE_ENERGY
import screeps.api.options
import store.firstNonFilledOrNull
import store.isNonEmpty
import store.isNonFilled

fun Creep.harvest() = sourceWorkerBase(
    targetSearch = { findClosestEnergyLack() },
    targetAction = { target -> transfer(target, RESOURCE_ENERGY) }
)