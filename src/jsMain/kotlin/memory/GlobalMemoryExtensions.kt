package memory

import actor.KernelSnapshot
import actor.KernelSnapshotCodec
import memory.delegates.memoryObject
import memory.delegates.memoryValue
import screeps.api.GlobalMemory

var GlobalMemory.isUpdateNeed: Boolean by memoryValue { false }
var GlobalMemory.actorKernelSnapshot: KernelSnapshot? by memoryObject(KernelSnapshotCodec)
