package utils

import screeps.api.Record

inline fun <K, V> Record<K, V>.remove(ownerId: String) {
    asDynamic()[ownerId] = undefined
}