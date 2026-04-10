package creep.body

import screeps.api.*

data class CreepCapabilities(
    val work: Int,
    val carry: Int,
    val move: Int
) {
    val canDoControllerUpkeep: Boolean
        get() = work > 0 && carry > 0 && move > 0

    val canDoConstruction: Boolean
        get() = work > 0 && carry > 0 && move > 0

    val canDoEnergyTransfer: Boolean
        get() = work > 0 && carry > 0 && move > 0

    companion object {
        fun from(creep: Creep): CreepCapabilities {
            val bodyParts = creep.body

            val workParts = bodyParts.count { part -> part.type == WORK }
            val carryParts = bodyParts.count { part -> part.type == CARRY }
            val moveParts = bodyParts.count { part -> part.type == MOVE }

            return CreepCapabilities(
                work = workParts,
                carry = carryParts * CARRY_CAPACITY,
                move = moveParts
            )
        }
    }
}
