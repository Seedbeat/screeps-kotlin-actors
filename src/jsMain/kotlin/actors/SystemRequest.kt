package actors

import actor.message.Request
import screeps.api.Creep
import kotlin.reflect.KClass

sealed class SystemRequest<T> : Request<T> {
    sealed class Query<T> : SystemRequest<T>() {
        abstract val limit: Int?

        data class Creeps(
            override val limit: Int? = null,
            val predicate: (Creep, CreepAssignment?) -> Boolean
        ) : Query<List<CreepStatus>>()

        data class CreepsByAssignment<T : CreepAssignment>(
            override val limit: Int? = null,
            val type: KClass<T>,
            val predicate: (Creep, T) -> Boolean
        ) : Query<List<CreepStatus>>()

        companion object {
            inline fun <reified T : CreepAssignment> creepsByAssignment(
                limit: Int? = null,
                noinline predicate: (Creep, T) -> Boolean
            ) = CreepsByAssignment(limit, type = T::class, predicate)
        }
    }
}
