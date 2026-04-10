import actors.base.Actors
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
        Actors.tick()
    }

    fun postLoop() {
//        CpuLogger.print()

        // try to generate pixel if bucket is filled
        if (Game.cpu.bucket >= 10000)
            Game.cpu.generatePixel()

        log.info("=============================== CPU: ${CpuLogger.total()} ===============================")
    }

    @Suppress("NOTHING_TO_INLINE", "unused")
    inline fun breakpoint() = js("debugger;")
}