package memory

import actor.KernelSnapshot
import actor.KernelSnapshotCodec
import actors.memory.delegates.memoryObject
import actors.memory.delegates.memoryValue
import screeps.api.GlobalMemory

var GlobalMemory.isUpdateNeed: Boolean by memoryValue { false }
var GlobalMemory.actorKernelSnapshot: KernelSnapshot? by memoryObject(KernelSnapshotCodec)
