package actors

import actor.ActorKernel
import actor.ActorSystem
import actor.message.Command
import actor.message.Request
import actor.message.Response
import actors.base.ActorBinding
import actors.base.Lifecycle
import actors.base.NoBinding
import room.RoomCommand
import utils.log.ILogging
import utils.log.LogLevel
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
        ILogging by Logging("ProfilingTest", LogLevel.ERROR) {

        var handledCommands = 0

        override suspend fun processCommand(msg: Command) {
            cpuMark("Waiting") {
                yieldNow()
                handledCommands++
            }
        }
        override suspend fun processRequest(msg: Request<*>): Response<*> = error("Not used in profiling tests")

        suspend fun <T> markByClass(block: suspend () -> T): T = cpuMark(RoomCommand.Scan::class, block)
        suspend fun <T> markByPayload(block: suspend () -> T): T = cpuMark(RoomCommand.Scan, block)
        suspend fun <T> markByType(block: suspend () -> T): T = cpuMark<RoomCommand.Scan, T>(block)
        suspend fun markNamed(block: suspend () -> Unit) = cpuMark("Prepare", block)
    }

    private class Fixture {
        val actor = ProfilingActor()
        var now = 0.0
        var clockReads = 0

        fun cpu(vararg path: String): Double {
            var parentId: Int? = null
            for (label in path) {
                parentId = CpuLogger.tree.node(parentId, label)
            }
            return CpuLogger.tree.selfCpu(requireNotNull(parentId))
        }

        fun assertCpu(expectedWhenEnabled: Double, vararg path: String) {
            val expected = if (CpuLogger.isEnabled) expectedWhenEnabled else 0.0
            assertEquals(expected, cpu(*path))
        }
    }

    @Test
    fun directlyDeliveredCommandReusesItsMessageScope() = withFixture { f ->
        f.actor.profiler.beginMessage(RoomCommand.Scan)
        f.actor.profiler.onSliceStart(0.0)
        runImmediately {
            f.actor.markByPayload { f.now = 3.0 }
        }
        f.actor.profiler.onSliceEnd(3.0)

        f.assertCpu(3.0, "room", "Command", "Scan")
        f.assertCpu(0.0, "room", "Command", "Scan", "Scan")
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

        f.assertCpu(0.0, "room", "Lifecycle", "Tick")
        f.assertCpu(2.0, "room", "Lifecycle", "Tick", "Scan")
        f.assertCpu(0.0, "room", "Lifecycle", "Tick", "Scan", "Run")
        if (CpuLogger.isEnabled) {
            assertTrue(f.clockReads > 0)
        } else {
            assertEquals(0, f.clockReads, "Disabled actor scopes must not sample the CPU clock")
        }
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

        f.assertCpu(1.0, "room", "Command", "Scan")
        f.assertCpu(2.0, "room", "Command", "Scan", "Prepare", "Scan")
    }

    @Test
    fun mailboxAndScheduledContinuationRunWithoutProfilingClockReadsWhenDisabled() {
        for (enabled in listOf(false, true)) {
            withFixture(enabled) { f ->
                ActorSystem.spawn(f.actor.id) { f.actor }
                try {
                    ActorSystem.send(f.actor.id, "test", RoomCommand.Scan)
                    assertTrue(ActorKernel.deliverOneReadyActorMessage())
                    assertEquals(0, f.actor.handledCommands)
                    assertTrue(ActorKernel.flushScheduledContinuations())
                    assertEquals(1, f.actor.handledCommands)
                    if (enabled) {
                        assertTrue(f.clockReads > 0)
                    } else {
                        assertEquals(0, f.clockReads, "Disabled kernel profiling must not sample the CPU clock")
                    }
                } finally {
                    ActorSystem.remove(f.actor.id)
                }
            }
        }
    }

    @Test
    fun contextCreatedWhileDisabledCanProfileNewMessagesAfterEnabling() = withFixture(false) { f ->
        CpuLogger.setProfilingEnabled(true)
        f.actor.profiler.beginMessage(Lifecycle.Tick(1))
        f.actor.profiler.onSliceStart(0.0)
        runImmediately { f.actor.markByType { f.now = 2.0 } }
        f.actor.profiler.onSliceEnd(2.0)

        f.assertCpu(2.0, "room", "Lifecycle", "Tick", "Scan")
    }

    @Test
    fun switchingProfilingSessionsDiscardsSuspendedScopesAndPreservesExecution() = withFixture { f ->
        ActorSystem.spawn(f.actor.id) { f.actor }
        try {
            ActorSystem.send(f.actor.id, "test", RoomCommand.Scan)
            assertTrue(ActorKernel.deliverOneReadyActorMessage())
            val clockReads = f.clockReads

            CpuLogger.setProfilingEnabled(false)
            CpuLogger.setProfilingEnabled(true)
            f.now = 5.0
            assertTrue(ActorKernel.flushScheduledContinuations())
            assertEquals(1, f.actor.handledCommands)
            assertEquals(clockReads, f.clockReads, "Old sessions must not record continuation CPU")
            assertTrue(!f.actor.profiler.isActive)

            ActorSystem.send(f.actor.id, "test", SystemCommand.Noop)
            assertTrue(ActorKernel.deliverOneReadyActorMessage())
            f.now = 6.0
            assertTrue(ActorKernel.flushScheduledContinuations())
            assertEquals(2, f.actor.handledCommands)
            assertTrue(f.clockReads > clockReads)
            assertTrue(f.actor.profiler.isMessageScope("Noop"), "The new message must close its nested scope")
        } finally {
            ActorSystem.remove(f.actor.id)
        }
    }

    private fun withFixture(enabled: Boolean = true, block: (Fixture) -> Unit) {
        val globals = js("globalThis")
        val previousGame = globals.Game
        val previousEnabled = CpuLogger.isEnabled
        CpuLogger.setProfilingEnabled(enabled)
        val f = Fixture()
        val game = js("({time: 1, cpu: {}})")
        game.cpu.getUsed = {
            f.clockReads++
            f.now
        }
        globals.Game = game
        CpuLogger.init()
        try {
            block(f)
        } finally {
            globals.Game = previousGame
            CpuLogger.setProfilingEnabled(previousEnabled)
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
