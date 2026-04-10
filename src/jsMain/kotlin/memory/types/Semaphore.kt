package memory.types

import memory.base.MemoryMap
import memory.codecs.RawCodec
import screeps.api.MemoryMarker

class Semaphore(
    parent: MemoryMarker,
    key: String
) : MemoryMap<SemaphoreValue>(parent, key, RawCodec()) {

    fun create(id: String, initialCount: Int, maximumCount: Int) {
        if (id in this) {
            throw Exception("Semaphore already exist")
        }

        writeInternal(id, initialCount, maximumCount)
    }

    fun recreate(id: String, initialCount: Int, maximumCount: Int) {
        writeInternal(id, initialCount, maximumCount)
    }

    fun isAvailable(id: String): Boolean? {
        val available = available(id) ?: return null
        return available > 0
    }

    fun available(id: String): Int? {
        val entry = this[id] ?: return null
        return entry.maximum - entry.current
    }

    fun current(id: String): Int? = this[id]?.current

    fun maximum(id: String): Int? = this[id]?.maximum

    fun tryAcquire(id: String): Boolean? {
        val entry = this[id] ?: return null

        if (entry.current == entry.maximum) {
            return false
        }

        entry.current += 1
        return true
    }

    fun tryRelease(id: String): Boolean? {
        val entry = this[id] ?: return null

        if (entry.current == 0) {
            return false
        }

        entry.current -= 1
        return true
    }

    private fun writeInternal(id: String, initialCount: Int, maximumCount: Int) {
        validate(id, initialCount, maximumCount)

        get(id)?.let { entry ->
            entry.current = initialCount
            entry.maximum = maximumCount
        } ?: set(
            id,
            SemaphoreValue(
                current = initialCount,
                maximum = maximumCount
            )
        )
    }

    private fun validate(id: String, initialCount: Int, maximumCount: Int) {
        if (id.isBlank()) {
            throw IllegalArgumentException("Invalid id: '$id'")
        }

        if (initialCount < 0) {
            throw IllegalArgumentException("InitialCount < 0: '$initialCount'")
        }

        if (maximumCount < 1) {
            throw IllegalArgumentException("MaximumCount < 1: '$maximumCount'")
        }

        if (initialCount > maximumCount) {
            throw IllegalArgumentException("Initial > Maximum: '$initialCount' > '$maximumCount'")
        }
    }
}
