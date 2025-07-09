package scheduler.missions

import room.RoomStage
import scheduler.IRoomMission
import room.RoomContext
import memory.stage
import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

object RoomStageAnalyzer : IRoomMission<RoomStage>, ILogging by Logging<RoomStageAnalyzer>(LogLevel.INFO) {
    // RCL | Available structures
    //  0  | Roads, 5 Containers
    //  1  | Roads, 5 Containers, 1 Spawn
    //  2  | Roads, 5 Containers, 1 Spawn, 5 Extensions, Ramparts, Walls
    //  3  | Roads, 5 Containers, 1 Spawn, 10 Extensions, Ramparts, Walls, 1 Tower
    //  4  | Roads, 5 Containers, 1 Spawn, 20 Extensions, Ramparts, Walls, 1 Tower, Storage
    //  5  | Roads, 5 Containers, 1 Spawn, 30 Extensions, Ramparts, Walls, 2 Towers, Storage, 2 Links
    //  6  | Roads, 5 Containers, 1 Spawn, 40 Extensions, Ramparts, Walls, 2 Towers, Storage, 3 Links, Extractor, 3 Labs, Terminal
    //  7  | Roads, 5 Containers, 2 Spawns, 50 Extensions, Ramparts, Walls, 3 Towers, Storage, 4 Links, Extractor, 6 Labs, Terminal, Factory
    //  8  | Roads, 5 Containers, 3 Spawns, 60 Extensions, Ramparts, Walls, 6 Towers, Storage, 6 Links, Extractor, 10 Labs, Terminal, Factory, Observer, Power Spawn, Nuker

    // Stage:
    //  0 -> target: RCL to lvl 1
    //  1 -> target: RCL to lvl 2, build 5 extensions, build container
    //  2 -> target: RCL to lvl 3, build 10 extensions, build roads
    //  3 -> target: RCL to lvl 4, build 20 extensions, build tower
    //  4 -> target: RCL to lvl 5, build 20 extensions, build storage

    override fun RoomContext.execute(): RoomStage {
        val controllerLevel = room.controller?.level ?: 0

        val stage = when {
            controllerLevel >= 4 && extensions.count() >= 20 && isSourcesCarriedByContainers && isStorageExist
            -> RoomStage.Stage4

            controllerLevel >= 3 && extensions.count() >= 10 && isSourcesCarriedByContainers
            -> RoomStage.Stage3

            controllerLevel >= 2 && extensions.count() >= 5 && isSourcesCarriedByContainers
            -> RoomStage.Stage2

            else -> RoomStage.Stage1
        }

        room.memory.stage = stage
        return stage
    }
}