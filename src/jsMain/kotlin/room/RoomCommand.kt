package room

import actor.message.Command

sealed interface RoomCommand : Command {
    data object Scan : RoomCommand
    data object SyncStage : RoomCommand
    data object SyncPlanningCache : RoomCommand
    data object SyncSemaphores : RoomCommand
}
