package actors

import actor.message.Command
import actor.message.Request
import actor.message.Response
import actors.base.Intent
import actors.base.IntentEntry
import actors.base.IntentResultType
import screeps.api.Game

abstract class ActorIntentBase<
        ObjectType,
        CommandType : Command,
        IntentType : Intent,
        RequestType : Request<*>,
        ResponseType : Response<*>>(
    id: String
) : ActorBase<ObjectType, CommandType, RequestType, ResponseType>(id) {

    private val intents = linkedMapOf<String, IntentEntry<IntentType>>()
    open val maxIntentsPerTick: Int = 5

    protected abstract suspend fun executeIntent(intent: IntentType, time: Int): IntentResultType

    protected fun addIntent(intent: IntentType) {
        val entry = intents[intent.intentId]

        if (entry != null) {
            intents[intent.intentId] = entry.copy(intent = intent)
        } else {
            intents[intent.intentId] = IntentEntry(intent = intent, createdTick = Game.time)
        }
    }

    protected suspend fun processIntents(time: Int) {
        var processed = 0
        while (processed < maxIntentsPerTick) {
            val entry = selectNextIntent(time) ?: break
            entry.lastAttemptedTick = time

            when (executeIntent(entry.intent, time)) {
                IntentResultType.COMPLETED,
                IntentResultType.DROPPED -> {
                    if (!entry.intent.repeatable) {
                        intents.remove(entry.intent.intentId)
                        log.info("Processed intent ${entry.intent}")
                    }
                }

                IntentResultType.RETAINED -> Unit
            }

            processed++
        }
    }

    private fun selectNextIntent(time: Int): IntentEntry<IntentType>? {
        var best: IntentEntry<IntentType>? = null
        var bestNeverAttempted = false
        var bestScore = Int.MIN_VALUE
        var bestLastAttemptedTick = Int.MIN_VALUE

        for (entry in intents.values) {
            val lastAttemptedTick = entry.lastAttemptedTick
            if (lastAttemptedTick == time) continue

            val neverAttempted = lastAttemptedTick == null
            val score = entry.selectionScore(time)
            val normalizedLastAttemptedTick = lastAttemptedTick ?: Int.MIN_VALUE

            val better = when {
                best == null -> true
                neverAttempted != bestNeverAttempted -> neverAttempted
                score != bestScore -> score > bestScore
                else -> normalizedLastAttemptedTick < bestLastAttemptedTick
            }

            if (!better) continue

            best = entry
            bestNeverAttempted = neverAttempted
            bestScore = score
            bestLastAttemptedTick = normalizedLastAttemptedTick
        }

        return best
    }
}
