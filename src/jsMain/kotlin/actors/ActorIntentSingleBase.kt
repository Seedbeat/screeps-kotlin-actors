package actors

import actor.message.Command
import actor.message.Request
import actor.message.Response
import actors.base.Intent
import actors.base.IntentResultType

abstract class ActorIntentSingleBase<
        ObjectType,
        CommandType : Command,
        IntentType : Intent,
        RequestType : Request,
        ResponseType : Response<*>>(
    id: String
) : ActorIntentBase<ObjectType, CommandType, IntentType, RequestType, ResponseType>(id) {
    protected var currentIntent: IntentType? = null

    protected abstract suspend fun planIntent(time: Int): IntentType?
    protected abstract suspend fun executeSingleIntent(intent: IntentType, time: Int): IntentResultType

    final override suspend fun planIntents(time: Int) {
        if (currentIntent != null) return
        currentIntent = planIntent(time)
    }

    final override suspend fun executeIntent(intent: IntentType, time: Int): IntentResultType =
        executeSingleIntent(intent, time)

    override fun enqueue(intent: IntentType) {
        currentIntent = intent
    }

    override fun select(time: Int, excludedIntentIds: Set<String>): IntentType? {
        val intent = currentIntent ?: return null
        if (excludedIntentIds.contains(intent.intentId)) return null
        return intent
    }

    override fun remove(intent: IntentType) {
        if (currentIntent?.intentId != intent.intentId) return
        currentIntent = null
    }
}
