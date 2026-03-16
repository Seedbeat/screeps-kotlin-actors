package actors

import actor.message.Command

sealed class SystemCommand : Command {
    data object Noop : SystemCommand()
}
