package actor.type

import actor.message.IResponse

sealed class CommanderResponse<T>: IResponse<T>