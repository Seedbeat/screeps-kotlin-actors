package actors.base

import actor.MessagingApi
import actor.message.Payload
import actor.message.Request

interface ActorApi : MessagingApi {
    fun systemSend(payload: Payload): Boolean
    suspend fun <T> systemRequest(payload: Request<T>): T
}