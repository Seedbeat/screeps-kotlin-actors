package actors

import actors.RoomResponse.*
import actors.base.*
import memory.planningCache
import memory.stage
import screeps.api.Room
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

class RoomActor(
    id: String
) : ActorIntentBase<Room, RoomCommand, RoomIntent, RoomRequest<*>, RoomResponse<*>>(id),
    ActorBinding<Room> by GameRoomBinding(id),
    ChildrenMultiManager,
    ILogging by Logging<RoomActor>(id, LogLevel.INFO) {

    companion object {
        private const val STAGE_SYNC_INTERVAL = 2
        private const val SEMAPHORE_SYNC_INTERVAL = 3
        private const val PLANNING_CACHE_SYNC_INTERVAL = 4
    }

    override val managers = mapOf(
        SpawnActor::class.simpleName!! to RoomSpawnsManager(self)
    )

    private val semaphoreService = RoomSemaphoreService(api = this)
    private val planningService = RoomPlanningService(api = this)

    override fun onDestroy() {
        destroyChildren()
    }

    override suspend fun processLifecycle(msg: Lifecycle) = when (msg) {
        is Lifecycle.Bootstrap -> {
            log.info("Bootstrapping room $id")
            processCommand(RoomCommand.Scan)
            processCommand(RoomCommand.SyncStage)
            processCommand(RoomCommand.SyncPlanningCache)
            processCommand(RoomCommand.SyncSemaphores)

            RoomIntent.recurring.forEach { intent ->
                processCommand(intent)
            }

            // Should be last
            broadcast(this, msg)
        }

        is Lifecycle.Tick -> {
            log.info("Room $id tick")
            processCommand(RoomCommand.Scan)
            msg.onEach(STAGE_SYNC_INTERVAL) { processCommand(RoomCommand.SyncStage) }
            msg.onEach(PLANNING_CACHE_SYNC_INTERVAL) { processCommand(RoomCommand.SyncPlanningCache) }
            msg.onEach(SEMAPHORE_SYNC_INTERVAL) { processCommand(RoomCommand.SyncSemaphores) }

            processIntents(msg.time)

            // Should be last
            broadcast(this, msg)
        }
    }

    override suspend fun processCommand(msg: RoomCommand) = when (msg) {
        is RoomCommand.Scan -> scanRoom()
        is RoomCommand.SyncStage -> syncStage()
        is RoomCommand.SyncPlanningCache -> syncPlanningCache()
        is RoomCommand.SyncSemaphores -> semaphoreService.syncSemaphores()

        is RoomIntent -> addIntent(msg)
    }

    override suspend fun processRequest(msg: RoomRequest<*>): RoomResponse<*> = when (msg) {
        is RoomRequest.TryAcquireResourceById -> TryAcquireResourceResponse(
            result = semaphoreService.tryAcquireResource(msg.ownerId, msg.resourceId)
        )

        is RoomRequest.TryAcquireResourceByType -> TryAcquireAnyResourceResponse(
            result = semaphoreService.tryAcquireAnyResource(msg.ownerId, msg.near, msg.type)
        )

        is RoomRequest.ReleaseResourceById -> ReleaseResourceResponse(
            result = semaphoreService.releaseResource(msg.ownerId, msg.resourceId)
        )
    }

    override suspend fun executeIntent(intent: RoomIntent, time: Int): IntentResultType = when (intent) {
        is RoomIntent.EnsureControllerSurvival -> planningService.ensureControllerSurvival()
        is RoomIntent.PlanWorkforce -> planningService.planWorkforce()
    }

    private fun scanRoom() {
        syncChildren()
        semaphoreService.reconcileMissingResourceOwners()
    }

    private fun syncStage() {
        val previous = self.memory.stage
        val current = RoomStagePlanner.calculate(self)
        self.memory.stage = current

        if (current != previous) {
            log.info("Room stage changed: $previous -> $current")
        }
    }

    private fun syncPlanningCache() {
        self.memory.planningCache = RoomPlanningAnalyzer.analyze(self)
    }

}
