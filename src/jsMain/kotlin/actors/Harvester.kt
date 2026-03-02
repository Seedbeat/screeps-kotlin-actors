package actors

import actors.HarvesterCommand.Harvest
import actors.HarvesterCommand.Transfer
import actors.HarvesterRequest.GetEnergy
import actors.HarvesterResponse.EnergyResponse
import actors.base.GameObjectBinding
import actors.base.IActorBinding
import screeps.api.Creep
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

class Harvester(id: String) :
    ActorBase<Creep, HarvesterCommand, HarvesterRequest, HarvesterResponse<*>>(id),
    IActorBinding<Creep> by GameObjectBinding(id),
    ILogging by Logging<Harvester>(id, LogLevel.INFO) {


    override suspend fun processCommand(msg: HarvesterCommand) = when (msg) {
        is Harvest -> {
            log.info("harvest ${msg.sourceId}")
        }

        is Transfer -> {
            log.info("transfer ${msg.targetId}")
        }
    }

    override suspend fun processRequest(msg: HarvesterRequest): HarvesterResponse<*> = when (msg) {
        is GetEnergy -> EnergyResponse(50)
    }
}