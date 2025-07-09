package room

import memory.resourceSemaphore
import screeps.api.Room
import screeps.utils.lazyPerTick
import utils.log.ILogger
import utils.log.LogLevel
import utils.log.Logging
import utils.tryAcquire
import utils.tryRelease

val Room.log: ILogger by lazyPerTick { Logging("Room:$name", LogLevel.ERROR).log }

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