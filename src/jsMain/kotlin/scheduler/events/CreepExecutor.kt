package scheduler.events

import creep.enums.Role
import creep.enums.State
import creep.roles.*
import creep.wait
import memory.role
import memory.state
import scheduler.IEvent
import screeps.api.Game
import screeps.api.values
import utils.CpuLogger
import utils.log.ILogging
import utils.log.Logging

object CreepExecutor : IEvent, ILogging by Logging<CreepExecutor>() {
    override fun execute() {
        Game.creeps.values.forEach { creep ->
            CpuLogger.markStart(creep.name, CreepExecutor::class.simpleName)

            if (!creep.my)
                return@forEach

            if (creep.memory.state == State.WAIT && Root.localTime % 3 != 0) {
                log.info("skip creep processing with name: ${creep.name}")
                return@forEach
            }

            log.info("process creep with name: ${creep.name}")
            when(creep.memory.state) {
                State.RECYCLE -> {
                    creep.recycle()
                    return@forEach
                }
                State.RENEW -> {
                    creep.renew()
                    return@forEach
                }
            }

            when (creep.memory.role) {
                Role.UPGRADER -> creep.upgrade()
                Role.HARVESTER -> creep.harvest()
                Role.MINER -> creep.mine()
                Role.COURIER -> creep.carry()
                Role.CARRIER -> creep.delivery()
                Role.REPAIRER -> creep.repair()
                Role.BUILDER -> creep.build()
                Role.KNIGHT -> creep.attack()
                Role.HEALER -> creep.heal()
                Role.SCAVENGER -> creep.sweep()
                Role.GRAVEDIGGER -> creep.dig()

                else -> creep.wait()
            }

            CpuLogger.markEnd(creep.name)
        }
    }

}