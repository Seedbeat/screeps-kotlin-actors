package actors.heuristics

import room.RoomStage

object RoomHeuristics {
    fun targetBuildTicks(stage: RoomStage): Int = when (stage) {
        RoomStage.Uninitialized,
        RoomStage.Stage1 -> 1800

        RoomStage.Stage2,
        RoomStage.Stage3 -> 1200

        RoomStage.Stage4,
        RoomStage.Stage5 -> 800

        RoomStage.Stage6,
        RoomStage.Stage7 -> 500

        RoomStage.Stage8,
        RoomStage.StageMax -> 300
    }

    fun maxBuildersCap(stage: RoomStage): Int = when (stage) {
        RoomStage.Uninitialized,
        RoomStage.Stage1 -> 1

        RoomStage.Stage2,
        RoomStage.Stage3 -> 2

        RoomStage.Stage4,
        RoomStage.Stage5 -> 3

        RoomStage.Stage6,
        RoomStage.Stage7,
        RoomStage.Stage8,
        RoomStage.StageMax -> 4
    }
}