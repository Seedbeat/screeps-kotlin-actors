package actors.base

import actor.message.ICommand

sealed class Lifecycle : ICommand {
    data class Tick(val time: Int) : Lifecycle()
    data object Bootstrap : Lifecycle()
}