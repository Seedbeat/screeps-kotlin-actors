package utils.console

import utils.trace.CpuLogger
import kotlin.test.*

class SystemConsoleTest {
    @Test
    fun consoleExpressionsReadAndToggleProfiling() = withConsole {
        assertEquals(true, js("(0, eval)('system.profiling')"))
        assertEquals(false, js("(0, eval)('system.profiling = false')"))
        assertTrue(!CpuLogger.isEnabled)
        assertEquals(true, js("(0, eval)('system.profiling = true')"))
        assertTrue(CpuLogger.isEnabled)
    }

    @Test
    fun nonBooleanAssignmentsAreRejectedWithoutChangingTheFlag() = withConsole {
        val system = js("globalThis.system")
        for (value in listOf<dynamic>("false", 0, null)) {
            assertFailsWith<IllegalArgumentException> { system.profiling = value }
            assertTrue(CpuLogger.isEnabled)
        }
    }

    @Test
    fun repeatedValueDoesNotInvalidateSuspendedProfilingSessions() = withConsole {
        val session = CpuLogger.session
        js("globalThis.system.profiling = true")
        assertEquals(session, CpuLogger.session)
        js("globalThis.system.profiling = false")
        assertEquals(session + 1, CpuLogger.session)
        SystemConsole.install()
        assertEquals(false, js("globalThis.system.profiling"))
    }

    @Test
    fun consoleExpressionsSetAndResetProfileFilterWithoutChangingTheSession() = withConsole {
        val session = CpuLogger.session
        val generation = CpuLogger.tree.generation
        js("(0, eval)(\"system.profileFilter = ['*', 'Lifecycle', 'Tick', 'processIntents']\")")
        assertContentEquals(arrayOf("*", "Lifecycle", "Tick", "processIntents"), CpuLogger.printFilterPath())
        assertEquals(session, CpuLogger.session)
        assertEquals(generation, CpuLogger.tree.generation)
        SystemConsole.install()
        assertEquals(4, js("globalThis.system.profileFilter.length"))
        js("globalThis.system.profileFilter = null")
        assertEquals(0, js("globalThis.system.profileFilter.length"))
        js("globalThis.system.profileFilter = ['room']")
        js("globalThis.system.profileFilter = []")
        assertEquals(0, js("globalThis.system.profileFilter.length"))
    }

    @Test
    fun profileFilterRejectsInvalidAssignmentsAtomically() = withConsole {
        val system = js("globalThis.system")
        system.profileFilter = arrayOf("room")
        for (value in listOf("room", false, 42, js("['room', 42]"), js("[null]"))) {
            assertFailsWith<IllegalArgumentException> { system.profileFilter = value }
            assertContentEquals(arrayOf("room"), CpuLogger.printFilterPath())
        }
    }

    @Test
    fun profileFilterCopiesAssignedAndReturnedArrays() = withConsole {
        val system = js("globalThis.system")
        val assigned = arrayOf("room")
        system.profileFilter = assigned
        assigned[0] = "changed"
        val returned = system.profileFilter.unsafeCast<Array<String>>()
        returned[0] = "also changed"
        assertContentEquals(arrayOf("room"), CpuLogger.printFilterPath())
    }

    private fun withConsole(block: () -> Unit) {
        val globals = js("globalThis")
        val objectApi = js("Object")
        val previousDescriptor = objectApi.getOwnPropertyDescriptor(globals, "system")
        val previousEnabled = CpuLogger.isEnabled
        val previousFilter = CpuLogger.printFilterPath()
        CpuLogger.setProfilingEnabled(true)
        CpuLogger.clearPrintFilter()
        SystemConsole.install()
        try {
            block()
        } finally {
            CpuLogger.setProfilingEnabled(previousEnabled)
            CpuLogger.setPrintFilter(*previousFilter)
            if (previousDescriptor == null) {
                js("Reflect").deleteProperty(globals, "system")
            } else {
                objectApi.defineProperty(globals, "system", previousDescriptor)
            }
        }
    }
}
