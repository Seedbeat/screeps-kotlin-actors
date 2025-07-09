package creep.roles

import map.health
import screeps.api.Creep

fun Creep.repair() = stagedSourceWorkerBase(
    targetSearch = {
        findClosestDamagedTower() ?: findClosestDamagedStructure()
    },
    targetAction = { target ->
        if (target.health > 99)
            this@repair.setUnassigned(room)

        repair(target)
    }
)