package actors

import actor.message.ICommand

sealed class SystemCommand : ICommand {
    data object Noop : SystemCommand()
}
