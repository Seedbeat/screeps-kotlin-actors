package actor.type

import actor.message.ICommand

sealed class CommanderCommand: ICommand

data class CommanderCommandSimple(
    val str: String
) : CommanderCommand()