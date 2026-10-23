package utils.console

import utils.trace.CpuLogger
import kotlin.test.*

class SystemConsoleTest {
    @Test
    fun consoleExpressionsReadAndToggleProfiling() = withConsole {
        assertEquals(true, js("(0, eval)('system.profile.enabled')"))
        assertEquals(false, js("(0, eval)('system.profile.enabled = false')"))
        assertTrue(!CpuLogger.isEnabled)
        assertEquals(true, js("(0, eval)('system.profile.enabled = true')"))
        assertTrue(CpuLogger.isEnabled)
    }

    @Test
    fun nonBooleanAssignmentsAreRejectedWithoutChangingTheFlag() = withConsole {
        val system = js("globalThis.system")
        for (value in listOf<dynamic>("false", 0, null, js("({})"), js("[]"))) {
            val error = assertFailsWith<IllegalArgumentException> { system.profile.enabled = value }
            assertEquals("system.profile.enabled must be a Boolean", error.message)
            assertTrue(CpuLogger.isEnabled)
        }
    }

    @Test
    fun profileNamespaceCannotBeReplacedAndItsPropertiesRemainMutable() = withConsole {
        val system = js("globalThis.system")
        val profile = system.profile
        val descriptor = js("Object").getOwnPropertyDescriptor(system, "profile")
        assertEquals(false, descriptor.writable)
        assertEquals(false, descriptor.configurable)
        assertEquals(false, js("Reflect.set(globalThis.system, 'profile', false)"))
        assertSame(profile, system.profile)
        assertTrue(CpuLogger.isEnabled)
        system.profile.filter = arrayOf("room")
        system.profile.enabled = false
        assertSame(profile, system.profile)
        assertEquals(false, profile.enabled)
        assertContentEquals(arrayOf("room"), profile.filter.unsafeCast<Array<String>>())
        system.profile.filter = arrayOf("SYSTEM")
        assertTrue(!CpuLogger.isEnabled)
        system.profile.enabled = true
        assertSame(profile, system.profile)
        assertEquals(true, profile.enabled)
        assertContentEquals(arrayOf("SYSTEM"), CpuLogger.printFilterPath())
        profile.enabled = false
        assertTrue(!CpuLogger.isEnabled)
    }

    @Test
    fun repeatedValueDoesNotInvalidateSuspendedProfilingSessions() = withConsole {
        val session = CpuLogger.session
        js("globalThis.system.profile.enabled = true")
        assertEquals(session, CpuLogger.session)
        js("globalThis.system.profile.enabled = false")
        assertEquals(session + 1, CpuLogger.session)
        SystemConsole.install()
        assertEquals(false, js("globalThis.system.profile.enabled"))
    }

    @Test
    fun consoleExpressionsSetAndResetProfileFilterWithoutChangingTheSession() = withConsole {
        val session = CpuLogger.session
        val generation = CpuLogger.tree.generation
        js("(0, eval)(\"system.profile.filter = ['*', 'Lifecycle', 'Tick', 'processIntents']\")")
        assertContentEquals(arrayOf("*", "Lifecycle", "Tick", "processIntents"), CpuLogger.printFilterPath())
        assertEquals(session, CpuLogger.session)
        assertEquals(generation, CpuLogger.tree.generation)
        SystemConsole.install()
        assertEquals(4, js("globalThis.system.profile.filter.length"))
        js("globalThis.system.profile.filter = null")
        assertEquals(0, js("globalThis.system.profile.filter.length"))
        js("globalThis.system.profile.filter = ['room']")
        js("globalThis.system.profile.filter = []")
        assertEquals(0, js("globalThis.system.profile.filter.length"))
    }

    @Test
    fun profileFilterRejectsInvalidAssignmentsAtomically() = withConsole {
        val system = js("globalThis.system")
        system.profile.filter = arrayOf("room")
        for (value in listOf("room", false, 42, js("['room', 42]"), js("[null]"))) {
            assertFailsWith<IllegalArgumentException> { system.profile.filter = value }
            assertContentEquals(arrayOf("room"), CpuLogger.printFilterPath())
        }
    }

    @Test
    fun profileFilterCopiesAssignedAndReturnedArrays() = withConsole {
        val system = js("globalThis.system")
        val assigned = arrayOf("room")
        system.profile.filter = assigned
        assigned[0] = "changed"
        val returned = system.profile.filter.unsafeCast<Array<String>>()
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
