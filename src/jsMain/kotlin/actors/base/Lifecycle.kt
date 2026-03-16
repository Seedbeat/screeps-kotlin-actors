package actors.base

import actor.message.Command

sealed class Lifecycle : Command {
    data class Tick(val time: Int) : Lifecycle()
    data object Bootstrap : Lifecycle()
}