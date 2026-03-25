package actors.base

import screeps.api.*
import utils.lazyPerTick

interface ActorBinding<T> {
    val selfOrNull: T?
    val self: T get() = selfOrNull ?: error("Actor is not bound")
    fun isBound() = selfOrNull != null
}

object NoBinding : ActorBinding<Unit> {
    override val selfOrNull: Unit = Unit
}

class GameObjectBinding<T : Identifiable>(id: String) : ActorBinding<T> {
    override val selfOrNull: T? by lazyPerTick { Game.getObjectById(id) }
}

class GameRoomBinding(name: String) : ActorBinding<Room> {
    override val selfOrNull: Room? by lazyPerTick { Game.rooms[name] }
}

class GameCreepBinding(name: String) : ActorBinding<Creep> {
    override val selfOrNull: Creep? by lazyPerTick { Game.creeps[name] }
}
