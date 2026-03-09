 package actors.base

import actor.ActorSystem
import actor.message.IPayload
import actors.SystemActor
import screeps.api.Game

object Actors {
    const val SYSTEM = "SYSTEM"

    fun init() {
        ActorSystem.spawn(SYSTEM, ::SystemActor)
        systemMessage(Lifecycle.Bootstrap)
    }

    fun tick() {
        systemMessage(Lifecycle.Tick(Game.time))
        ActorSystem.tick()
    }

    private fun systemMessage(msg: IPayload) {
        ActorSystem.send(SYSTEM, SYSTEM, msg)
    }
}