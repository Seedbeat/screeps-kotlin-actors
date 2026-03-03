package actors

import actors.RoomRequest.StatusRequest
import actors.RoomResponse.StatusResponse
import actors.base.GameRoomBinding
import actors.base.IActorBinding
import actors.base.Lifecycle
import screeps.api.Room
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

class RoomActor(
    id: String
) : ActorBase<Room, RoomCommand, RoomRequest, RoomResponse<*>>(id),
    IActorBinding<Room> by GameRoomBinding(id),
    ILogging by Logging<RoomActor>(id, LogLevel.INFO) {

    override suspend fun processLifecycle(msg: Lifecycle) = when (msg) {
        is Lifecycle.Bootstrap -> {} // TODO
        is Lifecycle.Tick -> planRoomIntents(msg.time)
    }

    override suspend fun processCommand(msg: RoomCommand) = when (msg) {
        is RoomCommand.Simple -> {
            log.info(msg)
        }

        is RoomIntent.EnsurePopulation -> TODO()
    }

    override suspend fun processRequest(msg: RoomRequest): RoomResponse<*> = when (msg) {
        StatusRequest -> StatusResponse(result = "room=$id")
    }

    private fun planRoomIntents(time: Int) {
    }
}
