package creep

import actors.CreepAssignment
import creep.BodySpec.Companion.cost
import creep.BodySpec.Companion.label
import screeps.api.*

object BodyRecipe {
    fun selectBodySpecByAssignment(energyBudget: Int, assignment: CreepAssignment) = when (assignment) {
        is CreepAssignment.ControllerUpkeep,
        is CreepAssignment.Construction -> scaled(
            energyBudget = energyBudget,
            core = listOf(MOVE, WORK, CARRY),
            segment = listOf(MOVE, WORK, CARRY, MOVE),
            maxRepeats = 2
        )
    }

    fun scaled(
        energyBudget: Int,
        core: List<BodyPartConstant>,
        segment: List<BodyPartConstant> = listOf(),
        maxRepeats: Int? = null
    ): BodySpec? {
        val coreCost = core.cost
        if (coreCost > energyBudget) return null

        val label = core.label

        if (segment.isEmpty()) {
            return BodySpec(
                body = core.toTypedArray(),
                label = label
            )
        }

        val repeatLimitByEnergy = (energyBudget - coreCost) / segment.cost
        val repeatLimitBySize = (MAX_CREEP_SIZE - core.size) / segment.size
        val repeatLimitByCaller = maxRepeats?.coerceAtLeast(0) ?: Int.MAX_VALUE

        val repeats = minOf(
            repeatLimitByEnergy,
            repeatLimitBySize,
            repeatLimitByCaller
        )

        val body = buildList(core.size + repeats * segment.size) {
            addAll(core)
            repeat(repeats) { addAll(segment) }
        }

        return BodySpec(
            body = body.toTypedArray(),
            label = label
        )
    }
}
