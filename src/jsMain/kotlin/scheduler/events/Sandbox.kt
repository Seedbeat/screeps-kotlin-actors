package scheduler.events

import scheduler.IEvent
import screeps.api.RESOURCE_ENERGY
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

object Sandbox : IEvent, ILogging by Logging<Sandbox>(LogLevel.DEBUG) {
    override val isEnabled get() = false

    override fun execute() {
        with(Root.room("E53S37")) {
            sourcesContainers.forEach {
                this@Sandbox.log.info(it.id, it.store.getUsedCapacity(RESOURCE_ENERGY))
            }
        }
    }
}