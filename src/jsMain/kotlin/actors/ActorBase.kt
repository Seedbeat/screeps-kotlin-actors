package actors

import actor.Actor
import actor.message.*
import actors.base.ActorApi
import actors.base.ActorBinding
import actors.base.Actors
import actors.base.Lifecycle
import kotlin.reflect.KClass

abstract class ActorBase<
        ObjectType,
        CommandType : Command,
        RequestType : Request<*>,
        ResponseType : Response<*>>(
    id: String
) : Actor(id), ActorBinding<ObjectType>, ActorApi {

    override suspend fun run() {
        while (true) {
            val msg = receive<Message>()
            log.debug("[${msg.messageId}]: received new message from '${msg.from}'")

            try {
                if (!isBound()) {
                    return
                }
                onReceive(msg.payload)?.let { response ->
                    log.debug("[${msg.messageId}]: send response to '${msg.from}'")
                    reply(msg, response)
                }
            } catch (exception: Throwable) {
                log.error("[Root exception handler] fail to process $msg", exception.stackTraceToString())
            }
        }
    }

    suspend fun onReceive(msg: Payload): ResponseType? = when (msg) {
        is Lifecycle -> {
            processLifecycle(msg)
            null
        }

        is Command -> {
            @Suppress("UNCHECKED_CAST")
            processCommand(msg as CommandType)
            null
        }

        is Request<*> -> {
            @Suppress("UNCHECKED_CAST")
            processRequest(msg as RequestType)
        }

        else -> {
            log.error("Not implemented for $msg")
            throw NotImplementedError()
        }
    }

    open suspend fun processLifecycle(msg: Lifecycle) = Unit
    abstract suspend fun processCommand(msg: CommandType)
    abstract suspend fun processRequest(msg: RequestType): ResponseType

    override fun systemSend(payload: Payload) =
        sendTo(actorId = Actors.SYSTEM, payload = payload)

    override suspend fun <T> systemRequest(payload: Request<T>): T =
        requestFrom(actorId = Actors.SYSTEM, payload = payload)

    protected suspend inline fun <reified C, T> cpuMark(noinline block: suspend () -> T): T =
        cpuMark(C::class, block)

    protected suspend fun <T> cpuMark(type: KClass<*>, block: suspend () -> T): T {
        val label = type.simpleName ?: "Unknown"
        // Mailbox delivery already opens the message type scope.
        if (profiler.isMessageScope(label)) return block()
        return cpuMark(label, block)
    }

    protected suspend fun <T> cpuMark(label: String, block: suspend () -> T): T {
        profiler.push(label)
        return try {
            block()
        } finally {
            profiler.pop()
        }
    }
}
