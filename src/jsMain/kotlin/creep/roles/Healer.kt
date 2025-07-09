package creep.roles

import Root
import creep.wait
import screeps.api.Creep
import screeps.api.ERR_INVALID_TARGET

fun Creep.heal() = workerBase(
    sourceSearch = {
        val creep = findClosestDamagedCreep()
        if (creep == null) {
            wait()
            null
        } else creep
    },
    sourceAction = { target -> heal(target) },
    targetSearch = { null },
    targetAction = { ERR_INVALID_TARGET },
    preAction = { _, _ ->
        if (Root.room(room.name).damagedCreeps.isEmpty())
            setUnassigned(room)
        false
    }
)