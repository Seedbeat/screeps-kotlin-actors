package creep.roles

import Root
import creep.enums.Role
import creep.enums.State
import creep.log
import creep.moveToNavTarget
import creep.unlockResource
import memory.role
import memory.state
import screeps.api.*
import store.isEmpty

fun Creep.recycle() {
    unlockResource(room)
    say("♻️")
    val spawn = Root.rooms()[room.name]?.spawns?.let { pos.findClosestByPath(it) } ?: return
    val code = spawn.recycleCreep(this)
    when {
        code == ERR_NOT_IN_RANGE -> moveToNavTarget(spawn)
        else -> return
    }
}

fun Creep.renew() {
    unlockResource(room)
    say("🔄")

    val spawn = Root.rooms()[room.name]?.spawns?.let { pos.findClosestByPath(it) } ?: return

    when (val code = spawn.renewCreep(this)) {
        ERR_BUSY,
        ERR_NOT_IN_RANGE -> moveToNavTarget(spawn)

        ERR_NOT_ENOUGH_ENERGY,
        ERR_FULL -> setUnassigned(room)

        OK -> if (ticksToLive == CREEP_LIFE_TIME) setUnassigned(room)

        else -> log.error("Unknown code:", code)
    }
}

fun Creep.renewCheck(): Boolean {
    if (this.ticksToLive > 200 || room.energyAvailable < 400)
        return false

    val bodyToCheck = Role.getMostValuableBody(memory.role)

    if (bodyToCheck.contentEquals(body.map { it.type }.toTypedArray())) {
        memory.state = State.RENEW
        return true
    }
    return false
}