import actor.ActorSystem
import actors.ActorConstants.SYSTEM
import actors.SystemActor
import actors.SystemCommand
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

object Root : ILogging by Logging<Root>(LogLevel.DEBUG) {

    var wasReset = true

    val localTime: Int by lazyPerTick { Game.time % 100 }

    private val roomContext = mutableMapOf<String, RoomContext>()

    fun rooms(): Map<String, RoomContext> = roomContext
    fun room(name: String): RoomContext = roomContext[name]!!

    fun gameLoop() {
        if (wasReset)
            onReset()

        preLoop()
        loop()
        postLoop()
        wasReset = false
    }

    fun onReset() {
        log.warn("RESET RESET RESET RESET RESET RESET RESET RESET RESET RESET RESET RESET RESET RESET RESET")
        ActorSystem.spawn(SYSTEM, ::SystemActor)
        ActorSystem.send(SYSTEM, Root::class.simpleName!!, SystemCommand.Bootstrap)
    }

    fun preLoop() {
        log.info("=============================== Tick: ${Game.time} ===============================")
        CpuLogger.init()
    }

    fun loop() {
        CpuLogger.mark("ContextCreation") {
            for ((name, room) in Game.rooms) {
                if (!roomContext.containsKey(name))
                    roomContext[name] = RoomContext(room)
            }
        }

        CpuLogger.mark("EventScheduler") {
            EventScheduler.execute()
        }

        ActorSystem.send(SYSTEM, Root::class.simpleName!!, SystemCommand.OnTick(Game.time))
        ActorSystem.tick()
    }

    fun postLoop() {
        CpuLogger.marks().takeIf { it.isNotEmpty() }?.values?.forEach { mark ->
            log.info(mark.name().padEnd(40), "CPU:", mark.diff)
        }

        // try to generate pixel if bucket is filled
        if (Game.cpu.bucket >= 10000)
            Game.cpu.generatePixel()

        log.info("=============================== CPU: ${CpuLogger.total()} ===============================")
    }

    @Suppress("NOTHING_TO_INLINE", "unused")
    inline fun breakpoint() = js("debugger;")
}