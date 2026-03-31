package actors

import actors.base.ActorApi
import actors.base.ActorBinding
import actors.base.IntentResultType
import creep.capabilities
import memory.planningCache
import room.constructionSites
import room.structures
import screeps.api.BUILD_POWER
import screeps.api.Creep
import screeps.api.Room
import screeps.api.structures.Structure
import store.energyStore
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

class RoomPlanningService<T>(
    api: T
) : ActorApi by api,
    ActorBinding<Room> by api,
    ILogging by Logging<RoomPlanningService<T>>(api.self.name, LogLevel.INFO)
        where T : ActorApi,
              T : ActorBinding<Room> {
    suspend fun ensureControllerSurvival(): IntentResultType {
        val controller = self.controller
            ?: return IntentResultType.RETAINED

        val alreadyAssigned = systemRequest(
            payload = SystemRequest.Query.creepsByAssignment<CreepAssignment.ControllerUpkeep>(limit = 1)
            { _, assignment -> assignment.roomName == self.name && assignment.controllerId == controller.id }
        ).isNotEmpty()

        if (alreadyAssigned) {
            return IntentResultType.COMPLETED
        }

        if (assignIfAvailable(
                actorId = availableUnassignedCreepActorId { creep, assignment ->
                    creep.capabilities.canDoControllerUpkeep && assignment == null
                },
                assignment = CreepAssignment.ControllerUpkeep(
                    roomName = self.name,
                    controllerId = controller.id
                )
            )
        ) {
            return IntentResultType.COMPLETED
        }

        val spawnActorId = availableSpawnActorId()
            ?: return IntentResultType.RETAINED

        SpawnCommand.TrySpawnControllerSurvivalWorker(
            roomName = self.name,
            controllerId = controller.id
        ).sendTo(spawnActorId)

        return IntentResultType.COMPLETED
    }

    suspend fun ensureConstruction(): IntentResultType {
        val constructionSites = self.constructionSites.my
        if (constructionSites.isEmpty()) {
            return IntentResultType.DROPPED
        }

        val assignedBuilders = systemRequest(
            payload = SystemRequest.Query.creepsByAssignment<CreepAssignment.Construction> { _, assignment ->
                assignment.roomName == self.name
            }
        )

        val assignedSiteCounts = assignedBuilders
            .mapNotNull { creep -> (creep.assignment as? CreepAssignment.Construction)?.constructionSiteId }
            .groupingBy { siteId -> siteId }
            .eachCount()

        val assignedThroughput = assignedBuilders.sumOf { creep ->
            creep.capabilities.work * BUILD_POWER
        }

        val analysis = ConstructionPlanningPolicy.analyze(
            room = self,
            planningCache = self.memory.planningCache,
            constructionSites = constructionSites,
            assignedSiteCounts = assignedSiteCounts
        )

        val targetSite = analysis.targetSite
            ?: return IntentResultType.COMPLETED

        val throughputSatisfied = assignedThroughput >= analysis.desiredThroughput
        val siteSaturated = analysis.targetSiteAssigned >= analysis.targetSiteCapacity
        val builderCapReached = assignedBuilders.size >= analysis.builderCountCap

        if (throughputSatisfied || siteSaturated || builderCapReached) {
            return IntentResultType.COMPLETED
        }

        if (assignIfAvailable(
                actorId = availableUnassignedCreepActorId { creep, assignment ->
                    creep.capabilities.canDoConstruction && assignment == null
                },
                assignment = CreepAssignment.Construction(
                    roomName = self.name,
                    constructionSiteId = targetSite.id
                )
            )
        ) {
            return IntentResultType.COMPLETED
        }

        val spawnableThroughput = ConstructionPlanningPolicy.spawnableThroughput(self, targetSite)
        if (spawnableThroughput <= 0) {
            return IntentResultType.RETAINED
        }

        val availableSpawnActorId = availableSpawnActorId()
            ?: return IntentResultType.RETAINED

        SpawnCommand.TrySpawnConstructionWorker(
            roomName = self.name,
            constructionSiteId = targetSite.id
        ).sendTo(availableSpawnActorId)

        return IntentResultType.COMPLETED
    }

    suspend fun ensureEnergyTransfer(): IntentResultType {
        val target = findEnergyTransferTarget()
            ?: return IntentResultType.DROPPED

        val alreadyAssigned = systemRequest(
            payload = SystemRequest.Query.creepsByAssignment<CreepAssignment.EnergyTransfer>(limit = 1)
            { _, assignment -> assignment.roomName == self.name && assignment.targetId == target.id }
        ).isNotEmpty()

        if (alreadyAssigned) {
            return IntentResultType.COMPLETED
        }

        if (assignIfAvailable(
                actorId = availableUnassignedCreepActorId { creep, assignment ->
                    creep.capabilities.canDoEnergyTransfer && assignment == null
                },
                assignment = CreepAssignment.EnergyTransfer(
                    roomName = self.name,
                    targetId = target.id,
                    goal = CreepAssignment.EnergyTransfer.Goal.UntilFull
                )
            )
        ) {
            return IntentResultType.COMPLETED
        }

        val spawnActorId = availableSpawnActorId()
            ?: return IntentResultType.RETAINED

        SpawnCommand.TrySpawnEnergyTransferWorker(
            roomName = self.name,
            targetId = target.id
        ).sendTo(spawnActorId)

        return IntentResultType.COMPLETED
    }

    private suspend fun availableUnassignedCreepActorId(
        predicate: (Creep, CreepAssignment?) -> Boolean
    ): String? = systemRequest(
        payload = SystemRequest.Query.Creeps(limit = 1, predicate = predicate)
    ).singleOrNull()?.actorId

    private fun assignIfAvailable(actorId: String?, assignment: CreepAssignment): Boolean {
        actorId ?: return false
        CreepCommand.Assign(assignment).sendTo(actorId)
        return true
    }

    private fun availableSpawnActorId(): String? =
        self.structures.my.spawns.firstOrNull { it.spawning == null }?.id

    private fun findEnergyTransferTarget(): Structure? =
        self.structures.my.run {
            extensions.firstOrNull { it.energyStore.isNotFull }
                ?: towers.firstOrNull { it.energyStore.isNotFull }
                ?: spawns.firstOrNull { it.energyStore.isNotFull }
        }
}
