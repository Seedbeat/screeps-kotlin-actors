package utils

import Root
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

private class LazyDelegate<in P, T>(
    private val tickNth: Int?,
    private val compute: P.() -> T
) : ReadOnlyProperty<P, T> {

    init {
        require(tickNth == null || tickNth in 1..Root.LOCAL_TIME_MAX) {
            "tickNth must be null or between 1 and ${Root.LOCAL_TIME_MAX}"
        }
    }

    @Suppress("UNCHECKED_CAST")
    private val value: T
        get() = backedValue as T

    private var initialized = false
    private var lastTick = -1
    private var backedValue: T? = null

    override fun getValue(thisRef: P, property: KProperty<*>): T = when {
        !initialized -> {
            initialized = true
            computeValue(thisRef)
        }

        tickNth == null -> value

        lastTick != Root.localTime -> when {
            tickNth == 1 -> computeValue(thisRef)
            Root.localTime % tickNth == 0 -> computeValue(thisRef)
            else -> value
        }

        else -> value
    }

    private fun computeValue(thisRef: P): T {
        lastTick = Root.localTime
        backedValue = compute(thisRef)
        return value
    }
}

fun <P, T> lazyOnce(compute: P.() -> T): ReadOnlyProperty<P, T> =
    LazyDelegate(tickNth = null, compute = compute)

fun <P, T> lazyPerTick(compute: P.() -> T): ReadOnlyProperty<P, T> =
    LazyDelegate(tickNth = 1, compute = compute)

fun <P, T> lazyPerNthTick(n: Int?, compute: P.() -> T): ReadOnlyProperty<P, T> =
    LazyDelegate(tickNth = n, compute = compute)