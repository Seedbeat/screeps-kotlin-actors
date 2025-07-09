package scheduler.events

import Settings
import scheduler.IEvent
import utils.log.ILogging
import utils.log.Logging

object UpdateSettings: IEvent, ILogging by Logging<UpdateSettings>() {
    override val isEnabled get() = false

    private val values = arrayOf(
        Settings.LoggingLevel,
        Settings.ShowCreepPath,
        Settings.ShowStatistics
    )

    override fun execute() {
        values.forEach {
            if (!it.exist())
                it.setDefault()
        }
    }
}