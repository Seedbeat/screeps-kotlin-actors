package utils.trace

internal data class ActiveCpuMark(
    val key: String,
    val nodeId: Int,
    var lastCpuSample: Double
)
