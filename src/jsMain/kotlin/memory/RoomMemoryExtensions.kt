package memory

import memory.delegates.memoryEnum
import memory.delegates.memoryNode
import memory.delegates.memoryNodeObject
import memory.delegates.memoryValue
import memory.types.RoomPlanningCacheMemory
import memory.types.Semaphore
import room.enums.RoomStage
import room.planning.RoomPlanningCache
import screeps.api.RoomMemory
import utils.ResourceLockOwners

var RoomMemory.stage: RoomStage by memoryEnum { RoomStage.Uninitialized }
val RoomMemory.resourceSemaphore: Semaphore by memoryNode(::Semaphore)
var RoomMemory.resourceLockOwners: ResourceLockOwners by memoryValue { ResourceLockOwners() }
var RoomMemory.planningCache: RoomPlanningCache? by memoryNodeObject(::RoomPlanningCacheMemory)
