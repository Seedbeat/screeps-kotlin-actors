package creep.roles

import creep.enums.State
import creep.wait
import map.waitPosition
import memory.state
import screeps.api.Creep
import screeps.api.ERR_INVALID_TARGET

fun Creep.attack() = workerBase(
    sourceSearch = {
        val creep = findClosestEnemy()
        if (creep == null) {
            wait()
            null
        } else creep
    },
    sourceAction = { target -> attack(target) },
    targetSearch = { null },
    targetAction = { ERR_INVALID_TARGET },
    preAction = { _, _ ->
        when (memory.state) {
            State.WAIT, State.UNASSIGNED -> {
                val waitPosition = waitPosition?.pos ?: return@workerBase false
                if (pos.inRangeTo(waitPosition, 4)) {
                    memory.state = State.RECYCLE
                    return@workerBase true
                }
            }

            else -> Unit
        }

        return@workerBase false
    }
)
