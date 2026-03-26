import actors.base.Actors
import room.RoomContext
import screeps.api.Game
import utils.CpuLogger
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

object Root : ILogging by Logging<Root>(LogLevel.DEBUG) {

    const val LOCAL_TIME_MAX = 100

    var wasReset = true

    inline val localTime: Int
        get() = Game.time % LOCAL_TIME_MAX

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
        Actors.init()
    }

    fun preLoop() {
        log.info("=============================== Tick: ${Game.time} ===============================")
        CpuLogger.init()
    }

    fun loop() {
//        CpuLogger.mark("ContextCreation") {
//            for ((name, room) in Game.rooms) {
//                if (!roomContext.containsKey(name))
//                    roomContext[name] = RoomContext(room)
//            }
//        }
//
//        CpuLogger.mark("EventScheduler") {
//            EventScheduler.execute()
//        }

        Actors.tick()
    }

    fun postLoop() {
        CpuLogger.print()

        // try to generate pixel if bucket is filled
        if (Game.cpu.bucket >= 10000)
            Game.cpu.generatePixel()

        log.info("=============================== CPU: ${CpuLogger.total()} ===============================")
    }

    @Suppress("NOTHING_TO_INLINE", "unused")
    inline fun breakpoint() = js("debugger;")
}