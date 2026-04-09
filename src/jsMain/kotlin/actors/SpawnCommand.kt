package actors

import actor.message.Command

sealed interface SpawnCommand : Command {
    data class TrySpawnWorker(
        val assignment: CreepAssignment,
        val profile: WorkerSpawnProfile
    ) : SpawnCommand
}
