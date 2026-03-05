package actors.base

import actor.Actor
import actor.message.IPayload

interface IChildrenMultiManager {
    val managers: Map<String, IChildrenManager<*>>

    fun broadcast(self: Actor, payload: IPayload) = managers.values.forEach {
        it.broadcast(self, payload)
    }

    fun syncChildren() = managers.values.forEach { it.syncChildren() }
}