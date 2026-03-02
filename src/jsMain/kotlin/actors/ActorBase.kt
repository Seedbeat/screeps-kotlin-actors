package actors

import actor.Actor
import actor.message.*
import actors.base.IActorBinding

abstract class ActorBase<
        ObjectType,
        CommandType : ICommand,
        RequestType : IRequest,
        ResponseType : IResponse<*>>(
    id: String
) : Actor(id), IActorBinding<ObjectType> {

    override suspend fun run() {
        while (true) {
            val msg = receive<IMessage>()
            log.debug("[${msg.messageId}]: received new message from '${msg.from}'")

            try {
                onReceive(msg.payload)?.let { response ->
                    log.debug("[${msg.messageId}]: send response to '${msg.from}'")
                    sendTo(msg.from, response, msg.messageId)
                }
            } catch (_: Exception) {
                log.error("[Root exception handler] fail to process $msg")
            }
        }
    }

    suspend fun onReceive(msg: IPayload): ResponseType? = when (msg) {
        is ICommand -> {
            @Suppress("UNCHECKED_CAST")
            processCommand((msg as CommandType))
            null
        }

        is IRequest -> {
            @Suppress("UNCHECKED_CAST")
            processRequest((msg as RequestType))
        }

        else -> {
            log.error("Not implemented for $msg")
            throw NotImplementedError()
        }
    }

    abstract suspend fun processCommand(msg: CommandType)
    abstract suspend fun processRequest(msg: RequestType): ResponseType
}