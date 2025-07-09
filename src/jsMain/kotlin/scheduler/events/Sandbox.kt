package scheduler.events

import scheduler.IEvent
import store.globalResourcePlan
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

object Sandbox : IEvent, ILogging by Logging<Sandbox>(LogLevel.DEBUG) {
    override val isEnabled get() = false

    override fun execute() {
//        with(Root.room("E53S37")) {
//            sourcesContainers.forEach {
//                this@Sandbox.log.info(it.id, it.store.getUsedCapacity(RESOURCE_ENERGY))
//            }
//        }

        globalResourcePlan.entries.forEach { (ownerId, plan) ->
            log.info(ownerId, "ownerPlan:", plan.count())

            plan.entries.forEach { (resource, resourcePlan) ->
                log.info(resource, "resourcePlan:", resourcePlan.count())

//                globalResourcePlan[ownerId][resource][ownerId] = Int.MAX_VALUE
//                globalResourcePlan[ownerId][resource]["TEST"] = Int.MAX_VALUE
//                globalResourcePlan[ownerId][resource]["TEST"] = Int.MAX_VALUE

                resourcePlan.entries.forEach { (customerId, amount) ->
                    log.info(customerId, "customerPlan:", amount)
                }

            }
        }

    }
}