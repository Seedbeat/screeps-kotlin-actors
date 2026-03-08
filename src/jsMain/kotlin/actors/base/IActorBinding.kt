package actors.base

import screeps.api.*
import utils.lazyPerTickNullable

interface IActorBinding<T> {
    val selfOrNull: T?
    val self: T get() = selfOrNull ?: error("Actor is not bound")
    fun isBound() = selfOrNull != null
}

object NoBinding : IActorBinding<Unit> {
    override val selfOrNull: Unit = Unit
}

class GameObjectBinding<T : Identifiable>(id: String) : IActorBinding<T> {
    override val selfOrNull: T? by lazyPerTickNullable { Game.getObjectById(id) }
}

class GameRoomBinding(id: String) : IActorBinding<Room> {
    override val selfOrNull: Room? by lazyPerTickNullable { Game.rooms[id] }
}

class GameCreepBinding(name: String) : IActorBinding<Creep> {
    override val selfOrNull: Creep? by lazyPerTickNullable { Game.creeps[name] }
}
