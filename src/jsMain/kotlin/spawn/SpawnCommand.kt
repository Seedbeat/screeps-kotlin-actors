package spawn

import actor.message.Command
import creep.CreepAssignment
import room.planning.WorkerSpawnProfile

sealed interface SpawnCommand : Command {
    data class TrySpawnWorker(
        val assignment: CreepAssignment,
        val profile: WorkerSpawnProfile
    ) : SpawnCommand
}
