package creep.roles

import Root
import creep.*
import creep.enums.Role
import creep.enums.State
import memory.lockedObjectId
import memory.role
import memory.stage
import memory.workObjectId
import room.RoomContext
import room.RoomStage
import screeps.api.*
import store.*

fun <T> Creep.stagedSourceWorkerBase(
    sourceRoom: Room = this.room,
    targetRoom: Room = this.room,
    targetSearch: RoomContext.() -> T?,
    targetCheck: RoomContext.(target: T) -> Boolean = { _ -> true },
    targetAction: RoomContext.(target: T) -> ScreepsReturnCode,
    preAction: (source: RoomContext, target: RoomContext) -> Boolean = { _, _ -> false }
) where T : NavigationTarget, T : Identifiable = when {

    sourceRoom.memory.stage >= RoomStage.Stage2
    -> stagedContainerWorkerBase(sourceRoom, targetRoom, targetSearch, targetCheck, targetAction, preAction)

    else
    -> sourceWorkerBase(sourceRoom, targetRoom, targetSearch, targetCheck, targetAction, preAction)
}

fun <T> Creep.sourceWorkerBase(
    sourceRoom: Room = this.room,
    targetRoom: Room = this.room,
    targetSearch: RoomContext.() -> T?,
    targetCheck: RoomContext.(target: T) -> Boolean = { _ -> true },
    targetAction: RoomContext.(target: T) -> ScreepsReturnCode,
    preAction: (source: RoomContext, target: RoomContext) -> Boolean = { _, _ -> false }
) where T : NavigationTarget, T : Identifiable = workerBase(
    sourceRoom = sourceRoom,
    sourceSearch = { findClosestAvailableSource() },
    sourceAction = { source -> harvest(source) },
    targetRoom = targetRoom,
    targetSearch = targetSearch,
    targetCheck = targetCheck,
    targetAction = targetAction,
    preAction = preAction
)

fun <T> Creep.stagedContainerWorkerBase(
    sourceRoom: Room = this.room,
    targetRoom: Room = this.room,
    targetSearch: RoomContext.() -> T?,
    targetCheck: RoomContext.(target: T) -> Boolean = { _ -> true },
    targetAction: RoomContext.(target: T) -> ScreepsReturnCode,
    preAction: (source: RoomContext, target: RoomContext) -> Boolean = { _, _ -> false }
) where T : NavigationTarget, T : Identifiable = when {

    sourceRoom.memory.stage >= RoomStage.Stage4 && sourceRoom.storage?.store?.isNonEmpty(RESOURCE_ENERGY) == true
    -> storageWorkerBase(sourceRoom, targetRoom, targetSearch, targetAction, preAction)

    else
    -> containerWorkerBase(sourceRoom, targetRoom, targetSearch, targetCheck, targetAction, preAction)
}

fun <T> Creep.storageWorkerBase(
    sourceRoom: Room = this.room,
    targetRoom: Room = this.room,
    targetSearch: RoomContext.() -> T?,
    targetAction: RoomContext.(target: T) -> ScreepsReturnCode,
    preAction: (source: RoomContext, target: RoomContext) -> Boolean = { _, _ -> false }
) where T : NavigationTarget, T : Identifiable = workerBase(
    sourceRoom = sourceRoom,
    sourceSearch = {
        room.storage?.let { storage ->
            if (storage.store.getUsedCapacity(RESOURCE_ENERGY)!! > 100) {

                storage.setPlannedAmount(this@storageWorkerBase, RESOURCE_ENERGY, -(store.getFreeCapacity(RESOURCE_ENERGY) ?: 0))
//                storage.decPlannedAmount(this@storageWorkerBase, RESOURCE_ENERGY)
                storage
            } else {
                wait()
                null
            }
        }
    },
    sourceAction = { storage ->
        val code = withdraw(storage, RESOURCE_ENERGY)

        storage.remPlannedAmount(this@storageWorkerBase, RESOURCE_ENERGY)

        code
    },
    targetRoom = targetRoom,
    targetSearch = targetSearch,
    targetAction = targetAction,
    preAction = preAction
)

fun <T> Creep.containerWorkerBase(
    sourceRoom: Room = this.room,
    targetRoom: Room = this.room,
    targetSearch: RoomContext.() -> T?,
    targetCheck: RoomContext.(target: T) -> Boolean = { _ -> true },
    targetAction: RoomContext.(target: T) -> ScreepsReturnCode,
    preAction: (source: RoomContext, target: RoomContext) -> Boolean = { _, _ -> false }
) where T : NavigationTarget, T : Identifiable = workerBase(
    sourceRoom = sourceRoom,
    sourceSearch = {
        val container = findClosestNonEmptyContainer(RESOURCE_ENERGY)

        container?.decPlannedAmount(this@containerWorkerBase, RESOURCE_ENERGY)

        container
    },
    sourceCheck = { it.store.getUsedCapacity(RESOURCE_ENERGY) >= 50 },
    sourceAction = { container ->
        val code = withdraw(container, RESOURCE_ENERGY)

        container.remPlannedAmount(this@containerWorkerBase, RESOURCE_ENERGY)

        code
    },
    targetRoom = targetRoom,
    targetSearch = targetSearch,
    targetCheck = targetCheck,
    targetAction = targetAction,
    preAction = preAction
)

