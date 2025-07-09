package spawn

import creep.enums.CreepType
import creep.enums.Role
import memory.createCreepMemory
import screeps.api.*
import screeps.api.structures.SpawnOptions
import screeps.api.structures.StructureSpawn
import utils.log.ILogging
import utils.log.Logging

object Spawner : ILogging by Logging<Spawner>() {

    fun spawn(
        structureSpawn: StructureSpawn,
        role: Role,
        opt: SpawnOptions.() -> Unit = {},
        memory: CreepMemory.() -> Unit = {}
    ): ScreepsReturnCode {
        val available = role.availableTypes.takeIf { it.isNotEmpty() }
            ?: return ERR_NOT_FOUND

        val energy = structureSpawn.room.energyAvailable
        val type = available.firstOrNull { it.cost <= energy }
            ?: return ERR_NOT_ENOUGH_ENERGY

        return spawn(structureSpawn, type, role, opt, memory)
    }

    fun spawn(
        structureSpawn: StructureSpawn,
        type: CreepType,
        role: Role,
        opt: SpawnOptions.() -> Unit = {},
        memory: CreepMemory.() -> Unit = {}
    ): ScreepsReturnCode {

        val (code, name) = structureSpawn.spawnCreep(type, role, opt, memory)

        when (code) {
            OK -> log.info("spawning $name with body $type")

            ERR_BUSY,
            ERR_NOT_ENOUGH_ENERGY -> run { } // do nothing
            ERR_NAME_EXISTS -> log.error("Duplicated name to spawn creep:", name)

            else -> log.error("unhandled error code $code")
        }
        return code
    }

    private fun StructureSpawn.spawnCreep(
        type: CreepType,
        role: Role,
        opt: SpawnOptions.() -> Unit = {},
        memory: CreepMemory.() -> Unit = {}
    ): Pair<ScreepsReturnCode, String> {
        val name = generateCreepName(type, role)
        return spawnCreep(
            type.body,
            name,
            options<SpawnOptions> {
                this.memory = createCreepMemory(type, role, memory)
            }.also(opt)
        ) to name
    }

    private fun generateCreepName(type: CreepType, role: Role): String {
        val roleEmoji = when (role) {
            Role.UNASSIGNED -> "❓"
            Role.HARVESTER -> "🚜"
            Role.BUILDER -> "🔨"
            Role.UPGRADER -> "🛠️"
            Role.REPAIRER -> "🔧"
            Role.MINER -> "⛏️"
            Role.COURIER -> "🚛"
            Role.CARRIER -> "🚚"
            Role.KNIGHT -> "🛡️"
            Role.RANGER -> "🏹"
            Role.HEALER -> "🩺"
            Role.SCAVENGER -> "🧹"
            Role.GRAVEDIGGER -> "⚰️"
        }

        val typeEmoji = when {
            type.cost >= 1000 -> "💎"
            type.cost >= 700 -> "🟣"
            type.cost >= 550 -> "🔴"
            type.cost >= 350 -> "🟠"
            type.cost >= 250 -> "🟡"
            type.cost > 0 -> "🟢"
            else -> "❓"
        }

        return "$roleEmoji-$typeEmoji-${Game.time.toString(32)}"
    }
}