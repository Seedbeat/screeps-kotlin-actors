package actor

import screeps.api.Game

object MessageUtils {
    private var messageSeed = 0

    fun resetSeed() { messageSeed = 0 }

    fun generateMessageId() = "${Game.time}|${++messageSeed}"
}