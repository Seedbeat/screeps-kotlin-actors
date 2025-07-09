package utils.collections

import Root

open class LinkedHashMapWithDefault<K, V>(val default: (K) -> V) : LinkedHashMap<K, V>() {
    override operator fun get(key: K): V {
        val value = super.get(key)
        return if (value == null) {
            Root.log.info("[ADD NEW]", key)
            val answer = default(key)
            put(key, answer)
            answer
        } else {
            Root.log.info("[CACHE]", key)
            value
        }
    }

    operator fun set(key: K, value: V) {
        Root.log.info("[PUT]", key, value)
        super.put(key, value)
    }
}