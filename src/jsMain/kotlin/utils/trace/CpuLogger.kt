package utils.trace

import screeps.api.Game
import utils.Number.toFixed
import kotlin.reflect.KFunction

object CpuLogger {
    @PublishedApi
    internal var isEnabled = true
        private set
    internal var session = 0
        private set
    private const val accuracy = 2
    internal const val NO_NODE = -1

    internal val tree = CpuTraceTree(accuracy)
    private val activeMarks = ArrayDeque<ActiveCpuMark>()
    private var printFilter = CpuTraceFilter.Empty

    internal fun setProfilingEnabled(enabled: Boolean) {
        if (isEnabled == enabled) return
        isEnabled = enabled
        session++
        tree.clear()
        activeMarks.clear()
    }

    fun init() {
        if (!isEnabled) return
        tree.clear()
        activeMarks.clear()
    }

    fun setPrintFilter(filter: CpuTraceFilter) {
        printFilter = filter
    }

    fun setPrintFilter(path: List<String>) {
        printFilter = CpuTraceFilter(path)
    }

    fun setPrintFilter(vararg path: String) {
        printFilter = CpuTraceFilter(*path)
    }

    fun clearPrintFilter() {
        printFilter = CpuTraceFilter.Empty
    }

    internal fun printFilterPath(): Array<String> = printFilter.path.toTypedArray()

    fun currentMarkId(): String? = if (isEnabled) activeMarks.lastOrNull()?.key else null

    inline fun <T> mark(id: KFunction<*>, parentId: KFunction<*>? = null, action: () -> T): T {
        if (!isEnabled) return action()
        return mark(id.name, parentId?.name ?: currentMarkId(), action)
    }

    inline fun <T> mark(id: String, action: () -> T): T {
        if (!isEnabled) return action()
        return mark(id, currentMarkId(), action)
    }

    inline fun <T> mark(id: String, parentId: String?, action: () -> T): T {
        if (!isEnabled) return action()
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

    fun markStart(id: String, parentId: String? = currentMarkId()) =
        markStart(id, id, parentId)

    fun markStart(key: String, label: String, parentKey: String?) {
        if (!isEnabled) return

        val now = Game.cpu.getUsed()
        flushActiveMark(now)

        val parentNodeId = parentKey?.let { findActiveMarkNodeId(it) }
        val nodeId = node(parentNodeId, label)
        activeMarks.addLast(ActiveCpuMark(key, nodeId, now))
    }

    fun markEnd(key: String) {
        if (!isEnabled) return
        if (activeMarks.isEmpty()) return

        val now = Game.cpu.getUsed()
        var index = -1
        for (i in activeMarks.indices.reversed()) {
            if (activeMarks[i].key == key) {
                index = i
                break
            }
        }
        if (index == -1) return

        val active = activeMarks.removeAt(index)
        addCpu(active.nodeId, now - active.lastCpuSample)

        if (index == activeMarks.size) {
            activeMarks.lastOrNull()?.lastCpuSample = now
        }
    }

    fun print() {
        print(printFilter)
    }

    fun print(filter: CpuTraceFilter) {
        if (!isEnabled) return
        tree.print(filter)
    }

    fun print(path: List<String>) {
        print(CpuTraceFilter(path))
    }

    fun total() = Game.cpu.getUsed().toFixed(accuracy)

    internal fun node(parentId: Int?, label: String): Int {
        if (!isEnabled) return NO_NODE
        return tree.node(parentId, label)
    }

    internal fun addCpu(nodeId: Int, diff: Double) {
        if (!isEnabled) return
        tree.addCpu(nodeId, diff)
    }

    private fun flushActiveMark(now: Double) {
        val active = activeMarks.lastOrNull() ?: return
        addCpu(active.nodeId, now - active.lastCpuSample)
        active.lastCpuSample = now
    }

    private fun findActiveMarkNodeId(key: String): Int? {
        for (i in activeMarks.indices.reversed()) {
            val active = activeMarks[i]
            if (active.key == key) {
                return active.nodeId
            }
        }
        return null
    }
}
