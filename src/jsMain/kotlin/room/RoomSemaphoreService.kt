package room

import actor.ActorSystem
import actors.base.ActorApi
import actors.base.ActorBinding
import creep.CreepCommand
import memory.resourceLockOwners
import memory.resourceSemaphore
import room.enums.RoomResourceType
import screeps.api.*
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging
import utils.remove

class RoomSemaphoreService<T>(
    api: T,
) : ActorApi by api,
    ActorBinding<Room> by api,
    ILogging by Logging<RoomSemaphoreService<T>>(api.self.name, LogLevel.INFO)
        where T : ActorApi,
              T : ActorBinding<Room> {

    fun syncSemaphores() {
        val coordinator = RoomSemaphoreCoordinator(self)
        val plan = coordinator.createSyncPlan()
        val semaphore = self.memory.resourceSemaphore

        plan.toCreate.forEach { definition ->
            log.info("Creating semaphore for ${definition.id}")
            semaphore.create(definition.id, 0, definition.max)
        }

        if (plan.changedResourceIds.isEmpty()) {
            return
        }

        releaseAffectedResourceOwners(plan.changedResourceIds)

        plan.toDelete.forEach { resourceId ->
            log.info("Deleting semaphore for $resourceId")
            semaphore.remove(resourceId)
        }

        plan.toRecreate.forEach { definition ->
            val currentMax = semaphore.maximum(definition.id)
            log.info("Recreating semaphore for ${definition.id}: $currentMax -> ${definition.max}")
            semaphore.recreate(definition.id, 0, definition.max)
        }
    }

    fun tryAcquireResource(ownerId: String, resourceId: String): Boolean? {
        val owners = self.memory.resourceLockOwners
        val ownedResourceId = owners[ownerId]

        if (ownedResourceId == resourceId) {
            log.info("$resourceId acquire by $ownerId skipped: already owned")
            return true
        }

        if (ownedResourceId != null) {
            log.error("$resourceId acquired by $ownerId, FAIL: owner already holds $ownedResourceId")
            return false
        }

        val result = self.memory.resourceSemaphore.tryAcquire(resourceId)
        when (result) {
            true -> {
                owners[ownerId] = resourceId
                setLockedResource(ownerId, resourceId)
                log.info("$resourceId acquired by $ownerId, OK")
            }

            false -> log.error("$resourceId acquired by $ownerId, FAIL")
            null -> log.error("$resourceId is not found by $ownerId, FAIL")
        }
        return result
    }

    fun tryAcquireAnyResource(ownerId: String, near: RoomPosition, type: RoomResourceType): String? {
        val owners = self.memory.resourceLockOwners
        val ownedResourceId = owners[ownerId]
        if (ownedResourceId != null) {
            return ownedResourceId
        }

        return when (type) {
            RoomResourceType.SOURCE -> {
                val source = resolveClosestAvailableSource(near) ?: return null
                if (tryAcquireResource(ownerId, source.id) == true) {
                    source.id
                } else {
                    null
                }
            }
        }
    }

    fun releaseResource(ownerId: String, resourceId: String): Boolean? {
        val owners = self.memory.resourceLockOwners
        val ownedResourceId = owners[ownerId]

        if (ownedResourceId == null) {
            log.error("$resourceId released by $ownerId, FAIL: owner has no lock")
            return false
        }

        if (ownedResourceId != resourceId) {
            log.error("$resourceId released by $ownerId, FAIL: owner holds $ownedResourceId")
            return false
        }

        val result = self.memory.resourceSemaphore.tryRelease(resourceId)
        when (result) {
            true -> {
                owners.remove(ownerId)
                setLockedResource(ownerId, null)
                log.info("$resourceId released by $ownerId, OK")
            }

            false -> log.error("$resourceId released by $ownerId, FAIL")
            null -> log.error("$resourceId is not found by $ownerId, FAIL")
        }
        return result
    }

    fun reconcileMissingResourceOwners() {
        val owners = self.memory.resourceLockOwners
        val missingOwnerIds = owners.keys.filter { ownerId -> !ActorSystem.contains(ownerId) }

        if (missingOwnerIds.isEmpty()) {
            return
        }

        var released = 0
        missingOwnerIds.forEach { ownerId ->
            val resourceId = owners[ownerId] ?: return@forEach
            val releaseResult = self.memory.resourceSemaphore.tryRelease(resourceId)

            when (releaseResult) {
                true -> {
                    owners.remove(ownerId)
                    released++
                    log.info("Released orphaned lock $resourceId owned by missing creep $ownerId")
                }

                false -> {
                    owners.remove(ownerId)
                    log.error("Failed to release orphaned lock $resourceId owned by missing creep $ownerId: semaphore is already empty")
                }

                null -> {
                    owners.remove(ownerId)
                    log.error("Failed to release orphaned lock $resourceId owned by missing creep $ownerId: semaphore is missing")
                }
            }
        }

        if (released > 0) {
            log.info("Reconciled $released orphaned resource locks")
        }
    }

    private fun releaseAffectedResourceOwners(resourceIds: Set<String>) {
        if (resourceIds.isEmpty()) {
            return
        }

        val affectedOwners = collectAffectedOwnerIds(self, resourceIds)
        if (affectedOwners.isEmpty()) {
            return
        }

        var released = 0
        affectedOwners.forEach { ownerId ->
            val lockedId = self.memory.resourceLockOwners[ownerId] ?: return@forEach
            val unassigned = unassignOwner(ownerId)
            val lockStillHeld = self.memory.resourceLockOwners[ownerId] == lockedId

            if (lockStillHeld && releaseResource(ownerId, lockedId) == true) {
                released++
            } else if (unassigned && !lockStillHeld) {
                released++
            }
        }

        if (released > 0) {
            log.info("Released $released resource locks")
        }
    }

    private fun collectAffectedOwnerIds(room: Room, resourceIds: Set<String>): Set<String> {
        val owners = room.memory.resourceLockOwners
        return owners.keys
            .filter { ownerId -> owners[ownerId] in resourceIds }
            .toSet()
    }

    private fun unassignOwner(ownerId: String): Boolean =
        sendTo(actorId = ownerId, payload = CreepCommand.ClearAssignment)

    private fun setLockedResource(ownerId: String, lockedResourceId: String?): Boolean =
        sendTo(actorId = ownerId, payload = CreepCommand.SetLockedResourceId(lockedResourceId))

    private fun resolveClosestAvailableSource(near: RoomPosition): Source? = near.findClosestByPath(
        objects = self.find(findConstant = FIND_SOURCES),
        opts = options {
            filter = { source ->
                source.energy > 0 && (self.memory.resourceSemaphore.isAvailable(source.id) ?: true)
            }
        }
    )
}
