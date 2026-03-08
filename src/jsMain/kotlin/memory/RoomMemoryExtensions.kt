package memory

import room.RoomStage
import screeps.api.RoomMemory
import screeps.utils.memory.memory
import utils.ResourceLockOwners
import utils.Semaphore

var RoomMemory.resourceSemaphore: Semaphore by memory { Semaphore() }
var RoomMemory.resourceLockOwners: ResourceLockOwners by memory { ResourceLockOwners() }
var RoomMemory.stage by memory(RoomStage.Uninitialized)

// TODO
//var RoomMemory.missions: Array<String> by memory { arrayOf() }