package creep.roles

import memory.workObjectId
import screeps.api.Creep
import screeps.api.Game
import screeps.api.structures.StructureController

fun Creep.upgrade() = stagedSourceWorkerBase(
    targetSearch = { Game.getObjectById<StructureController>(memory.workObjectId) },
    targetAction = { target -> upgradeController(target) },
    preAction = { _, _ -> renewCheck() }
)