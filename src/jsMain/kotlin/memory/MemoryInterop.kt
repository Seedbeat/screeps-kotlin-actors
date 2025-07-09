@file:Suppress("UnusedReceiverParameter")

package memory

import screeps.api.CreepMemory
import screeps.api.Memory
import screeps.api.MutableRecord
import screeps.api.global
import utils.JsInterop
import utils.log.ILogging
import utils.log.Logging
import kotlin.reflect.KCallable


fun Memory.delete(item: KCallable<*>) =
    MemoryInterop.deleteByPath(item)

fun MutableRecord<String, CreepMemory>.delete(name: String) =
    MemoryInterop.deleteByPath(Memory::creeps, key = name)

private object MemoryInterop : ILogging by Logging<MemoryInterop>() {

    fun deleteByPath(vararg path: KCallable<*>, key: String) = delete(objectPath(*path, key = key))

    fun deleteByPath(vararg path: KCallable<*>) = delete(objectPath(*path))

    fun objectPath(vararg path: KCallable<*>, key: String) = objectPath(*path).plus("[\"$key\"]")

    fun objectPath(vararg path: KCallable<*>) = getObjectInternal(global::Memory, *path)


    private fun delete(objectPath: String): Unit = JsInterop.execute("delete $objectPath")

    private fun getObjectInternal(vararg path: KCallable<*>) = path.joinToString(".") { it.name }
}

