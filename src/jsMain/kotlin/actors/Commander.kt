package actors

import actor.ActorBase
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

class Commander(id: String) :
    ActorBase<CommanderCommand, CommanderRequest, CommanderResponse<*>>(id),
    ILogging by Logging.Companion<Commander>(id, LogLevel.INFO) {

    override suspend fun processCommand(msg: CommanderCommand) {
        log.info(msg)
        val energy = requestFrom<Int>("harvester", HarvesterGetEnergy())
        log.info("Energy: $energy")
    }

    override suspend fun processRequest(msg: CommanderRequest): CommanderResponse<*> {
        log.info(msg)
        TODO("Not yet implemented")
    }

}