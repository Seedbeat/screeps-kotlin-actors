package actors

import actor.ActorSystem
import actors.RoomRequest.StatusRequest
import actors.RoomResponse.StatusResponse
import actors.base.*
import creep.enums.Role
import screeps.api.Room
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

class RoomActor(
    id: String
) : ActorIntentQueueBase<Room, RoomCommand, RoomIntent, RoomRequest, RoomResponse<*>>(id),
    IActorBinding<Room> by GameRoomBinding(id),
    IChildrenMultiManager,
    ILogging by Logging<RoomActor>(id, LogLevel.INFO) {

    override val managers = mapOf(
        SpawnActor::class.simpleName!! to RoomSpawnsManager(self)
    )

    override suspend fun processLifecycle(msg: Lifecycle) = when (msg) {
        is Lifecycle.Bootstrap -> {
            log.info("Bootstrapping room $id")
            syncChildren()
            broadcast(this, msg)
            processCommand(RoomCommand.Scan)
        }
        is Lifecycle.Tick -> {
            log.info("Room $id tick")
            syncChildren()
            broadcast(this, msg)
            processIntents(msg.time)
        }
    }

    override suspend fun processCommand(msg: RoomCommand) = when (msg) {
        is RoomCommand.Scan -> {
            scanRoom()
        }

        is RoomIntent.EnsurePopulation -> {
            enqueue(msg)
        }
    }

    override suspend fun processRequest(msg: RoomRequest): RoomResponse<*> = when (msg) {
        StatusRequest -> StatusResponse(result = "room=$id")
    }

    override suspend fun planIntents(time: Int) {
        val role = Role.HARVESTER
        val targetCount = 2

        managers[SpawnActor::class.simpleName!!]?.childrenIds?.forEach { id ->
            enqueue(
                RoomIntent.EnsurePopulation(
                    priority = IntentPriority.NORMAL,
                    createdTick = time,
                    interruptible = true,
                    spawnActorId = id,
                    role = role,
                    targetCount = targetCount
                )
            )
        }
    }

    override suspend fun executeIntent(intent: RoomIntent, time: Int): IntentResultType = when (intent) {
        is RoomIntent.EnsurePopulation -> {
            if (!ActorSystem.contains(intent.spawnActorId)) {
                IntentResultType.RETAINED
            } else {
                sendTo(
                    intent.spawnActorId,
                    SpawnCommand.EnsurePopulation(
                        role = intent.role,
                        targetCount = intent.targetCount
                    )
                )
                IntentResultType.COMPLETED
            }
        }
    }

    private fun scanRoom() {
        syncChildren()
    }


}
