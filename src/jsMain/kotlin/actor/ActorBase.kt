package actor

import actor.message.*

abstract class ActorBase<CommandType : ICommand, RequestType : IRequest, ResponseType : IResponse<*>>(
    id: String
) : Actor(id) {

    override suspend fun run() {
        while (true) {
            val msg = receive<IMessage>()
            log.info("[${msg.messageId}]: $id received new message from '${msg.from}'")

            try {
                onReceive(msg.payload)?.let { response ->
                    log.info("[${msg.messageId}]: $id send response to '${msg.from}'")
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

        else -> null
    }

    abstract suspend fun processCommand(msg: CommandType)
    abstract suspend fun processRequest(msg: RequestType): ResponseType
}
