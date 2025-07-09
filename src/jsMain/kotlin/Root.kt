import room.RoomContext
import scheduler.EventScheduler
import screeps.api.Game
import screeps.api.component1
import screeps.api.component2
import screeps.api.iterator
import screeps.utils.lazyPerTick
import utils.CpuLogger
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging
import kotlin.collections.set

object Root : ILogging by Logging<Root>(LogLevel.DEBUG) {

    val localTime: Int by lazyPerTick { Game.time % 100 }

    private val roomContext = mutableMapOf<String, RoomContext>()

    fun rooms(): Map<String, RoomContext> = roomContext
    fun room(name: String): RoomContext = roomContext[name]!!

    fun gameLoop() {
        log.info("=============================== Tick: ${Game.time} ===============================")
        CpuLogger.init()

        CpuLogger.markStart("ContextCreation")
        for ((name, room) in Game.rooms) {
            if (!roomContext.containsKey(name))
                roomContext[name] = RoomContext(room)
        }
        CpuLogger.markEnd("ContextCreation")

        CpuLogger.markStart("EventScheduler")
        EventScheduler.execute()
        CpuLogger.markEnd("EventScheduler")

        CpuLogger.marks().takeIf { it.isNotEmpty() }?.run {
            forEach { (_, mark) -> log.info(mark.name().padEnd(40), "CPU:", mark.diff) }
        }

        // try to generate pixel if bucket is filled
        if (Game.cpu.bucket >= 10000)
            Game.cpu.generatePixel()

        log.info("=============================== CPU: ${CpuLogger.total()} ===============================")
    }
}