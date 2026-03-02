package utils

import screeps.api.Game
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

private class TickLazyNullable<in P, T>(
    val computeOncePerTick: P.() -> T
) : ReadOnlyProperty<P, T> {
    private var value: Any? = UNSET
    private var tick: Int = -1

    override fun getValue(thisRef: P, property: KProperty<*>): T {
        if (Game.time != tick) {
            tick = Game.time
            value = computeOncePerTick(thisRef)
        }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    private object UNSET
}

fun <P, T> lazyPerTickNullable(computeOncePerTick: P.() -> T): ReadOnlyProperty<P, T> {
    return TickLazyNullable(computeOncePerTick)
}