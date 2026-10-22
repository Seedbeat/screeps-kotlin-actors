package actors

import actor.message.Command
import actor.message.Request
import actor.message.Response
import actors.base.ActorBinding
import actors.base.Lifecycle
import actors.base.NoBinding
import room.RoomCommand
import utils.log.ILogging
import utils.log.Logging
import utils.trace.CpuLogger
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ActorProfilingTest {
    private class ProfilingActor :
        ActorBase<Unit, Command, Request<*>, Response<*>>("room"),
        ActorBinding<Unit> by NoBinding,
        ILogging by Logging("ProfilingTest") {

        override suspend fun processCommand(msg: Command) = Unit
        override suspend fun processRequest(msg: Request<*>): Response<*> = error("Not used in profiling tests")

        suspend fun <T> markByClass(block: suspend () -> T): T = cpuMark(RoomCommand.Scan::class, block)
        suspend fun <T> markByType(block: suspend () -> T): T = cpuMark<RoomCommand.Scan, T>(block)
        suspend fun markNamed(block: suspend () -> Unit) = cpuMark("Prepare", block)
    }

    private class Fixture {
        val actor = ProfilingActor()
        var now = 0.0

        fun cpu(vararg path: String): Double {
            var parentId: Int? = null
            for (label in path) {
                parentId = CpuLogger.tree.node(parentId, label)
            }
            return CpuLogger.tree.selfCpu(requireNotNull(parentId))
        }
    }

    @Test
    fun directlyDeliveredCommandReusesItsMessageScope() = withFixture { f ->
        f.actor.profiler.beginMessage(RoomCommand.Scan)
        f.actor.profiler.onSliceStart(0.0)
        runImmediately {
            f.actor.markByClass { f.now = 3.0 }
        }
        f.actor.profiler.onSliceEnd(3.0)

        assertEquals(3.0, f.cpu("room", "Command", "Scan"))
        assertEquals(0.0, f.cpu("room", "Command", "Scan", "Scan"))
    }

    @Test
    fun commandCalledFromLifecycleGetsOneTypedScopeAndReturnsItsResult() = withFixture { f ->
        f.actor.profiler.beginMessage(Lifecycle.Tick(1))
        f.actor.profiler.onSliceStart(0.0)
        runImmediately {
            val result = f.actor.markByType {
                f.now = 2.0
                42
            }
            assertEquals(42, result)
        }
        f.actor.profiler.onSliceEnd(2.0)

        assertEquals(0.0, f.cpu("room", "Lifecycle", "Tick"))
        assertEquals(2.0, f.cpu("room", "Lifecycle", "Tick", "Scan"))
        assertEquals(0.0, f.cpu("room", "Lifecycle", "Tick", "Scan", "Run"))
    }

    @Test
    fun sameCommandNestedInAnotherOperationKeepsItsScopeAndUnwindsOnFailure() = withFixture { f ->
        f.actor.profiler.beginMessage(RoomCommand.Scan)
        f.actor.profiler.onSliceStart(0.0)
        assertFailsWith<IllegalStateException> {
            runImmediately {
                f.actor.markNamed {
                    f.actor.markByClass {
                        f.now = 2.0
                        error("Handler failed")
                    }
                }
            }
        }
        f.actor.profiler.onSliceEnd(3.0)

        assertEquals(1.0, f.cpu("room", "Command", "Scan"))
        assertEquals(2.0, f.cpu("room", "Command", "Scan", "Prepare", "Scan"))
    }

    private fun withFixture(block: (Fixture) -> Unit) {
        val globals = js("globalThis")
        val previousGame = globals.Game
        val f = Fixture()
        val game = js("({cpu: {}})")
        game.cpu.getUsed = { f.now }
        globals.Game = game
        CpuLogger.init()
        try {
            block(f)
        } finally {
            globals.Game = previousGame
            CpuLogger.init()
        }
    }

    private fun runImmediately(block: suspend () -> Unit) {
        var completion: Result<Unit>? = null
        block.startCoroutine(object : Continuation<Unit> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) {
                completion = result
            }
        })
        assertTrue(completion != null, "This test operation must complete without suspension")
        completion.getOrThrow()
    }
}
