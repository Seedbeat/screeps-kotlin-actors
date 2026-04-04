 package actors.base

import actor.ActorSystem
import actor.message.Payload
import actors.SystemActor
import screeps.api.Game

object Actors {
    const val SYSTEM = "SYSTEM"

    fun init() {
        ActorSystem.spawn(SYSTEM, ::SystemActor)
        systemMessage(Lifecycle.Bootstrap(Game.time))
    }

    fun tick() {
        systemMessage(Lifecycle.Tick(Game.time))
        ActorSystem.tick()
    }

    private fun systemMessage(msg: Payload) {
        ActorSystem.send(SYSTEM, SYSTEM, msg)
    }
}