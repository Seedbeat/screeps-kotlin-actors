package map

import screeps.api.Attackable
import screeps.api.STRUCTURE_RAMPART
import screeps.api.STRUCTURE_WALL
import screeps.api.structures.Structure
import kotlin.math.min
import kotlin.math.roundToInt

private const val WALL_HEALTH_MAX = 1_000_000
private const val RAMPART_HEALTH_MAX = 1_000_000

val Structure.health: Int
    get() = when (structureType) {
        STRUCTURE_WALL -> calcHealth(hits, min(hitsMax, WALL_HEALTH_MAX))
        STRUCTURE_RAMPART -> calcHealth(hits, min(hitsMax, RAMPART_HEALTH_MAX))
        else -> calcHealth(hits, hitsMax)
    }

val Attackable.health: Int get() = calcHealth(hits, hitsMax)

private fun calcHealth(hits: Int, hitsMax: Int): Int = ((hits.toFloat() / hitsMax.toFloat()) * 100f).roundToInt()