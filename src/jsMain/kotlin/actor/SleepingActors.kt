package actor

import actor.message.IMessage
import utils.log.ILogging
import utils.log.Logging
import utils.log.LogLevel
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

object SleepingActors : ILogging by Logging<SleepingActors>(LogLevel.DEBUG) {
    private val sleeping = mutableMapOf<String, Continuation<IMessage>>()

    fun isSleeping(actorId: String) = sleeping.containsKey(actorId)
    fun remove(actorId: String) = sleeping.remove(actorId)

    fun update(actorId: String, continuation: Continuation<IMessage>) {
        log.debug("Update continuation of actor '$actorId'")
        sleeping[actorId] = continuation
    }

    fun wake(actorId: String, msg: IMessage) {
        log.debug("[${msg.messageId}] Waking actor '$actorId'")
        remove(actorId)?.resume(msg)
    }
}