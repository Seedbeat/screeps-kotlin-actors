package scheduler

import Root
import scheduler.events.*
import scheduler.missions.*
import screeps.api.Game
import screeps.api.values
import utils.CpuLogger
import utils.log.ILogging
import utils.log.Logging

object EventScheduler : ILogging by Logging<EventScheduler>() {

    private val events: Array<Pair<Int, Array<IEvent>>> = arrayOf(
        1 to arrayOf(GlobalUpdate, Cleanup, TowerExecutor, CreepExecutor, Sandbox),
        5 to arrayOf(UpdateSettings),
        10 to arrayOf(Statistics)
    )

    private val roomMissions: Array<Pair<Int, Array<IRoomMission<*>>>> = arrayOf(
        19 to arrayOf(RoomStageAnalyzer),
        1 to arrayOf(RoomKnightSpawn, RoomSafe),
        3 to arrayOf(RoomHarvesterSpawn, RoomCarrierSpawn, RoomCourierSpawn, RoomMinerSpawn, RoomUpgraderSpawn),
        6 to arrayOf(RoomHealerSpawn, RoomRepairerSpawn, RoomBuilderSpawn, RoomScavengerSpawn, RoomGravediggerSpawn)
    )

    fun execute() {
        events.execute {
            CpuLogger.mark(this::class.simpleName!!, EventScheduler::class.simpleName) {
                execute()
            }
        }

        Game.rooms.values.forEach { room ->
            roomMissions.execute {
                CpuLogger.mark("[${room.name}] " + this::class.simpleName!!, EventScheduler::class.simpleName) {
                    Root.room(room.name).execute()
                }
            }
        }
    }

    private fun <T : IEventBase> Array<Pair<Int, Array<T>>>.execute(action: T.() -> Unit) = forEach { (tick, events) ->
        if (Root.localTime % tick != 0) {
            events.forEach { event ->
                if (event.isEnabled)
                    log.info("Skip ${event::class.simpleName}")
            }
            return@forEach
        }

        events.forEach { event ->
            if (event.isEnabled) try {
                log.info("Call ${event::class.simpleName}")
                action(event)
            } catch (exception: Exception) {
                log.error("Fail ${event::class.simpleName}", exception)
            }
        }
    }

}