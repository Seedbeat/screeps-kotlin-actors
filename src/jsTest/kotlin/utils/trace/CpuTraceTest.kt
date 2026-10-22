package utils.trace

import kotlin.test.Test
import kotlin.test.assertEquals

class CpuTraceTest {
    private class Fixture {
        val tree = CpuTraceTree(accuracy = 2)
        var now = 0.0
        var clockReads = 0
        val trace = CpuTrace(tree) {
            clockReads++
            now
        }

        fun cpu(vararg path: String): Double {
            var parentId: Int? = null
            for (label in path) {
                parentId = tree.node(parentId, label)
            }
            return tree.selfCpu(requireNotNull(parentId))
        }

        fun begin() = trace.begin("room", "Lifecycle", "Tick")
    }

    @Test
    fun nestedMarksAttributeOnlyExclusiveCpuToEachNode() {
        val f = Fixture()
        f.begin()
        f.trace.onSliceStart(0.0)
        f.now = 1.0
        f.trace.push("Planning")
        f.now = 3.0
        f.trace.push("Query")
        f.now = 4.0
        f.trace.pop()
        f.now = 6.0
        f.trace.pop()
        f.trace.onSliceEnd(7.0)

        assertEquals(2.0, f.cpu("room", "Lifecycle", "Tick"))
        assertEquals(4.0, f.cpu("room", "Lifecycle", "Tick", "Planning"))
        assertEquals(1.0, f.cpu("room", "Lifecycle", "Tick", "Planning", "Query"))
    }

    @Test
    fun repeatedSlicesWithinOneTickExcludeWaitingTime() {
        val f = Fixture()
        f.begin()
        f.trace.onSliceStart(0.0)
        f.now = 1.0
        f.trace.push("Planning")
        f.trace.onSliceEnd(2.0)

        f.trace.onSliceStart(20.0)
        f.now = 21.0
        f.trace.pop()
        f.trace.onSliceEnd(22.0)

        assertEquals(2.0, f.cpu("room", "Lifecycle", "Tick"))
        assertEquals(2.0, f.cpu("room", "Lifecycle", "Tick", "Planning"))
    }

    @Test
    fun suspendedNestedMarksRebindWhenNextTickCreatesNodesInDifferentOrder() {
        val f = Fixture()
        f.begin()
        f.trace.onSliceStart(0.0)
        f.now = 1.0
        f.trace.push("Planning")
        f.now = 2.0
        f.trace.push("Query")
        f.trace.onSliceEnd(3.0)

        f.tree.clear()
        val other = f.tree.node(null, "another actor")
        f.tree.addCpu(other, 9.0)
        f.trace.onSliceStart(0.25)
        f.now = 1.25
        f.trace.pop()
        f.now = 2.25
        f.trace.pop()
        f.trace.onSliceEnd(3.25)

        assertEquals(9.0, f.tree.selfCpu(other))
        assertEquals(1.0, f.cpu("room", "Lifecycle", "Tick"))
        assertEquals(1.0, f.cpu("room", "Lifecycle", "Tick", "Planning"))
        assertEquals(1.0, f.cpu("room", "Lifecycle", "Tick", "Planning", "Query"))
    }

    @Test
    fun continuationCanResumeAfterSeveralEmptyTicks() {
        val f = Fixture()
        f.begin()
        f.trace.onSliceStart(0.0)
        f.now = 1.0
        f.trace.push("Waiting")
        f.trace.onSliceEnd(2.0)

        repeat(3) { f.tree.clear() }
        f.trace.onSliceStart(1.0)
        f.trace.onSliceEnd(3.0)

        assertEquals(0.0, f.cpu("room", "Lifecycle", "Tick"))
        assertEquals(2.0, f.cpu("room", "Lifecycle", "Tick", "Waiting"))
    }

    @Test
    fun startingAnotherMessageDiscardsPreviousPathAndCpuSample() {
        val f = Fixture()
        f.begin()
        f.trace.onSliceStart(0.0)
        f.now = 1.0
        f.trace.push("Planning")
        f.trace.onSliceEnd(2.0)

        f.tree.clear()
        f.trace.begin("creep", "Command", "Assign")
        f.trace.onSliceStart(10.0)
        f.trace.onSliceEnd(11.0)

        assertEquals(1.0, f.cpu("creep", "Command", "Assign"))
        assertEquals(0.0, f.cpu("room", "Lifecycle", "Tick", "Planning"))
    }

    @Test
    fun inactiveStackChangesDoNotReadTheCpuClock() {
        val f = Fixture()
        f.begin()
        f.trace.push("Removed")
        f.trace.pop()
        f.trace.push("Deferred")
        f.trace.onSliceEnd(100.0)
        assertEquals(0, f.clockReads)

        f.trace.onSliceStart(1.0)
        f.trace.onSliceEnd(2.0)
        assertEquals(1.0, f.cpu("room", "Lifecycle", "Tick", "Deferred"))
        assertEquals(0, f.clockReads)
    }

    @Test
    fun deepStackSurvivesBufferGrowthAndTreeReset() {
        val f = Fixture()
        f.begin()
        f.trace.onSliceStart(0.0)
        val marks = (1..20).map { "Level$it" }
        marks.forEach { f.trace.push(it) }
        f.trace.onSliceEnd(1.0)

        f.tree.clear()
        f.trace.onSliceStart(10.0)
        f.now = 11.0
        repeat(marks.size) { f.trace.pop() }
        f.trace.onSliceEnd(12.0)

        val deepestPath = listOf("room", "Lifecycle", "Tick") + marks
        assertEquals(1.0, f.cpu(*deepestPath.toTypedArray()))
        assertEquals(1.0, f.cpu("room", "Lifecycle", "Tick"))
    }
}
