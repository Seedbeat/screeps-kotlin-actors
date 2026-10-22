package utils.console

import utils.trace.CpuLogger

object SystemConsole {
    fun install() = ConsoleNamespace.install("system") {
        property(
            name = "profiling",
            type = ConsoleType.boolean,
            get = { CpuLogger.isEnabled },
            set = CpuLogger::setProfilingEnabled,
        )
        property(
            name = "profileFilter",
            type = ConsoleType.nullableStringArray,
            get = CpuLogger::printFilterPath,
            set = { path ->
                if (path == null) CpuLogger.clearPrintFilter()
                else CpuLogger.setPrintFilter(path.toList())
            },
        )
    }
}
