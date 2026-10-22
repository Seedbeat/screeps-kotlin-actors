package utils.trace

data class CpuTraceFilter(
    val path: List<String> = emptyList()
) {
    companion object {
        const val WILDCARD = "*"
        val Empty = CpuTraceFilter()
    }

    val isEmpty: Boolean
        get() = path.isEmpty()

    constructor(vararg path: String) : this(path.toList())

    // Anchored path filter. "*" matches exactly one label at its position.
    fun matchesPathPrefix(labels: List<String>): Boolean {
        if (isEmpty) return true
        if (labels.size > path.size) return true

        labels.forEachIndexed { index, label ->
            val expected = path[index]
            if (expected != WILDCARD && expected != label) {
                return false
            }
        }

        return true
    }

    fun matchesFullPath(labels: List<String>): Boolean {
        if (isEmpty) return true
        if (labels.size < path.size) return false

        path.forEachIndexed { index, expected ->
            if (expected != WILDCARD && expected != labels[index]) {
                return false
            }
        }

        return true
    }
}
