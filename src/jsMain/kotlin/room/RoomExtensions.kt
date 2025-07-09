package room

import memory.resourceSemaphore
import screeps.api.FindConstant
import screeps.api.Identifiable
import screeps.api.Room
import screeps.api.options
import screeps.utils.lazyPerTick
import utils.isAvailable
import utils.log.ILogger
import utils.log.LogLevel
import utils.log.Logging
import utils.tryAcquire
import utils.tryRelease

val Room.log: ILogger by lazyPerTick { Logging("Room:$name", LogLevel.ERROR).log }

fun <T : Identifiable> Room.findFreeResourceOfType(findConstant: FindConstant<T>): Array<T> {
    return find(findConstant, options {
        filter = { foundObject ->
            memory.resourceSemaphore.isAvailable(foundObject.id) ?: true
        }
    })
}

fun Room.acquireResource(ownerId: String, resourceId: String) {
    when (memory.resourceSemaphore.tryAcquire(resourceId)) {
        true -> log.info("$resourceId acquired by $ownerId, OK")
        false -> log.error("$resourceId acquired by $ownerId, FAIL")
        null -> Unit
    }
}

fun Room.releaseResource(ownerId: String, resourceId: String) {
    when (memory.resourceSemaphore.tryRelease(resourceId)) {
        true -> log.info("$resourceId released by $ownerId, OK")
        false -> log.error("$resourceId released by $ownerId, FAIL")
        null -> Unit
    }
}