package utils

import screeps.api.MutableRecord
import screeps.api.get
import screeps.api.set
import screeps.utils.contains
import screeps.utils.mutableRecordOf

private const val CURRENT_INDEX = 0
private const val MAXIMUM_INDEX = 1

external interface Semaphore : MutableRecord<String, Array<Int>>

// Constructor
fun Semaphore() = mutableRecordOf<String, Array<Int>>().unsafeCast<Semaphore>()

fun Semaphore.create(id: String, initialCount: Int, maximumCount: Int) {
    if (this.contains(id))
        throw Exception("Semaphore already exist")

    createInternal(id, initialCount, maximumCount)
}

fun Semaphore.recreate(id: String, initialCount: Int, maximumCount: Int) = createInternal(id, initialCount, maximumCount)

// Variables
fun Semaphore.isAvailable(id: String) = this[id]?.run { current() < maximum() }
fun Semaphore.available(id: String) = this[id]?.run { maximum() - current() }

fun Semaphore.current(id: String) = this[id]?.current()
fun Semaphore.maximum(id: String) = this[id]?.maximum()


// Public methods
fun Semaphore.tryAcquire(id: String): Boolean? {
    val semaphore = this[id]
        ?: return null

    if (semaphore.current() == semaphore.maximum())
        return false

    semaphore.increment()
    return true
}

fun Semaphore.tryRelease(id: String): Boolean? {
    val semaphore = this[id]
        ?: return null

    if (semaphore.current() == 0)
        return false

    semaphore.decrement()
    return true
}

// Private methods
private fun Semaphore.createInternal(id: String, initialCount: Int, maximumCount: Int) {
    validate(id, initialCount, maximumCount)
    this[id] = arrayOf(initialCount, maximumCount)
}

private fun validate(id: String, initialCount: Int, maximumCount: Int) {
    if (id.isBlank())
        throw IllegalArgumentException("Invalid id: '$id'")

    if (initialCount < 0)
        throw IllegalArgumentException("InitialCount < 0: '$initialCount'")

    if (maximumCount < 1)
        throw IllegalArgumentException("MaximumCount < 1: '$maximumCount'")

    if (initialCount > maximumCount)
        throw IllegalArgumentException("Initial > Maximum: '$initialCount' > '$maximumCount'")
}

private fun Array<Int>.current() = this[CURRENT_INDEX]
private fun Array<Int>.maximum() = this[MAXIMUM_INDEX]

private fun Array<Int>.increment() = set(0, current().inc())
private fun Array<Int>.decrement() = set(0, current().dec())
