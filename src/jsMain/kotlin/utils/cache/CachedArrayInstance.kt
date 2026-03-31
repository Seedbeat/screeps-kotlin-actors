package utils.cache

class CachedArrayInstance<V>(lifetime: Int = 1) {
    private val entries = CachedInstance<Array<V>>(lifetime)

    fun getOrPut(key: Any, create: () -> Array<V>): Array<V> =
        entries.getOrPut(key, create)

    fun getOrPut(firstKey: Any, secondKey: Any, create: () -> Array<V>): Array<V> =
        entries.getOrPut(firstKey, secondKey, create)

    fun getOrPut(vararg keys: Any, create: () -> Array<V>): Array<V> =
        entries.getOrPut(*keys, create = create)

    fun <T : V> getOrPutTyped(firstKey: Any, secondKey: Any, create: () -> Array<T>): Array<T> =
        entries.getOrPut(firstKey, secondKey) { create().unsafeCast<Array<V>>() }.unsafeCast<Array<T>>()

    fun <T : V> getOrPutTypeIn(firstKey: Any, secondKey: Any, create: () -> Array<V>): Array<T> =
        entries.getOrPut(firstKey, secondKey) { create().unsafeCast<Array<V>>() }.unsafeCast<Array<T>>()
}