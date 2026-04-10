package memory

import actors.RoomPlanningCache
import actors.memory.delegates.memoryEnum
import actors.memory.delegates.memoryNode
import actors.memory.delegates.memoryNodeObject
import actors.memory.delegates.memoryValue
import actors.memory.types.RoomPlanningCacheMemory
import actors.memory.types.Semaphore
import room.RoomStage
import screeps.api.RoomMemory
import utils.ResourceLockOwners

var RoomMemory.stage: RoomStage by memoryEnum { RoomStage.Uninitialized }
val RoomMemory.resourceSemaphore: Semaphore by memoryNode(::Semaphore)
var RoomMemory.resourceLockOwners: ResourceLockOwners by memoryValue { ResourceLockOwners() }
var RoomMemory.planningCache: RoomPlanningCache? by memoryNodeObject(::RoomPlanningCacheMemory)
