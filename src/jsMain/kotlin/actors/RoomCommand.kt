package actors

import actor.message.Command

sealed class RoomCommand : Command {
    data object Scan : RoomCommand()
    data object SyncStage : RoomCommand()
    data object SyncSemaphores : RoomCommand()
}
