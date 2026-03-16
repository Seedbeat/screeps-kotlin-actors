package actor.message

sealed interface Message {
    val from: String
    val payload: Payload
    val messageId: String
}
