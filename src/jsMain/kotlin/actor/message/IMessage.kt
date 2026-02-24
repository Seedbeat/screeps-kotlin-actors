package actor.message

sealed interface IMessage {
    val from: String
    val payload: IPayload
    val messageId: String
}
