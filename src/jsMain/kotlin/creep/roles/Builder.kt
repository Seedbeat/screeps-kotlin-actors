package creep.roles

import screeps.api.Creep

fun Creep.build() = stagedSourceWorkerBase(
    targetSearch = { pos.findClosestByPath(constructionSites) },
    targetAction = { target -> build(target) },
    preAction = { _, target -> if (target.constructionSites.isNotEmpty()) renewCheck() else false }
)