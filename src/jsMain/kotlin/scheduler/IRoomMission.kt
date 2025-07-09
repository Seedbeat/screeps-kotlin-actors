package scheduler

import creep.enums.Role
import memory.role
import room.RoomContext
import screeps.api.CreepMemory
import screeps.api.structures.SpawnOptions
import spawn.Spawner
import utils.log.ILogging

interface IRoomMission<T> : IMission<RoomContext, T>, ILogging {

    fun RoomContext.spawnMissionBase(
        role: Role,
        targetCountCalc: RoomContext.() -> Int
    ) = spawnMissionBase(role, targetCountCalc, {}, {})

    fun RoomContext.spawnMissionBase(
        role: Role,
        targetCountCalc: RoomContext.() -> Int,
        opt: SpawnOptions.() -> Unit = {},
        memory: CreepMemory.() -> Unit = {}
    ) {
        val creepsCount = creeps[role]
        val targetCount = targetCountCalc()
        val isCreepNeeded = creepsCount < targetCount

        if (isCreepNeeded) {
            log.info("Insufficient number of ${role}s detected ($creepsCount < $targetCount) at room ${room.name}, trying to spawn one")
            Spawner.spawn(spawns.first(), role, opt, memory)
        }
    }
}