package memory

import actor.KernelSnapshot
import actor.KernelSnapshotCodec
import screeps.api.GlobalMemory
import screeps.utils.memory.memory
import utils.rawMemory

var GlobalMemory.isUpdateNeed: Boolean by memory { false }
var GlobalMemory.actorKernelSnapshot: KernelSnapshot? by rawMemory(KernelSnapshotCodec)
