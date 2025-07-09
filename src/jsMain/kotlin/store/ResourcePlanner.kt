package store

import screeps.api.*
import utils.collections.LinkedHashMapWithDefault
import kotlin.math.max
import kotlin.math.min

val globalResourcePlan = GlobalResourcePlan()

class GlobalResourcePlan : LinkedHashMapWithDefault<String, ResourcePlan>({ ResourcePlan() })

class ResourcePlan : LinkedHashMapWithDefault<ResourceConstant, SingleResourcePlan>({ SingleResourcePlan() })

class SingleResourcePlan : LinkedHashMapWithDefault<String, Int>({ 0 }) {

    fun inc(key: String, amount: Int) {
        Root.log.info("inc:", key, amount)
        set(key, get(key) + amount)
    }

    fun dec(key: String, amount: Int) {
        Root.log.info("dec:", key, amount)
        set(key, get(key) - amount)
    }

    val total get() = values.sum()
}

//val <T : StoreOwner> T.resourcePlan: ResourcePlan
//    get() = globalResourcePlan[id]


fun <T : StoreOwner> T.getPlannedAmount(resource: ResourceConstant): Int {
    return globalResourcePlan[id][resource].total
}

fun <T : StoreOwner> T.getUsedCapacityAnalyzed(resource: ResourceConstant): Int {
    return (store.getUsedCapacity(resource) ?: 0) + getPlannedAmount(resource)
}

fun <T : StoreOwner> T.getFreeCapacityAnalyzed(resource: ResourceConstant): Int {
    val capacity = (store.getCapacity(resource) ?: 0)
    val used = getUsedCapacityAnalyzed(resource)
    val available = capacity - used

    return available
}

fun <T : StoreOwner> T.setPlannedAmount(customer: Identifiable, resource: ResourceConstant, amount: Int) {
    Root.log.info("[BEFORE]", globalResourcePlan[id][resource].count())
    Root.log.info("[SET]", id, resource, customer.id, amount)
    globalResourcePlan[id][resource][customer.id] = amount
    Root.log.info("[AFTER]", globalResourcePlan[id][resource].count())
}

fun <T : StoreOwner> T.incPlannedAmount(customer: Identifiable, resource: ResourceConstant, amount: Int) {
    globalResourcePlan[id][resource].inc(customer.id, amount)
}

fun <T : StoreOwner> T.decPlannedAmount(customer: Identifiable, resource: ResourceConstant, amount: Int) {
    globalResourcePlan[id][resource].dec(customer.id, amount)
}

fun <T : StoreOwner> T.remPlannedAmount(customer: Identifiable, resource: ResourceConstant) {
    globalResourcePlan[id][resource].remove(customer.id)
}

fun <T : StoreOwner, R> T.incPlannedAmount(
    customer: R,
    resource: ResourceConstant
) where R : StoreOwner, R : Identifiable {
    val available = max(getFreeCapacityAnalyzed(resource), 0)
    val future = customer.store.getUsedCapacity(resource) ?: 0
    val planned = min(available, future)

    setPlannedAmount(customer, resource, planned)
}

fun <T : StoreOwner, R> T.decPlannedAmount(
    customer: R,
    resource: ResourceConstant
) where R : StoreOwner, R : Identifiable {
    val available = min(getUsedCapacityAnalyzed(resource), (store.getCapacity(resource) ?: 0))
    val future = customer.store.getFreeCapacity(resource) ?: 0
    val planned = min(available, future)

    setPlannedAmount(customer, resource, planned)
}