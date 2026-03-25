package creep

import Settings
import creep.enums.State
import invoke
import memory.lockedObjectId
import memory.state
import room.acquireResource
import room.releaseResource
import screeps.api.*
import screeps.utils.lazyPerTick
import utils.log.ILogger
import utils.log.LogLevel
import utils.log.Logging

val Creep.log: ILogger by lazyPerTick { Logging(this.name, LogLevel.ERROR).log }
private val showPath by lazyPerTick { Settings.ShowCreepPath() }

fun Creep.wait(target: HasPosition? = Game.flags["PausePoint"]): HasPosition? {
    if (target != null) {
        val code = this.moveToNavTarget(target)
        if (code != OK && code != ERR_TIRED) {
            say("ERR: $code")
        } else {
            state(State.WAIT)
        }
    } else {
        say("WAIT ERR!")
    }
    return target
}

fun Creep.moveToNavTarget(target: NavigationTarget): ScreepsReturnCode {
    val style = if (showPath) options<RoomVisual.LineStyle> {
        lineStyle = LINE_STYLE_DASHED
        color = "#aaaaaa"
    } else null

    return moveTo(target, options { visualizePathStyle = style; })

//    val code = moveTo(target, options { visualizePathStyle = style; noPathFinding = true })
//
//    return if (code == ERR_NOT_FOUND) {
//        moveTo(target, options { visualizePathStyle = style; noPathFinding = false })
//    } else code
}


fun Creep.lockResource(room: Room, resourceId: String) {
    if (resourceId == memory.lockedObjectId)
        return

    memory.lockedObjectId = resourceId
    room.acquireResource(this.name, resourceId)
}

fun Creep.unlockResource(room: Room) {
    log.debug("unlock ${memory.lockedObjectId}")

    if (isResourceLocked) {
        room.releaseResource(this.name, memory.lockedObjectId!!)
        memory.lockedObjectId = ""
    }
}

val Creep.isResourceLocked get() = memory.lockedObjectId != null

fun <T : Identifiable> Creep.getLockedResource(): T? =
    if (isResourceLocked)
        Game.getObjectById(memory.lockedObjectId) else null

val Creep.state: Enum<State>
    get() = memory.state

fun Creep.state(state: State) {
    memory.state = state
}