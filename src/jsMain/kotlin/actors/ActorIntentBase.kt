package actors

import actor.message.Command
import actor.message.Request
import actor.message.Response
import actors.base.Intent
import actors.base.IntentResultType

abstract class ActorIntentBase<
        ObjectType,
        CommandType : Command,
        IntentType : Intent,
        RequestType : Request,
        ResponseType : Response<*>>(
    id: String
) : ActorBase<ObjectType, CommandType, RequestType, ResponseType>(id) {

    protected abstract suspend fun planIntents(time: Int)
    protected abstract suspend fun executeIntent(intent: IntentType, time: Int): IntentResultType
    protected open fun agingStepTicks(): Int = 10
    protected open fun maxIntentsPerTick(): Int = 1

    protected abstract fun enqueue(intent: IntentType)

    protected abstract fun select(time: Int, excludedIntentIds: Set<String> = emptySet()): IntentType?

    protected abstract fun remove(intent: IntentType)

    protected suspend fun processIntents(time: Int) {
        planIntents(time)

        val attemptedIntentIds = mutableSetOf<String>()
        repeat(maxIntentsPerTick().coerceAtLeast(0)) {
            val intent = select(time, attemptedIntentIds) ?: return
            attemptedIntentIds += intent.intentId

            when (executeIntent(intent, time)) {
                IntentResultType.COMPLETED,
                IntentResultType.DROPPED -> {
                    log.info("Processed intent $intent to $time")
                    remove(intent)
                }

                IntentResultType.RETAINED -> Unit
            }
        }
    }
}
