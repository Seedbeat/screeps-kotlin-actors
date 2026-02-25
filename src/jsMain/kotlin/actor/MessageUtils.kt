package actor

import screeps.api.Game

object MessageUtils {
    private var messageSeed = 0 // TODO: Save/Load from Memory

    fun generateMessageId() = "${Game.time}|${++messageSeed}"
}