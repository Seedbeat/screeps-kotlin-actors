package actors

import actor.message.Command

sealed interface SystemCommand : Command {
    data object Noop : SystemCommand
}
