package utils

import Root
import screeps.api.Game
import utils.Number.toFixed
import kotlin.reflect.KFunction

object CpuLogger {
    private const val isEnabled = true
    private const val accuracy = 2

    private val filter: HashSet<String> = hashSetOf(
//        CreepExecutor::class.simpleName
    )

    data class Mark(
        val id: String,
        val parentId: String? = null,
        val depth: Int = 0
    ) {
        fun calcDiff(now: Double = Game.cpu.getUsed()) {
            diff = now - initial
        }

        val initial: Double = Game.cpu.getUsed()
        var diff: Double = 0.0
        private val displayName: String = when (depth) {
            0 -> id
            else -> "".padStart(depth * 3 - 2, ' ') + "┗━ " + id
        }

        fun name(): String = displayName
    }

    private val marks = mutableMapOf<String, Mark>()
    private val activeMarks = ArrayDeque<String>()

    fun init() {
        marks.clear()
        activeMarks.clear()
    }

    fun currentMarkId(): String? = activeMarks.lastOrNull()


    inline fun <T> mark(id: KFunction<*>, parentId: KFunction<*>? = null, action: () -> T): T =
        mark(id.name, parentId?.name ?: currentMarkId(), action)

    inline fun <T> mark(id: String, parentId: String? = currentMarkId(), action: () -> T): T {
        markStart(id, parentId)
        return try {
            action()
        } finally {
            markEnd(id)
        }
    }

    fun markStart(id: KFunction<*>, parentId: KFunction<*>? = null) =
        markStart(id.name, parentId?.name ?: currentMarkId())

    fun markEnd(id: KFunction<*>) =
        markEnd(id.name)

    fun markStart(id: String, parentId: String? = currentMarkId()) {
        if (!isEnabled) return
        if (filter.isNotEmpty() && !filter.contains(id)) return

        val depth = when (parentId) {
            null -> 0
            else -> (marks[parentId]?.depth ?: 0) + 1
        }

        marks[id] = Mark(id, parentId, depth)
        activeMarks.addLast(id)
    }

    fun markEnd(id: String) {
        if (!isEnabled) return

        val now = Game.cpu.getUsed()
        marks[id]?.calcDiff(now)
        if (activeMarks.isNotEmpty()) {
            if (activeMarks.last() == id) {
                activeMarks.removeLast()
            } else {
                activeMarks.remove(id)
            }
        }
    }


    fun marks(): Map<String, Mark> = marks

    fun print() {
        if (!isEnabled) return

        marks.values.forEach { mark ->
            Root.log.info(mark.name().padEnd(40), "CPU:", mark.diff.toFixed(accuracy))
        }
    }

    fun total() = Game.cpu.getUsed().toFixed(accuracy)
}
