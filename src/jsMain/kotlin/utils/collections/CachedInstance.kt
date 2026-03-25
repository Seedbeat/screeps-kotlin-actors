package utils.collections

import utils.lazyPerTick

class CachedInstance<V> {
    private val entries by lazyPerTick { mutableMapOf<Any, V>() }

    fun getOrPut(key: Any, create: () -> V): V =
        entries.getOrPut(SingleKey(key), create)

    fun getOrPut(firstKey: Any, secondKey: Any, create: () -> V): V =
        entries.getOrPut(DoubleKey(firstKey, secondKey), create)

    fun getOrPut(vararg keys: Any, create: () -> V): V =
        entries.getOrPut(MultiKey(keys.toList()), create)

    private data class SingleKey(val value: Any)
    private data class DoubleKey(val first: Any, val second: Any)
    private data class MultiKey(val values: List<Any>)
}