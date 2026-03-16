package actor.message

data class BaseMessage(
    override val messageId: String,
    override val from: String,
    override val payload: Payload
) : Message
