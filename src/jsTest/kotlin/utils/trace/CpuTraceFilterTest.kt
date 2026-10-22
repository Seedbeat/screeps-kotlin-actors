package utils.trace

import kotlin.test.Test
import kotlin.test.assertEquals

class CpuTraceFilterTest {
    @Test
    fun matchingBranchIncludesAncestorsAndDescendantsButNotSiblings() {
        val tree = sampleTree()
        val lines = printedLines(tree, CpuTraceFilter("room", "Tick", "Planning"))
        assertEquals(listOf("room", "Tick", "Planning", "Work"), lines.map { it.first })
        assertEquals("total=15.00", lines.first().second)
        assertEquals("total=7.00", lines[2].second)
    }

    @Test
    fun wildcardMatchesExactlyOneLevelAndUnmatchedPathPrintsNothing() {
        val tree = sampleTree()
        val lines = printedLines(tree, CpuTraceFilter("*", "Tick", "Planning"))
        assertEquals(listOf("room", "Tick", "Planning", "Work"), lines.map { it.first })
        assertEquals(emptyList(), printedLines(tree, CpuTraceFilter("*", "Planning")))
    }

    @Test
    fun emptyFilterRestoresAllBranches() {
        assertEquals(
            listOf("room", "Tick", "Planning", "Work", "Scan", "SYSTEM"),
            printedLines(sampleTree(), CpuTraceFilter.Empty).map { it.first })
    }

    private fun sampleTree(): CpuTraceTree {
        val tree = CpuTraceTree(accuracy = 2)
        val room = tree.node(null, "room")
        val tick = tree.node(room, "Tick")
        val planning = tree.node(tick, "Planning")
        val work = tree.node(planning, "Work")
        val scan = tree.node(tick, "Scan")
        val system = tree.node(null, "SYSTEM")
        for ((index, node) in listOf(room, tick, planning, work, scan, system).withIndex()) {
            tree.addCpu(node, (index + 1).toDouble())
        }
        return tree
    }

    private fun printedLines(tree: CpuTraceTree, filter: CpuTraceFilter): List<Pair<String, String>> {
        val console = js("console")
        val previousLog = console.log
        val lines = mutableListOf<Pair<String, String>>()
        console.log = { _: dynamic, label: dynamic, _: dynamic, total: dynamic, _: dynamic ->
            lines += label.toString().trim().substringAfter("┗━ ") to total.toString()
        }
        try {
            tree.print(filter)
        } finally {
            console.log = previousLog
        }
        return lines
    }
}