fun <S, T> Creep.workerBase(
    sourceRoom: Room = this.room,
    sourceSearch: RoomContext.() -> S?,
    sourceCheck: RoomContext.(source: S) -> Boolean = { _ -> true },
    sourceAction: RoomContext.(source: S) -> ScreepsReturnCode,
    targetRoom: Room = this.room,
    targetSearch: RoomContext.() -> T?,
    targetCheck: RoomContext.(target: T) -> Boolean = { _ -> true },
    targetAction: RoomContext.(target: T) -> ScreepsReturnCode,
    preAction: (source: RoomContext, target: RoomContext) -> Boolean = { _, _ -> false }
) where S : NavigationTarget, S : Identifiable,
        T : NavigationTarget, T : Identifiable {

    val sourceContext by lazy { Root.room(sourceRoom.name) }
    val targetContext by lazy { Root.room(targetRoom.name) }

    if (preAction(sourceContext, targetContext))
        return

    val isFull = store.isFull(RESOURCE_ENERGY)
    val isEmpty = store.isEmpty(RESOURCE_ENERGY)
    val state = state

    when {
        // find and lock resource to harvest
        isEmpty && (!isResourceLocked || state == State.UNASSIGNED) -> {
            log.debug("find and lock resource to harvest")

            unlockResource(targetRoom)
            val source = sourceSearch(sourceContext)
            if (source == null) {
                say("➡️⛔")
                state(State.UNASSIGNED)
                return
            }
            lockResource(sourceRoom, source.id)
            doSourceWork(sourceContext, source, sourceAction)
        }

        // continue harvest
        !isFull && state == State.SOURCE_WORK -> {
            log.debug("continue harvest at: ${memory.lockedObjectId}")

            getLockedResource<S>()
                ?.takeIf { source -> sourceCheck(sourceContext, source) }
                ?.let { source -> doSourceWork(sourceContext, source, sourceAction) }
                ?: say("➡️🔒⛔")
                    .also { setUnassigned(sourceRoom) }
        }

        // just continue our action to empty tank
        !isEmpty && state == State.TARGET_WORK -> {
            log.debug("continue work at: ${memory.lockedObjectId}")

            getLockedResource<T>()
                ?.takeIf { target -> targetCheck(targetContext, target) }
                ?.let { target -> doTargetWork(targetContext, target, targetAction) }
                ?: say("⛔🔒⬅️")
                    .also { setUnassigned(targetRoom) }
        }

        // find and lock target
        !isEmpty || state == State.UNASSIGNED -> {
            log.debug("find and lock target")

            unlockResource(sourceRoom)
            targetSearch(targetContext)?.let { target ->
                lockResource(targetRoom, target.id)
                doTargetWork(targetContext, target, targetAction)
            } ?: say("⛔⬅️")
                .also { wait() }
        }

        else -> {
            unlockResource(targetRoom)
            unlockResource(sourceRoom)
            state(State.UNASSIGNED)
            wait()
        }

    }

}

private fun <S : NavigationTarget> Creep.doSourceWork(
    sourceContext: RoomContext,
    source: S,
    sourceAction: RoomContext.(S) -> ScreepsReturnCode
) {
    state(State.SOURCE_WORK)
    val code = sourceContext.sourceAction(source)
    when {
        code == ERR_NOT_IN_RANGE -> moveToNavTarget(source)
        code == ERR_INVALID_TARGET -> setUnassigned(sourceContext.room)
        code == ERR_NOT_ENOUGH_RESOURCES -> setUnassigned(sourceContext.room)

        store.isFull(RESOURCE_ENERGY) ->
            setUnassigned(sourceContext.room)

        code != OK -> say("➡️ $code")
    }
}

private fun <T : NavigationTarget> Creep.doTargetWork(
    targetContext: RoomContext,
    target: T,
    targetAction: RoomContext.(T) -> ScreepsReturnCode
) {
    state(State.TARGET_WORK)
    val code = targetContext.targetAction(target)

    when {
        code == ERR_NOT_IN_RANGE -> moveToNavTarget(target)

        code == ERR_FULL || store.isEmpty(RESOURCE_ENERGY) ->
            setUnassigned(targetContext.room)

        code != OK -> say("⬅️ $code")
    }
}

fun Creep.setUnassigned(room: Room) {
    unlockResource(room)
    if (memory.role != Role.UPGRADER)
        memory.workObjectId = ""

    state(State.UNASSIGNED)
}