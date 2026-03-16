package actors.base

import actor.Actor
import actor.message.Payload

interface ChildrenMultiManager {
    val managers: Map<String, ChildrenManager<*>>

    fun broadcast(self: Actor, payload: Payload) = managers.values.forEach {
        it.broadcast(self, payload)
    }

    fun syncChildren() = managers.values.forEach { it.syncChildren() }

    fun destroyChildren() = managers.values.forEach { it.destroyChildren() }
}
