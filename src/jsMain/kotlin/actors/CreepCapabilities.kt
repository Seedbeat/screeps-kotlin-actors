package actors

import screeps.api.*
import utils.lazyOnce

data class CreepCapabilities(
    val work: Int,
    val carry: Int,
    val move: Int
) {
    val canDoControllerUpkeep: Boolean
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

        val Creep.capabilities: CreepCapabilities by lazyOnce(::from)
    }
}
