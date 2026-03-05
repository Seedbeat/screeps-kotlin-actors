package utils

import screeps.api.Game
import kotlin.reflect.KFunction

object CpuLogger {
    private val isEnabled = true

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
    private val activeMarks = ArrayDeque<String>()

    fun init() {
        marks.clear()
        activeMarks.clear()
    }

    private fun currentMarkId(): String? = activeMarks.lastOrNull()

    fun <T> mark(id: String, parentId: String? = currentMarkId(), action: () -> T): T {
        markStart(id, parentId)
        return try {
            action()
        } finally {
            markEnd(id)
        }
    }

    fun <T> mark(id: KFunction<*>, parentId: KFunction<*>? = null, action: () -> T): T =
        mark(id.name, parentId?.name ?: currentMarkId(), action)

    fun markStart(id: String, parentId: String? = currentMarkId()) {
        if (!isEnabled || (filter.isNotEmpty() && !filter.contains(id))) return

        marks[id] = Mark(id, parentId)
        activeMarks.addLast(id)
    }

    fun markStart(id: KFunction<*>, parentId: KFunction<*>? = null) {
        markStart(id.name, parentId?.name ?: currentMarkId())
    }

    fun markEnd(id: String) {
        if (!isEnabled) return

        marks[id]?.calcDiff()
        if (activeMarks.isNotEmpty()) {
            if (activeMarks.last() == id) {
                activeMarks.removeLast()
            } else {
                activeMarks.remove(id)
            }
        }
    }

    fun markEnd(id: KFunction<*>) {
        markEnd(id.name)
    }

    fun marks(): Map<String, Mark> = marks

    fun total() = Game.cpu.getUsed()
}
