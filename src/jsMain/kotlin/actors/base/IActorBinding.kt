package actors.base

import screeps.api.Game
import screeps.api.Identifiable
import screeps.api.Room
import screeps.api.get
import utils.lazyPerTickNullable

interface IActorBinding<T> {
    val selfOrNull: T?
    val self: T get() = selfOrNull ?: error("Actor is not bound")
}

object NoBinding : IActorBinding<Unit> {
    override val selfOrNull: Unit? = null
}

class GameObjectBinding<T : Identifiable>(id: String) : IActorBinding<T> {
    override val selfOrNull: T? by lazyPerTickNullable { Game.getObjectById(id) }
}

class GameRoomBinding(id: String) : IActorBinding<Room> {
    override val selfOrNull: Room? by lazyPerTickNullable { Game.rooms[id] }
}