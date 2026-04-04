package actors.base

import actor.message.Command

sealed class Lifecycle : Command {
    abstract val time: Int

    data class Tick(override val time: Int) : Lifecycle()
    data class Bootstrap(override val time: Int) : Lifecycle()
}