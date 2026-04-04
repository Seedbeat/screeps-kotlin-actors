package actors.base

import actor.message.Command

interface Intent : Command {
    val intentId: String
    val priority: IntentPriority
    val repeatable: Boolean
}