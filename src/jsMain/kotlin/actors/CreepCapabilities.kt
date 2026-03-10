package actors

import screeps.api.*

data class CreepCapabilities(
    val harvestPower: Int,
    val carryCapacity: Int,
    val movePower: Int,
    val upgradePower: Int
) {
    val canDoControllerUpkeep: Boolean
        get() = harvestPower > 0 && carryCapacity > 0 && movePower > 0 && upgradePower > 0

    companion object {
        fun from(creep: Creep): CreepCapabilities {
            val bodyParts = creep.body
            val workParts = bodyParts.count { part -> part.type == WORK }
            val carryParts = bodyParts.count { part -> part.type == CARRY }
            val moveParts = bodyParts.count { part -> part.type == MOVE }

            return CreepCapabilities(
                harvestPower = workParts,
                carryCapacity = carryParts * CARRY_CAPACITY,
                movePower = moveParts,
                upgradePower = workParts
            )
        }
    }
}
