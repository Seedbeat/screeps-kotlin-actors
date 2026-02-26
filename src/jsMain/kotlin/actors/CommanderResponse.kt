package actors

import actor.message.IResponse

sealed class CommanderResponse<T>: IResponse<T>