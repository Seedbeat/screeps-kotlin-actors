package scheduler.events

import Root
import Settings
import invoke
import map.health
import memory.role
import room.RoomContext
import scheduler.IEvent
import screeps.api.Game
import utils.log.ILogger
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

object Statistics : IEvent, ILogging by Logging<Statistics>(LogLevel.INFO) {
    override val isEnabled: Boolean get() = Settings.ShowStatistics()

    override fun execute() {
        val heap = Game.cpu.getHeapStatistics()

        val totalMemory = heap["heap_size_limit"].unsafeCast<Int>() / 1024
        val availableMemory = heap["total_available_size"].unsafeCast<Int>() / 1024
        val usedMemory = heap["externally_allocated_size"].unsafeCast<Int>() / 1024

        log.info("USER:")
        log.info("    GCL    :", "Level:", Game.gcl.level, ", Progress:", Game.gcl.progress, "/", Game.gcl.progressTotal)
        log.info("    GPL    :", "Level:", Game.gpl.level, ", Progress:", Game.gpl.progress, "/", Game.gpl.progressTotal)
        log.info("    BUCKET :", Game.cpu.bucket)
        log.info("    MEMORY :", "Used:", usedMemory, "KB", "/", totalMemory, "KB, Available:", availableMemory, "KB")
        log.info("----------------------------------------------------------")

        Root.rooms().values.forEach { printRoomStatistics(log, it) }

    }

    fun printRoomStatistics(log: ILogger, context: RoomContext) = with(context) {
        log.info("[${room.name}]")
        log.info("OVERALL:")
        log.info("    STAGE :", stage)
        log.info("    RCL   :", room.controller?.level ?: "unknown")
        log.info("    ENERGY:", room.energyAvailable, "/", room.energyCapacityAvailable)
        log.info()
        log.info("CREEPS:")
        creeps.groupBy { it.memory.role }.mapValues { it.value.count() }.forEach { (role, count) ->
            log.info("    ${role.toString().padEnd(10)}:", count)
        }
        log.info("    --------------------")
        log.info("    ALL      :", creeps.count())
        log.info("    HOSTILE  :", hostileCreeps.count())
        log.info("    DAMAGED  :", damagedCreeps.count())
        log.info()
        log.info("STRUCTURES:")
        log.info("    MY       :", structuresMy.count())
        log.info("    BUILDING :", constructionSites.count())
        log.info("    HP < 100%:", damagedStructures.count())
        log.info("    HP <  80%:", damagedStructures.count { it.health < 80 })
        log.warn("    HP <  50%:", damagedStructures.count { it.health < 50 })
        log.warn("    HP <  25%:", damagedStructures.count { it.health < 25 })
        log.info("    --------------------")
        log.info("    ALL      :", structuresAll.count())
        log.info("----------------------------------------------------------")
    }
}