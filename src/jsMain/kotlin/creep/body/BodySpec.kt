package creep.body

import screeps.api.*

data class BodySpec(
    @Suppress("ArrayInDataClass")
    val body: Array<BodyPartConstant>,
    val label: String
) {
    val cost: Int
        get() = body.sumOf { BODYPART_COST[it]!! }

    companion object {
        private val bodyPartAbbreviations = mapOf(
            MOVE to "🦿",
            WORK to "🛠️",
            CARRY to "🎒",
            ATTACK to "⚔️",
            RANGED_ATTACK to "🏹",
            TOUGH to "🛡️",
            HEAL to "💊",
            CLAIM to "🚩",
        )

        val List<BodyPartConstant>.cost: Int
            get() = this.sumOf { BODYPART_COST[it]!! }

        val List<BodyPartConstant>.label: String
            get() = fold(StringBuilder()) { acc, value ->
                acc.append(value.abbreviation)
            }.toString()

        val BodyPartConstant.abbreviation: String
            get() = bodyPartAbbreviations[this] ?: "❓"
    }
}
