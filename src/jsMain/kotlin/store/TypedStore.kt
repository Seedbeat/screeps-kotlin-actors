package store

import screeps.api.ResourceConstant
import screeps.api.Store

class TypedStore(store: Store, resource: ResourceConstant) {
    val free: Int = store.getFreeCapacity(resource) ?: 0
    val used: Int = store.getUsedCapacity(resource) ?: 0
    val capacity: Int = store.getCapacity(resource) ?: 0

    val percentage: Int = if (capacity == 0) 0 else (used * 100) / capacity

    val isEmpty: Boolean = used == 0
    val isNotEmpty: Boolean = !isEmpty
    val isFull: Boolean = free == 0
    val isNotFull: Boolean = !isFull
}