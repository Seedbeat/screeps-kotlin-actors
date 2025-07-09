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
import store.isEmpty
import store.isFull
import store.isNonEmpty

fun <T> Creep.stagedSourceWorkerBase(
    sourceRoom: Room = this.room,
    targetRoom: Room = this.room,
    targetSearch: RoomContext.() -> T?,
    targetAction: RoomContext.(target: T) -> ScreepsReturnCode,
    preAction: (source: RoomContext, target: RoomContext) -> Boolean = { _, _ -> false }
) where T : HasPosition, T : Identifiable = when {

    sourceRoom.memory.stage >= RoomStage.Stage2
    -> stagedContainerWorkerBase(sourceRoom, targetRoom, targetSearch, targetAction, preAction)

    else
    -> sourceWorkerBase(sourceRoom, targetRoom, targetSearch, targetAction, preAction)
}

fun <T> Creep.sourceWorkerBase(
    sourceRoom: Room = this.room,
    targetRoom: Room = this.room,
    targetSearch: RoomContext.() -> T?,
    targetAction: RoomContext.(target: T) -> ScreepsReturnCode,
    preAction: (source: RoomContext, target: RoomContext) -> Boolean = { _, _ -> false }
) where T : HasPosition, T : Identifiable = workerBase(
    sourceRoom = sourceRoom,
    sourceSearch = { findClosestAvailableSource() },
    sourceAction = { source -> harvest(source) },
    targetRoom = targetRoom,
    targetSearch = targetSearch,
    targetAction = targetAction,
    preAction = preAction
)

fun <T> Creep.stagedContainerWorkerBase(
    sourceRoom: Room = this.room,
    targetRoom: Room = this.room,
    targetSearch: RoomContext.() -> T?,
    targetAction: RoomContext.(target: T) -> ScreepsReturnCode,
    preAction: (source: RoomContext, target: RoomContext) -> Boolean = { _, _ -> false }
) where T : HasPosition, T : Identifiable = when {

    sourceRoom.memory.stage >= RoomStage.Stage4 && sourceRoom.storage?.store?.isNonEmpty(RESOURCE_ENERGY) == true
    -> storageWorkerBase(sourceRoom, targetRoom, targetSearch, targetAction, preAction)

    else
    -> containerWorkerBase(sourceRoom, targetRoom, targetSearch, targetAction, preAction)
}

fun <T> Creep.storageWorkerBase(
    sourceRoom: Room = this.room,
    targetRoom: Room = this.room,
    targetSearch: RoomContext.() -> T?,
    targetAction: RoomContext.(target: T) -> ScreepsReturnCode,
    preAction: (source: RoomContext, target: RoomContext) -> Boolean = { _, _ -> false }
) where T : HasPosition, T : Identifiable = workerBase(
    sourceRoom = sourceRoom,
    sourceSearch = {
        room.storage?.let {
            if (it.store.getUsedCapacity(RESOURCE_ENERGY)!! > 100) it else {
                wait()
                null
            }
        }
    },
    sourceAction = { withdraw(it, RESOURCE_ENERGY) },
    targetRoom = targetRoom,
    targetSearch = targetSearch,
    targetAction = targetAction,
    preAction = preAction
)

fun <T> Creep.containerWorkerBase(
    sourceRoom: Room = this.room,
    targetRoom: Room = this.room,
    targetSearch: RoomContext.() -> T?,
    targetAction: RoomContext.(target: T) -> ScreepsReturnCode,
    preAction: (source: RoomContext, target: RoomContext) -> Boolean = { _, _ -> false }
) where T : HasPosition, T : Identifiable = workerBase(
    sourceRoom = sourceRoom,
    sourceSearch = { findClosestNonEmptyContainer(RESOURCE_ENERGY) },
    sourceAction = { withdraw(it, RESOURCE_ENERGY) },
    targetRoom = targetRoom,
    targetSearch = targetSearch,
    targetAction = targetAction,
    preAction = preAction
)

fun <S, T> Creep.workerBase(
    sourceRoom: Room = this.room,
    sourceSearch: RoomContext.() -> S?,
    sourceAction: RoomContext.(target: S) -> ScreepsReturnCode,
    targetRoom: Room = this.room,
    targetSearch: RoomContext.() -> T?,
    targetAction: RoomContext.(target: T) -> ScreepsReturnCode,
    preAction: (source: RoomContext, target: RoomContext) -> Boolean = { _, _ -> false }
) where S : HasPosition, S : Identifiable,
        T : HasPosition, T : Identifiable {

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
                say("SRC ERR!")
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
                ?.let { source -> doSourceWork(sourceContext, source, sourceAction) }
                ?: say("SRC LK ERR!")
                    .also { setUnassigned(sourceRoom) }
        }

        // just continue our action to empty tank
        !isEmpty && state == State.TARGET_WORK -> {
            log.debug("continue work at: ${memory.lockedObjectId}")

            getLockedResource<T>()
                ?.let { target -> doTargetWork(targetContext, target, targetAction) }
                ?: say("TGT LK ERR!")
                    .also { setUnassigned(targetRoom) }
        }

        // find and lock target
        !isEmpty || state == State.UNASSIGNED -> {
            log.debug("find and lock target")

            unlockResource(sourceRoom)
            targetSearch(targetContext)?.let { target ->
                lockResource(targetRoom, target.id)
                doTargetWork(targetContext, target, targetAction)
            } ?: say("TGT ERR!")
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

private fun <S : HasPosition> Creep.doSourceWork(
    sourceContext: RoomContext,
    source: S,
    sourceAction: RoomContext.(S) -> ScreepsReturnCode
) {
    state(State.SOURCE_WORK)
    val code = sourceContext.sourceAction(source)
    when {
        code == ERR_NOT_IN_RANGE -> moveToNavTarget(source)
        code == ERR_NOT_ENOUGH_RESOURCES -> setUnassigned(sourceContext.room)

        store.isFull(RESOURCE_ENERGY) ->
            setUnassigned(sourceContext.room)

        code != OK -> say("SRC: $code")
    }
}

private fun <T : HasPosition> Creep.doTargetWork(
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

        code != OK -> say("TGT: $code")
    }
}

fun Creep.setUnassigned(room: Room) {
    unlockResource(room)
    if (memory.role != Role.UPGRADER)
        memory.workObjectId = ""

    state(State.UNASSIGNED)
}