package actor.message

data class Message(
    override val messageId: String,
    override val from: String,
    override val payload: IPayload
) : IMessage
