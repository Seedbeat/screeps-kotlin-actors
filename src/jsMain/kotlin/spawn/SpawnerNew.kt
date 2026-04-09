package spawn

import actors.CreepAssignment
import actors.WorkerSpawnProfile
import creep.BodyRecipe
import creep.BodySpec
import memory.assignment
import memory.createCreepMemory
import memory.homeRoom
import screeps.api.*
import screeps.api.structures.SpawnOptions
import screeps.api.structures.StructureSpawn
import utils.log.ILogging
import utils.log.Logging

object SpawnerNew : ILogging by Logging<SpawnerNew>() {

    fun StructureSpawn.spawn(
        assignment: CreepAssignment,
        profile: WorkerSpawnProfile = WorkerSpawnProfile.Standard,
        opt: SpawnOptions.() -> Unit = {}
    ): ScreepsReturnCode {
        val energyBudget = room.energyAvailable
        val body = BodyRecipe.selectBodySpecByAssignment(
            energyBudget = energyBudget,
            assignment = assignment,
            profile = profile
        )
            ?: return ERR_NOT_ENOUGH_ENERGY

        return spawn(
            body = body,
            opt = opt
        ) {
            this.assignment = assignment
        }
    }

    fun StructureSpawn.spawn(
        body: BodySpec,
        opt: SpawnOptions.() -> Unit = {},
        memory: CreepMemory.() -> Unit = {}
    ): ScreepsReturnCode {
        val name = generateCreepName(body)
        val home = room.name

        val options = options<SpawnOptions> {
            this.memory = createCreepMemory {
                homeRoom = home
            }.also(memory)
        }.also(opt)

        val code = spawnCreep(
            body = body.body,
            name = name,
            opts = options
        )

        when (code) {
            OK -> log.info("spawning $name with body ${body.label} cost=${body.cost}")
            ERR_BUSY, ERR_NOT_ENOUGH_ENERGY -> Unit
            ERR_NAME_EXISTS -> log.error("Duplicated name to spawn creep: $name")
            else -> log.error("unhandled error code $code")
        }

        return code
    }

    private fun generateCreepName(body: BodySpec): String =
        "${body.label}_${Game.time.toString(32)}"
}
