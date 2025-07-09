package utils

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

private class TickLazyAdvanced<in P : Any?, T : Any>(
    val tickNth: Int,
    val compute: P.() -> T
) : ReadOnlyProperty<P, T> {

    var value: T? = null

    override fun getValue(thisRef: P, property: KProperty<*>): T {
        // init
        if (value == null) {
            value = compute(thisRef)
        }

        // update
        if (tickNth != -1 && Root.localTime % tickNth == 0) {
            value = compute(thisRef)
        }

        // cached
        return value!!
    }
}

fun <P : Any?, T : Any> lazyPerNthTick(n: Int, compute: P.() -> T): ReadOnlyProperty<P, T> {
    return TickLazyAdvanced(n, compute)
}