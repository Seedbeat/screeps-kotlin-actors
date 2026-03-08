package actors

import actor.message.ICommand

sealed class RoomCommand : ICommand {
    data object Scan : RoomCommand()
    data object SyncStage : RoomCommand()
    data object SyncSemaphores : RoomCommand()
}
