package utils.console

import utils.trace.CpuLogger

object SystemConsole {
    fun install() = ConsoleNamespace.install("system") {
        namespace("profile") {
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
