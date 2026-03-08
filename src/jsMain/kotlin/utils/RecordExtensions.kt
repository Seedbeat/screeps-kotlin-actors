package utils

import screeps.api.Record

@Suppress("NOTHING_TO_INLINE")
inline fun <K, V> Record<K, V>.remove(ownerId: String) {
    asDynamic()[ownerId] = undefined
}