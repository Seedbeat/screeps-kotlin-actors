package actors

import actor.message.ICommand

sealed class SystemCommand : ICommand {
    data class OnTick(
        val time: Int
    ) : SystemCommand()

    data object Bootstrap : SystemCommand()
}