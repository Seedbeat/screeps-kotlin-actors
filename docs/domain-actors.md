# Domain Actors And Protocols

Last source scan: 2026-04-21.

This document describes actor ownership and message protocols in the domain layer.

## Ownership Tree

```text
SYSTEM: SystemActor
  RoomActor(room.name) for every owned room
    SpawnActor(spawn.id) for every owned spawn in that room
  CreepActor(creep.name) for every creep in Game.creeps
```

Ownership is managed by child managers in `actors/base/ChildrenManager.kt`.

Managers:

- `OwnedRoomsManager`: desired ids are owned room names from `Game.rooms.values`.
- `OwnedCreepsManager`: desired ids are creep names from `Game.creeps.keys`.
- `RoomSpawnsManager`: desired ids are spawn ids from `room.find(FIND_MY_SPAWNS)`.

`syncChildren()`:

1. marks already-existing desired actors as owned
2. spawns desired actors that do not exist
3. removes previously owned actors that are no longer desired

## SystemActor

File: `src/jsMain/kotlin/actors/SystemActor.kt`

Actor id: `SYSTEM`.

Binding: `NoBinding`.

Responsibilities:

- own room actors
- own creep actors
- broadcast lifecycle messages to children
- answer global creep queries
- delete stale creep memory for names no longer present in `Game.creeps`

Lifecycle:

- `Bootstrap`: sync children, clean stale creep memory, broadcast Bootstrap.
- `Tick`: sync children, clean stale creep memory, broadcast Tick.

Requests:

- `SystemRequest.Query.Creeps`
- `SystemRequest.Query.CreepsByAssignment`

Responses:

- `SystemResponse.Query.CreepsResponse`

The query result type is `List<CreepStatus>`, where each status includes actor id, home room, current room, assignment,
capabilities, and locked resource id.

## RoomActor

File: `src/jsMain/kotlin/room/RoomActor.kt`

Actor id: room name.

Binding: `GameRoomBinding(id)`.

Responsibilities:

- own spawn actors for the room
- scan children
- reconcile orphaned room resource locks
- sync room stage
- sync planning cache
- sync room resource semaphores
- run room intents
- answer room resource acquire/release requests
- broadcast lifecycle messages to spawn actors after room-side work

Lifecycle on Bootstrap:

1. `RoomCommand.Scan`
2. `RoomCommand.SyncStage`
3. `RoomCommand.SyncPlanningCache`
4. `RoomCommand.SyncSemaphores`
5. add recurring room intents
6. broadcast Bootstrap to spawns

Lifecycle on Tick:

1. `RoomCommand.Scan`
2. sync stage every 2 ticks
3. sync planning cache every 4 ticks
4. sync semaphores every 3 ticks
5. process intents
6. broadcast Tick to spawns

Commands:

- `RoomCommand.Scan`
- `RoomCommand.SyncStage`
- `RoomCommand.SyncPlanningCache`
- `RoomCommand.SyncSemaphores`
- `RoomIntent.EnsureControllerSurvival`
- `RoomIntent.PlanWorkforce`

Requests:

- `RoomRequest.TryAcquireResourceById`
- `RoomRequest.TryAcquireResourceByType`
- `RoomRequest.ReleaseResourceById`

Responses:

- `RoomResponse.TryAcquireResource`
- `RoomResponse.TryAcquireAnyResource`
- `RoomResponse.ReleaseResource`

## SpawnActor

File: `src/jsMain/kotlin/spawn/SpawnActor.kt`

Actor id: `StructureSpawn.id`.

Binding: `GameObjectBinding<StructureSpawn>(id)`.

Responsibilities:

- execute spawn-side commands
- call `Spawner.spawn()` for worker creation

Commands:

- `SpawnCommand.TrySpawnWorker(assignment, profile)`

Current request state:

- `SpawnRequest<T>` exists as a sealed marker protocol.
- `SpawnActor.processRequest()` is `TODO()`.
- Do not send requests to `SpawnActor` until a handler exists.

`SpawnActor` should stay execution-oriented. Room-wide population policy belongs in `RoomActor` and `RoomIntentService`.

## CreepActor

File: `src/jsMain/kotlin/creep/CreepActor.kt`

Actor id: creep name.

Binding: `GameCreepBinding(id)`.

Responsibilities:

- execute creep assignment each Tick
- assign or clear memory-backed assignments
- set locked resource id memory
- clean assignment and lock memory on actor destruction

Lifecycle:

- `Bootstrap`: currently no-op.
- `Tick`: `CreepAssignmentService.executeAssignment()`.

Commands:

- `CreepCommand.Assign(assignment)`
- `CreepCommand.ClearAssignment`
- `CreepCommand.SetLockedResourceId(resourceId)`

Current request state:

- `CreepRequest<T>` exists as a sealed marker protocol.
- `CreepActor.processRequest()` is `TODO()`.
- Do not send requests to `CreepActor` until a handler exists.

## Protocol Inventory

Lifecycle:

- `Lifecycle.Bootstrap(time)`
- `Lifecycle.Tick(time)`

System:

- `SystemCommand.Noop`
- `SystemRequest.Query.Creeps(limit, predicate)`
- `SystemRequest.Query.CreepsByAssignment(limit, type, predicate)`
- `SystemResponse.Query.CreepsResponse(result)`

Room:

- `RoomCommand.Scan`
- `RoomCommand.SyncStage`
- `RoomCommand.SyncPlanningCache`
- `RoomCommand.SyncSemaphores`
- `RoomIntent.EnsureControllerSurvival`
- `RoomIntent.PlanWorkforce`
- `RoomRequest.TryAcquireResourceById(ownerId, resourceId)`
- `RoomRequest.TryAcquireResourceByType(ownerId, near, type)`
- `RoomRequest.ReleaseResourceById(ownerId, resourceId)`
- `RoomResponse.TryAcquireResource(result)`
- `RoomResponse.TryAcquireAnyResource(result)`
- `RoomResponse.ReleaseResource(result)`

Spawn:

- `SpawnCommand.TrySpawnWorker(assignment, profile)`

Creep:

- `CreepCommand.Assign(assignment)`
- `CreepCommand.ClearAssignment`
- `CreepCommand.SetLockedResourceId(resourceId)`

## Affiliation Model

The runtime separates actor ownership from room affiliation:

- actor ownership: `SystemActor` owns every `CreepActor`
- long-lived affiliation: `CreepMemory.homeRoom`
- assignment affiliation: `CreepAssignment.roomName`
- live location: `Game.creeps[name].room.name`

Room workforce planning currently queries `SystemActor` for creeps where:

```kotlin
creep.memory.homeRoom == room.name || assignment?.roomName == room.name
```

This lets planning count creeps serving the room even when they are not physically standing in the room.

## Binding Loss

Domain actors are bound to live Screeps state except `SystemActor`.

If a bound actor cannot resolve its target object or room, `ActorBase.run()` returns. The actor is then destroyed and
removed by the base runtime. This is the normal cleanup path for missing creeps, spawns, or rooms.

## Adding A Protocol

When adding behavior across actors:

1. Add a command or request/response type in the target domain package.
2. Implement `processCommand()` or `processRequest()` on the owner actor.
3. Keep the message name intent-based, not implementation-based.
4. Keep memory mutation inside the actor that owns the state.
5. Document the new protocol here and in the relevant behavior doc.
