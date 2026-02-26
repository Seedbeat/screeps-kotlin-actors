package utils

import screeps.api.Game

object CpuLogger {
    private val isEnabled = false

    private val filter: HashSet<String> = hashSetOf(
//        CreepExecutor::class.simpleName
    )

    data class Mark(
        val id: String,
        val parentId: String? = null
    ) {
        fun calcDiff() {
            diff = Game.cpu.getUsed() - initial
        }

        val initial: Double = Game.cpu.getUsed()
        var diff: Double = 0.0

        fun name(): String {
            var parentCount = 0
            var currentParentId = this.parentId

            while (currentParentId != null) {
                parentCount++
                currentParentId = marks[currentParentId]?.parentId
            }

            return when (parentCount) {
                0 -> this.id
                else -> "".padStart(parentCount * 3 - 2, ' ') + "┗━ " + this.id
            }
        }
    }

    private val marks = mutableMapOf<String, Mark>()

    fun init() {
        marks.clear()
    }

    fun <T> mark(id: String, parentId: String? = null, action: () -> T): T {
        markStart(id, parentId)
        val res = action()
        markEnd(id)
        return res
    }

    fun markStart(id: String, parentId: String? = null) {
        if (!isEnabled || (filter.isNotEmpty() && !filter.contains(id))) return

        marks[id] = Mark(id, parentId)
    }

    fun markEnd(id: String) {
        if (!isEnabled) return

        marks[id]?.calcDiff()
    }

    fun mark(id: String, parentId: String? = null, action: () -> Unit) {
        markStart(id, parentId)
        action()
        markEnd(id)
    }

    fun marks(): Map<String, Mark> = marks

    fun total() = Game.cpu.getUsed()
}