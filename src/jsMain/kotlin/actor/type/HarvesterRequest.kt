package actor.type

import actor.message.IRequest

sealed class HarvesterRequest : IRequest

class HarvesterGetEnergy : HarvesterRequest()
