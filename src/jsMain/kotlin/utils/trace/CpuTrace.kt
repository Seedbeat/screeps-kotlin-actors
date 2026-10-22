package utils.trace

import screeps.api.Game

class CpuTrace internal constructor(
    private val tree: CpuTraceTree,
    private val cpuUsed: () -> Double
) {
    constructor() : this(CpuLogger.tree, { Game.cpu.getUsed() })

    // Labels belong to the suspended operation; node ids belong to one report generation.
    private var labels = arrayOfNulls<String>(8)
    private var nodeIds = IntArray(8)
    private var stackSize = 0
    private var baseStackSize = 0
    private var resolvedGeneration = -1
    private var lastCpuSample: Double? = null

    fun begin(rootLabel: String, categoryLabel: String, typeLabel: String) {
        labels.fill(null, toIndex = stackSize)
        stackSize = 0
        lastCpuSample = null
        resolvedGeneration = -1

        pushLabel(rootLabel)
        pushLabel(categoryLabel)
        pushLabel(typeLabel)
        baseStackSize = stackSize
    }

    fun push(label: String) {
        flushSegment()
        pushLabel(label)
    }

    internal fun isMessageScope(label: String): Boolean =
        stackSize > 0 && stackSize == baseStackSize && labels[stackSize - 1] == label

    fun pop() {
        if (stackSize > baseStackSize) {
            flushSegment()
            stackSize--
            labels[stackSize] = null
        }
    }

    fun onSliceStart(now: Double) {
        resolveNodes()
        lastCpuSample = now
    }

    fun onSliceEnd(now: Double) {
        flushSegment(now)
        lastCpuSample = null
    }

    private fun flushSegment(now: Double? = null) {
        val start = lastCpuSample ?: return
        val end = now ?: cpuUsed()
        tree.addCpu(currentNodeId(), end - start)
        lastCpuSample = end
    }

    private fun pushLabel(label: String) {
        if (stackSize == labels.size) {
            labels = labels.copyOf(labels.size * 2)
            nodeIds = nodeIds.copyOf(nodeIds.size * 2)
        }

        labels[stackSize] = label
        if (lastCpuSample != null) {
            val parentId = if (stackSize == 0) null else nodeIds[stackSize - 1]
            nodeIds[stackSize] = tree.node(parentId, label)
        } else {
            // The path can also be changed before its first execution slice.
            resolvedGeneration = -1
        }
        stackSize++
    }

    private fun resolveNodes() {
        if (resolvedGeneration == tree.generation) return

        var parentId: Int? = null
        for (index in 0 until stackSize) {
            val nodeId = tree.node(parentId, labels[index]!!)
            nodeIds[index] = nodeId
            parentId = nodeId
        }
        resolvedGeneration = tree.generation
    }

    private fun currentNodeId(): Int =
        if (stackSize == 0) CpuLogger.NO_NODE else nodeIds[stackSize - 1]
}
