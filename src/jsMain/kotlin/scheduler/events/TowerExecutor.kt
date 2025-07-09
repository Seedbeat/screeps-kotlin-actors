package scheduler.events

import Root
import map.health
import room.RoomContext
import scheduler.IEvent
import screeps.api.RESOURCE_ENERGY
import screeps.api.structures.StructureTower
import utils.CpuLogger
import utils.log.ILogging
import utils.log.Logging

object TowerExecutor : IEvent, ILogging by Logging<TowerExecutor>() {
    override fun execute() {
        Root.rooms().values.forEach { it.process() }
    }

    private fun RoomContext.process() = towers.forEach { tower ->
        CpuLogger.mark(tower.id, TowerExecutor::class.simpleName) {
            process(tower)
        }
    }

    private fun RoomContext.process(tower: StructureTower) {
        tower.findClosestEnemy(byRange = true)?.let {
            tower.attack(it)
            return
        }

        damagedTowers
            .filter { it.id != tower.id && it.health < 90 }
            .take(3).toTypedArray()
            .let { tower.findClosestBase(it, byRange = true) }
            ?.let {
                tower.repair(it)
                return
            }

        if ((tower.store.getUsedCapacity(RESOURCE_ENERGY) ?: 0) <= 500)
            return

        tower.findClosestDamagedCreep(byRange = true)?.let {
            tower.heal(it)
            return
        }

        tower.findClosestDamagedStructure(byRange = true)?.let {
            tower.repair(it)
            return
        }
    }

}