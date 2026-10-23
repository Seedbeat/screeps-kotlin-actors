package utils.console

import utils.trace.CpuLogger

object SystemConsole {
    fun install() = ConsoleNamespace.install("system") {
        namespace("profile") {
            action("on") {
                CpuLogger.setProfilingEnabled(true)
                CpuLogger.isEnabled
            }
            action("off") {
                CpuLogger.setProfilingEnabled(false)
                CpuLogger.isEnabled
            }
            property(
                name = "enabled",
                type = ConsoleType.boolean,
                get = { CpuLogger.isEnabled },
                set = CpuLogger::setProfilingEnabled,
            )
            property(
                name = "filter",
                type = ConsoleType.nullableStringArray,
                get = CpuLogger::printFilterPath,
                set = { path ->
                    if (path == null) CpuLogger.clearPrintFilter()
                    else CpuLogger.setPrintFilter(path.toList())
                },
            )
        }
    }
}
