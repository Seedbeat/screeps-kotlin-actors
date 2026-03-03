package actors

import actor.message.ICommand

sealed class RoomCommand : ICommand {
    data class Simple(val str: String) : RoomCommand()
}
