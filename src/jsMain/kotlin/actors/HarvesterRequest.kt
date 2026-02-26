package actors

import actor.message.IRequest

sealed class HarvesterRequest : IRequest

class HarvesterGetEnergy : HarvesterRequest()
