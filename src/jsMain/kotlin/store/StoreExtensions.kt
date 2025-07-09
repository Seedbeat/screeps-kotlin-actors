package store

import screeps.api.*
import kotlin.math.roundToInt

fun Store.isFull(resource: ResourceConstant) = getFreeCapacity(resource) == 0
fun Store.isEmpty(resource: ResourceConstant) = getUsedCapacity(resource) == 0
fun Store.isNonEmpty(resource: ResourceConstant) = !isEmpty(resource)
fun Store.isNonFilled(resource: ResourceConstant) = !isFull(resource)
fun Store.fillPercentage(resource: ResourceConstant): Int {
    val used = getUsedCapacity(resource)?.toFloat() ?: return 0
    val capacity = getCapacity(resource)?.toFloat() ?: return 0

    return ((used / capacity) * 100f).roundToInt()
}

fun <T : StoreOwner> Iterable<T>.firstNonEmptyOrNull(resource: ResourceConstant): T? =
    firstOrNull { it.store.isNonEmpty(resource) }

fun <T : StoreOwner> Array<T>.firstNonEmptyOrNull(resource: ResourceConstant): T? =
    firstOrNull { it.store.isNonEmpty(resource) }


fun <T : StoreOwner> Iterable<T>.firstNonFilledOrNull(resource: ResourceConstant): T? =
    firstOrNull { it.store.isNonFilled(resource) }

fun <T : StoreOwner> Array<T>.firstNonFilledOrNull(resource: ResourceConstant): T? =
    firstOrNull { it.store.isNonFilled(resource) }