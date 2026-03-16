package actors

import actor.message.Request

sealed class HarvesterRequest : Request {

    class GetEnergy : HarvesterRequest()
}

