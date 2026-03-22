package actors.base

inline fun <T> Lifecycle.Tick.onEach(
    interval: Int,
    otherwise: T,
    block: () -> T
): T = if (time % interval == 0) block() else otherwise

inline fun Lifecycle.Tick.onEach(
    interval: Int,
    block: () -> Unit
) {
    if (time % interval == 0) {
        block()
    }
}