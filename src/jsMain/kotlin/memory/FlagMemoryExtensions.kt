package memory

import actors.memory.delegates.memoryValue
import screeps.api.FlagMemory

var FlagMemory.test: Int by memoryValue { 0 }
