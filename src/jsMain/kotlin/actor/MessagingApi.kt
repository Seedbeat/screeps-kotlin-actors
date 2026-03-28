package actor

import actor.message.Message
import actor.message.Payload
import actor.message.Request

interface MessagingApi {
    val id: String

    fun sendTo(actorId: String, payload: Payload): Boolean
    suspend fun <T> requestFrom(actorId: String, payload: Request<T>): T
    fun reply(msg: Message, response: Payload): Boolean

    fun selfSend(payload: Payload): Boolean
    suspend fun <T> selfRequest(payload: Request<T>): T

    fun Payload.sendTo(actorId: String): Boolean = sendTo(actorId, this)
    suspend fun <T> Request<T>.request(actorId: String): T = requestFrom(actorId, this)
}