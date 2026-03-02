package actors

import actor.message.IRequest

sealed class RoomRequest : IRequest {
    data object StatusRequest : RoomRequest()
}

