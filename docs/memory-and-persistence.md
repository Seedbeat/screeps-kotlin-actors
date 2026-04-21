# Memory And Persistence

Last source scan: 2026-04-21.

This document describes Screeps Memory usage and reset behavior.

## Persistence Model

Persistence is partial.

What persists:

- explicit Screeps Memory fields
- room planning cache
- room resource semaphores and lock owners
- creep home room, assignment, and locked resource id
- kernel snapshot bookkeeping

What does not persist:

- actor instances
- actor-local fields
- actor mailboxes
- sleeping `receive()` continuations
- `nextTick()` continuations
- pending request/response continuations
- scheduled continuations

On reset, live actors are reconstructed from Screeps game state by child managers after `SystemActor` is spawned.

## Global Memory

Defined in `memory/GlobalMemoryExtensions.kt`.

Fields:

- `Memory.isUpdateNeed: Boolean`
- `Memory.actorKernelSnapshot: KernelSnapshot?`
- `Memory.settings: MutableRecord<String, Any>` from `Settings.kt`

`actorKernelSnapshot` is written by `ActorSystem.tick()` after scheduler work. It stores actor ids and actor type names.

Current restore does not rehydrate actor instances. The snapshot is runtime bookkeeping and diagnostics, not full actor
persistence.

## Kernel Snapshot Schema

`KernelSnapshot`:

- `time: Int`
- `actors: List<ActorSnapshot>`

`ActorSnapshot`:

- `id: String`
- `type: String`

`KernelSnapshotCodec` serializes to plain JS objects and deserializes defensively. Invalid snapshot data becomes an
empty `KernelSnapshot`.

## Creep Memory

Defined in `memory/CreepMemoryExtensions.kt`.

Fields:

- `homeRoom: String`
- `lockedObjectId: String?`
- `assignment: CreepAssignment?`

`homeRoom` defaults to an empty string when missing.

`lockedObjectId` stores the resource id currently locked by the creep through room resource arbitration.

`assignment` uses `CreepAssignmentMemory`, a sealed memory node.

## Assignment Memory

Assignments use an explicit sealed schema:

- `_type`
- variant fields

Assignment variants:

- `ControllerUpkeep`: `roomName`, `controllerId`, `phase`
- `ControllerProgress`: `roomName`, `controllerId`, `phase`
- `Construction`: `roomName`, `constructionSiteId`, `phase`
- `EnergyTransfer`: `roomName`, `targetId`, `goal`, `phase`

Energy transfer goals:

- `UntilFull`: discriminator only
- `Amount`: `amount`
- `Percent`: `percent`

If required fields are missing, the memory node reads `null`.

## Room Memory

Defined in `memory/RoomMemoryExtensions.kt`.

Fields:

- `stage: RoomStage`
- `resourceSemaphore: Semaphore`
- `resourceLockOwners: ResourceLockOwners`
- `planningCache: RoomPlanningCache?`

`stage` defaults to `RoomStage.Uninitialized`.

`resourceSemaphore` is a `MemoryMap<SemaphoreValue>`.

`resourceLockOwners` is a mutable JS record mapping owner actor id to resource id.

`planningCache` uses `RoomPlanningCacheMemory`, an explicit object memory node.

## Semaphore Memory

`SemaphoreValue` fields:

- `current: Int`
- `maximum: Int`

Operations:

- `create(id, initialCount, maximumCount)`
- `recreate(id, initialCount, maximumCount)`
- `tryAcquire(id): Boolean?`
- `tryRelease(id): Boolean?`
- `isAvailable(id): Boolean?`
- `available(id): Int?`
- `current(id): Int?`
- `maximum(id): Int?`

Return values:

- `true`: operation succeeded
- `false`: entry exists but cannot acquire/release
- `null`: entry does not exist

## Planning Cache Memory

`RoomPlanningCacheMemory` mirrors `RoomPlanningCache` field by field.

If any required field is missing, the cache reads as `null`. Writers store every field. Clearing deletes every field.

The cache is recomputed by `RoomActor` every 4 ticks.

## Spawn, Flag, And Power Creep Memory

These extension files currently contain placeholder `test` fields:

- `SpawnMemoryExtensions.kt`
- `FlagMemoryExtensions.kt`
- `PowerCreepMemoryExtensions.kt`

They are not part of the current actor behavior.

## Memory Delegates

The memory layer provides typed property delegates:

- `memoryValue`
- `memoryEnum`
- `memoryObject`
- `memoryNode`
- `memoryNodeObject`
- `memoryNodeValue`
- `memoryNodeEnum`

Raw fields use `MemoryRawIO`.

Nested `MemoryNode` fields use `MemoryNodeIO`.

`RawCodec` passes JS values through as-is. `EnumCodec` stores enum names.

## Reset Behavior

On VM reset:

1. `Root.wasReset` is true.
2. `Root.onReset()` calls `Actors.init()`.
3. `Actors.init()` spawns `SYSTEM` and queues Bootstrap.
4. The normal tick queues Tick and enters `ActorSystem.tick()`.
5. `ActorSystem.tick()` calls `ActorKernel.restore()` if `Memory.actorKernelSnapshot` exists.
6. Current restore logs a warning and does not create actors.
7. `SystemActor` syncs children from live `Game` state.
8. `RoomActor` syncs spawns and room state.
9. `CreepActor` resumes from memory-backed assignments.

Implication: any behavior that must survive reset must be in Screeps Memory or reconstructible from live Game state.

## Adding Persistent State

When adding persistent state:

1. Choose the owner memory object: global, room, creep, spawn, or a nested node.
2. Add a typed extension or memory node.
3. Define default behavior for missing fields.
4. Define read behavior for partial/corrupt data.
5. Define clear behavior if the object disappears.
6. Document reset behavior.
7. Avoid compatibility branches for old shapes unless the active runtime reads them.

Do not assume `ActorKernel.snapshot()` is enough for new persistent actor behavior.

## Credential Safety

`gradle.properties` is ignored by `.gitignore` and may contain Screeps credentials. Documentation should name supported
properties but must not quote local values.
