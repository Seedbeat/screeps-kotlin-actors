package spawn

import creep.enums.CreepType
import creep.enums.Role
import memory.createCreepMemory
import screeps.api.CreepMemory
import screeps.api.Game
import screeps.api.ScreepsReturnCode
import screeps.api.options
import screeps.api.structures.SpawnOptions
import screeps.api.structures.StructureSpawn

fun StructureSpawn.spawnCreep(
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

private fun generateCreepName(type: CreepType, role: Role) =
    "${role}_${type.name}_${Game.time}"