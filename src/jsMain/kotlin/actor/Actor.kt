package actor

import actor.command.Command
import actor.request.Request
import actor.response.Response

abstract class Actor<CommandType : Command, RequestType : Request, ResponseType : Response> {

    abstract fun processCommand(command: CommandType): Unit
    abstract fun processRequest(request: RequestType): ResponseType


}