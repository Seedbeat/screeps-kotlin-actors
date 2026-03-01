package actor

import screeps.api.Game

object MessageId {
    private var messageSeed = 0

    fun resetSeed() { messageSeed = 0 }

    fun next() = "${Game.time}|${++messageSeed}"
}