package actors

import actor.message.Command
import actor.message.Request
import actor.message.Response
import actors.base.Intent

abstract class ActorIntentQueueBase<
        ObjectType,
        CommandType : Command,
        IntentType : Intent,
        RequestType : Request<*>,
        ResponseType : Response<*>>(
    id: String
) : ActorIntentBase<ObjectType, CommandType, IntentType, RequestType, ResponseType>(id) {
    protected val pendingIntents = linkedMapOf<String, IntentType>()

    override fun enqueue(intent: IntentType) {
        pendingIntents.getOrPut(intent.intentId) { intent }
    }

    override fun select(time: Int, excludedIntentIds: Set<String>): IntentType? =
        pendingIntents.values
            .asSequence()
            .filterNot { intent -> excludedIntentIds.contains(intent.intentId) }
            .maxByOrNull { intent -> intent.effectivePriorityScore(time, agingStepTicks()) }

    override fun remove(intent: IntentType) {
        pendingIntents.remove(intent.intentId)
    }
}
