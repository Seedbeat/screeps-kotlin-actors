package memory

import actors.memory.delegates.memoryEnum
import actors.memory.delegates.memoryValue
import room.RoomStage
import screeps.api.RoomMemory
import utils.ResourceLockOwners
import utils.Semaphore

var RoomMemory.resourceSemaphore: Semaphore by memoryValue { Semaphore() }
var RoomMemory.resourceLockOwners: ResourceLockOwners by memoryValue { ResourceLockOwners() }
var RoomMemory.stage: RoomStage by memoryEnum { RoomStage.Uninitialized }

// TODO
//var RoomMemory.missions: Array<String> by memory { arrayOf() }
