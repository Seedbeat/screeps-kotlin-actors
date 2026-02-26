package actors

import actor.ActorBase
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

class Harvester(id: String) :
    ActorBase<HarvesterCommand, HarvesterRequest, HarvesterResponse<*>>(id),
    ILogging by Logging.Companion<Harvester>(id, LogLevel.INFO) {


    override suspend fun processCommand(msg: HarvesterCommand) = when (msg) {
        is HarvesterHarvest -> {
            log.info("harvest ${msg.sourceId}")
        }

        is HarvesterTransfer -> {
            log.info("transfer ${msg.targetId}")
        }
    }

    override suspend fun processRequest(msg: HarvesterRequest): HarvesterResponse<*> = when (msg) {
        is HarvesterGetEnergy -> HarvesterGetEnergyResponse(50)
    }
}