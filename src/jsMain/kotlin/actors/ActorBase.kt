package actors

import actor.Actor
import actor.message.*
import actors.base.ActorBinding
import actors.base.Lifecycle

abstract class ActorBase<
        ObjectType,
        CommandType : Command,
        RequestType : Request,
        ResponseType : Response<*>>(
    id: String
) : Actor(id), ActorBinding<ObjectType> {

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
                    sendTo(msg.from, response, msg.messageId)
                }
            } catch (_: Exception) {
                log.error("[Root exception handler] fail to process $msg")
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
            processCommand((msg as CommandType))
            null
        }

        is Request -> {
            @Suppress("UNCHECKED_CAST")
            processRequest((msg as RequestType))
        }

        else -> {
            log.error("Not implemented for $msg")
            throw NotImplementedError()
        }
    }

    open suspend fun processLifecycle(msg: Lifecycle) = Unit
    abstract suspend fun processCommand(msg: CommandType)
    abstract suspend fun processRequest(msg: RequestType): ResponseType
}