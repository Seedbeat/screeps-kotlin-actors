package actors.heuristics

import screeps.api.Room

object EnergyHeuristics {
    fun energyRatio(room: Room): Double = if (room.energyCapacityAvailable <= 0) {
        0.0
    } else {
        room.energyAvailable.toDouble() / room.energyCapacityAvailable.toDouble()
    }

    fun energyRatioModifier(energyRatio: Double): Double = when {
        energyRatio >= 0.9 -> 0.75
        energyRatio >= 0.6 -> 1.0
        energyRatio >= 0.3 -> 1.2
        else -> 1.5
    }
}