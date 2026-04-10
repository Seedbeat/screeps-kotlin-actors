package creep

import Settings
import creep.CreepCapabilities.Companion.from
import invoke
import screeps.api.*
import utils.lazyOnce
import utils.lazyPerTick
import utils.log.ILogger
import utils.log.LogLevel
import utils.log.Logging

val Creep.log: ILogger by lazyPerTick { Logging(this.name, LogLevel.ERROR).log }
private val showPath by lazyPerTick { Settings.ShowCreepPath() }

fun Creep.moveToNavTarget(target: NavigationTarget): ScreepsReturnCode {
    val style = if (showPath) options<RoomVisual.LineStyle> {
        lineStyle = LINE_STYLE_DASHED
        color = "#aaaaaa"
    } else null

    return moveTo(target, options { visualizePathStyle = style; })
}

val Creep.capabilities: CreepCapabilities by lazyOnce(::from)