package actors

import actor.ActorSystem
import actors.CreepCommand.SetLockedResourceId
import actors.CreepRequest.Unassign
import actors.RoomRequest.ReleaseResource
import actors.RoomRequest.StatusRequest
import actors.RoomResponse.*
import actors.SystemRequest.CountCreeps
import actors.base.*
import creep.enums.Role
import memory.resourceLockOwners
import memory.resourceSemaphore
import memory.stage
import screeps.api.*
import utils.*
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

class RoomActor(
    id: String
) : ActorIntentQueueBase<Room, RoomCommand, RoomIntent, RoomRequest, RoomResponse<*>>(id),
    IActorBinding<Room> by GameRoomBinding(id),
    IChildrenMultiManager,
    ILogging by Logging<RoomActor>(id, LogLevel.INFO) {

    companion object {
        private const val STAGE_SYNC_INTERVAL = 19
        private const val SEMAPHORE_SYNC_INTERVAL = 25
        private const val TARGET_HARVESTERS = 2
    }

    override val managers = mapOf(
        SpawnActor::class.simpleName!! to RoomSpawnsManager(self)
    )

    override fun onDestroy() {
        destroyChildren()
    }

    override suspend fun processLifecycle(msg: Lifecycle) = when (msg) {
        is Lifecycle.Bootstrap -> {
            log.info("Bootstrapping room $id")
            syncChildren()
            reconcileMissingResourceOwners()
            broadcast(this, msg)
            processCommand(RoomCommand.Scan)
            syncStage()
            processCommand(RoomCommand.SyncSemaphores)
        }
        is Lifecycle.Tick -> {
            log.info("Room $id tick")
            syncChildren()
            reconcileMissingResourceOwners()
            broadcast(this, msg)

            val stageChanged = if (msg.time % STAGE_SYNC_INTERVAL == 0) {
                syncStage()
            } else {
                false
            }

            if (stageChanged || msg.time % SEMAPHORE_SYNC_INTERVAL == 0) {
                processCommand(RoomCommand.SyncSemaphores)
            }
            processIntents(msg.time)
        }
    }

    override suspend fun processCommand(msg: RoomCommand) = when (msg) {
        is RoomCommand.Scan -> {
            scanRoom()
        }

        is RoomCommand.SyncStage -> {
            syncStage()
            Unit
        }

        is RoomCommand.SyncSemaphores -> {
            syncSemaphores()
        }

        is RoomIntent.EnsurePopulation -> {
            enqueue(msg)
        }
    }

    override suspend fun processRequest(msg: RoomRequest): RoomResponse<*> = when (msg) {
        StatusRequest -> StatusResponse(result = "room=$id")
        is RoomRequest.TryAcquireResource -> TryAcquireResourceResponse(
            result = tryAcquireResource(msg.ownerId, msg.resourceId)
        )
        is ReleaseResource -> ReleaseResourceResponse(
            result = releaseResource(msg.ownerId, msg.resourceId)
        )
    }

    override suspend fun planIntents(time: Int) {
        enqueue(
            RoomIntent.EnsurePopulation(
                priority = IntentPriority.NORMAL,
                createdTick = time,
                interruptible = true,
                role = Role.HARVESTER,
                targetCount = TARGET_HARVESTERS
            )
        )
    }

    override suspend fun executeIntent(intent: RoomIntent, time: Int): IntentResultType = when (intent) {
        is RoomIntent.EnsurePopulation -> {
            val currentPopulation: Int = requestFrom(
                Actors.SYSTEM,
                CountCreeps(homeRoom = id, role = intent.role)
            )

            if (currentPopulation >= intent.targetCount) {
                IntentResultType.COMPLETED
            } else {
                val availableSpawnActorId = self.find(FIND_MY_SPAWNS)
                    .firstOrNull { spawn -> spawn.spawning == null && ActorSystem.contains(spawn.id) }
                    ?.id

                if (availableSpawnActorId == null) {
                    IntentResultType.RETAINED
                } else {
                    sendTo(
                        availableSpawnActorId,
                        SpawnCommand.TrySpawn(role = intent.role)
                    )
                    IntentResultType.COMPLETED
                }
            }
        }
    }

    private fun scanRoom() {
        syncChildren()
        reconcileMissingResourceOwners()
    }

    private fun syncStage(): Boolean {
        val previous = self.memory.stage
        val current = RoomStagePlanner(self).calculate()
        self.memory.stage = current

        if (current != previous) {
            log.info("Room stage changed: $previous -> $current")
            return true
        }

        return false
    }

    private suspend fun syncSemaphores() {
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

    private fun tryAcquireResource(ownerId: String, resourceId: String): Boolean? {
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
                mirrorLockedResourceId(ownerId, resourceId)
                log.info("$resourceId acquired by $ownerId, OK")
            }
            false -> log.error("$resourceId acquired by $ownerId, FAIL")
            null -> log.error("$resourceId is not found by $ownerId, FAIL")
        }
        return result
    }

    private fun releaseResource(ownerId: String, resourceId: String): Boolean? {
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
                mirrorLockedResourceId(ownerId, null)
                log.info("$resourceId released by $ownerId, OK")
            }
            false -> log.error("$resourceId released by $ownerId, FAIL")
            null -> log.error("$resourceId is not found by $ownerId, FAIL")
        }
        return result
    }

    private suspend fun releaseAffectedResourceOwners(resourceIds: Set<String>) {
        if (resourceIds.isEmpty()) {
            return
        }

        val affectedOwners = collectAffectedOwnerIds(resourceIds)
        if (affectedOwners.isEmpty()) {
            return
        }

        var released = 0
        affectedOwners.forEach { ownerId ->
            val lockedId = self.memory.resourceLockOwners[ownerId] ?: return@forEach
            unassignOwner(ownerId)

            if (releaseResource(ownerId, lockedId) == true) {
                released++
            }
        }

        if (released > 0) {
            log.info("Released $released resource locks")
        }
    }

    private fun collectAffectedOwnerIds(resourceIds: Set<String>): Set<String> {
        val owners = self.memory.resourceLockOwners
        return owners.keys
            .filter { ownerId -> owners[ownerId] in resourceIds }
            .toSet()
    }

    private fun reconcileMissingResourceOwners() {
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

    private suspend fun unassignOwner(ownerId: String): Boolean {
        if (!ActorSystem.contains(ownerId)) {
            return false
        }

        return requestFrom(ownerId, Unassign)
    }

    private fun mirrorLockedResourceId(ownerId: String, resourceId: String?) {
        if (!ActorSystem.contains(ownerId)) {
            return
        }

        sendTo(ownerId, SetLockedResourceId(resourceId))
    }

}
