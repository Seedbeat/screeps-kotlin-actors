package store

import screeps.api.RESOURCE_ENERGY
import screeps.api.ResourceConstant
import screeps.api.StoreOwner
import utils.cache.CachedInstance

private val typedStoreCache = CachedInstance<TypedStore>()

fun StoreOwner.storeOf(resource: ResourceConstant): TypedStore =
    typedStoreCache.getOrPut(this.id, resource) { TypedStore(this.store, resource) }

val StoreOwner.energyStore: TypedStore get() = storeOf(RESOURCE_ENERGY)
