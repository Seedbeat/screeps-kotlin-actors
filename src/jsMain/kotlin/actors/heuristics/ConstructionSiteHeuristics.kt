package actors.heuristics

import screeps.api.*
import kotlin.math.roundToInt

object ConstructionSiteHeuristics {
    fun siteBuildersCapacity(site: ConstructionSite): Int = when (site.structureType) {
        STRUCTURE_SPAWN,
        STRUCTURE_STORAGE,
        STRUCTURE_TERMINAL -> 2

        STRUCTURE_RAMPART -> 2
        STRUCTURE_ROAD -> 1
        else -> 1
    }

    fun priorityWeight(site: ConstructionSite): Int = when (sitePriority(site)) {
        0 -> 250
        1 -> 175
        2 -> 125
        3 -> 100
        4 -> 50
        else -> 100
    }

    fun sitePriority(site: ConstructionSite): Int = when (site.structureType) {
        STRUCTURE_SPAWN -> 0
        STRUCTURE_EXTENSION,
        STRUCTURE_TOWER -> 1

        STRUCTURE_STORAGE,
        STRUCTURE_TERMINAL,
        STRUCTURE_CONTAINER -> 2

        STRUCTURE_RAMPART -> 3
        STRUCTURE_ROAD -> 4
        else -> 5
    }

    fun priorityModifier(sitePriority: Int): Double = when (sitePriority) {
        0 -> 0.5
        1 -> 0.7
        2 -> 0.9
        3 -> 1.1
        4 -> 1.4
        else -> 1.0
    }

    fun weightedRemainingWork(constructionSites: Array<ConstructionSite>): Int = constructionSites.sumOf { site ->
        val remainingWork = (site.progressTotal - site.progress).coerceAtLeast(0)
        (remainingWork * priorityWeight(site).toDouble() / 100.0).roundToInt()
    }
}