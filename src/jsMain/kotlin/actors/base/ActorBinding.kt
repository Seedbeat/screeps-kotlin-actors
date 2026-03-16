package actors.base

import screeps.api.*
import utils.lazyPerTickNullable

interface ActorBinding<T> {
    val selfOrNull: T?
    val self: T get() = selfOrNull ?: error("Actor is not bound")
    fun isBound() = selfOrNull != null
}

object NoBinding : ActorBinding<Unit> {
    override val selfOrNull: Unit = Unit
}

class GameObjectBinding<T : Identifiable>(id: String) : ActorBinding<T> {
    override val selfOrNull: T? by lazyPerTickNullable { Game.getObjectById(id) }
}

class GameRoomBinding(id: String) : ActorBinding<Room> {
    override val selfOrNull: Room? by lazyPerTickNullable { Game.rooms[id] }
}

class GameCreepBinding(name: String) : ActorBinding<Creep> {
    override val selfOrNull: Creep? by lazyPerTickNullable { Game.creeps[name] }
}
