package actors.base

import actor.Actor
import actor.ActorSystem
import actor.message.IPayload
import actors.RoomActor

interface IChildManager<T> {
    val targetChildrenIds: Set<String>
    fun filterRegisteredChildren(actors: Collection<Actor>): Collection<Actor>

    fun Actor.broadcastToChildren(payload: IPayload) =
        targetChildrenIds.forEach { actorId -> sendTo(actorId, payload) }

    fun syncOwnedChildren(create: (actorId: String) -> T) {
        val roomActorsIds = ActorSystem.actors().values
            .let(::filterRegisteredChildren)
            .map(Actor::id)
            .toSet()

        val toCreate = targetChildrenIds - roomActorsIds
        val toRemove = roomActorsIds - targetChildrenIds

        toCreate.forEach { actorId -> ActorSystem.spawn(actorId, ::RoomActor) }
        toRemove.forEach { actorId -> ActorSystem.remove(actorId) }
    }
}