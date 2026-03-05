package actors

import actor.message.ICommand
import actor.message.IRequest
import actor.message.IResponse
import actors.base.IIntent

abstract class ActorIntentQueueBase<
        ObjectType,
        CommandType : ICommand,
        IntentType : IIntent,
        RequestType : IRequest,
        ResponseType : IResponse<*>>(
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
