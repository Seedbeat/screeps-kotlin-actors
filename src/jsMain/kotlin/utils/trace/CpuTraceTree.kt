package utils.trace

import Root
import utils.Number.toFixed

internal class CpuTraceTree(
    private val accuracy: Int
) {
    private data class NodeKey(
        val parentId: Int?,
        val label: String
    )

    private data class Node(
        val label: String,
        val parentId: Int?,
        val depth: Int,
        val children: MutableList<Int> = mutableListOf(),
        var selfCpu: Double = 0.0
    )

    private val nodes = mutableListOf<Node>()
    private val nodeIds = mutableMapOf<NodeKey, Int>()
    var generation: Int = 0
        private set

    fun clear() {
        nodes.clear()
        nodeIds.clear()
        generation++
    }

    fun node(parentId: Int?, label: String): Int {
        if (parentId == CpuLogger.NO_NODE) return CpuLogger.NO_NODE

        val key = NodeKey(parentId, label)
        val existing = nodeIds[key]
        if (existing != null) return existing

        val nodeId = nodes.size
        val node = Node(
            label = label,
            parentId = parentId,
            depth = parentDepth(parentId) + 1
        )
        nodes += node
        nodeIds[key] = nodeId
        if (parentId != null) {
            nodes[parentId].children += nodeId
        }
        return nodeId
    }

    fun addCpu(nodeId: Int, diff: Double) {
        if (nodeId == CpuLogger.NO_NODE) return
        if (diff <= 0.0) return

        nodes[nodeId].selfCpu += diff
    }

    fun selfCpu(nodeId: Int): Double = nodes[nodeId].selfCpu

    fun print(filter: CpuTraceFilter = CpuTraceFilter.Empty) {
        val totals = DoubleArray(nodes.size)
        nodes.indices.forEach { nodeId ->
            if (nodes[nodeId].parentId == null) {
                calculateTotal(nodeId, totals)
            }
        }

        val visible = when {
            filter.isEmpty -> null
            else -> calculateVisibleNodes(filter)
        }

        nodes.indices.forEach { nodeId ->
            if (nodes[nodeId].parentId == null) {
                printNode(nodeId, totals, visible)
            }
        }
    }

    private fun calculateTotal(nodeId: Int, totals: DoubleArray): Double {
        val node = nodes[nodeId]
        var total = selfCpu(nodeId)
        node.children.forEach { childId ->
            total += calculateTotal(childId, totals)
        }
        totals[nodeId] = total
        return total
    }

    private fun printNode(nodeId: Int, totals: DoubleArray, visible: BooleanArray?) {
        if (visible != null && !visible[nodeId]) return

        val node = nodes[nodeId]
        Root.log.info(
            formatName(node.label, node.depth).padEnd(40),
            "CPU:",
            "total=${totals[nodeId].toFixed(accuracy)}",
            "self=${node.selfCpu.toFixed(accuracy)}"
        )
        node.children.forEach { childId -> printNode(childId, totals, visible) }
    }

    private fun parentDepth(parentId: Int?): Int =
        parentId?.let { nodes[it].depth } ?: -1

    private fun calculateVisibleNodes(filter: CpuTraceFilter): BooleanArray {
        val visible = BooleanArray(nodes.size)
        val path = mutableListOf<String>()

        nodes.indices.forEach { nodeId ->
            if (nodes[nodeId].parentId == null) {
                markVisibleSubtree(
                    nodeId = nodeId,
                    filter = filter,
                    path = path,
                    matchedAncestor = false,
                    visible = visible
                )
            }
        }

        return visible
    }

    private fun markVisibleSubtree(
        nodeId: Int,
        filter: CpuTraceFilter,
        path: MutableList<String>,
        matchedAncestor: Boolean,
        visible: BooleanArray
    ): Boolean {
        val node = nodes[nodeId]
        path.add(node.label)

        val matchesPrefix = filter.matchesPathPrefix(path)
        val matchesFullPath = filter.matchesFullPath(path)
        val shouldPrintDescendants = matchedAncestor || matchesFullPath
        var shouldPrintNode = shouldPrintDescendants

        if (matchesPrefix) {
            node.children.forEach { childId ->
                if (markVisibleSubtree(childId, filter, path, shouldPrintDescendants, visible)) {
                    shouldPrintNode = true
                }
            }
        }

        visible[nodeId] = shouldPrintNode
        path.removeAt(path.lastIndex)

        return shouldPrintNode
    }

    private fun formatName(label: String, depth: Int): String = when (depth) {
        0 -> label
        else -> "".padStart(depth * 3 - 2, ' ') + "┗━ " + label
    }
}
