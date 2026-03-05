package actors

import actor.message.ICommand

sealed class RoomCommand : ICommand {
    data object Scan : RoomCommand()
}
