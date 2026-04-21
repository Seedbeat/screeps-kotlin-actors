# Room Planning And Resources

Last source scan: 2026-04-21.

This document covers room stages, workforce planning, and room-managed resource locks.

## RoomActor Responsibilities

`RoomActor` owns room-wide policy. It is the only domain actor that should decide room population targets, room work
allocation, room resource semaphore shape, and room-side resource arbitration.

`SpawnActor` executes spawn commands, but it should not decide room workforce policy.

## Room Lifecycle Work

On Bootstrap, `RoomActor` runs:

- scan room children
- sync stage
- sync planning cache
- sync semaphores
- add recurring room intents
- broadcast Bootstrap to spawn actors

On each Tick, `RoomActor` runs:

- scan room children
- sync stage every 2 ticks
- sync planning cache every 4 ticks
- sync semaphores every 3 ticks
- process room intents
- broadcast Tick to spawn actors

`scanRoom()` calls `syncChildren()` and `RoomSemaphoreService.reconcileMissingResourceOwners()`.

## Room Stages

`RoomStagePlanner.calculate(room)` stores `RoomMemory.stage`.

Implemented stage thresholds:

- `Stage1`: fallback when higher requirements are not met.
- `Stage2`: controller level at least 2, at least 5 extensions, all sources adjacent to containers.
- `Stage3`: controller level at least 3, at least 10 extensions, at least 1 tower, all sources adjacent to containers.
- `Stage4`: controller level at least 4, at least 20 extensions, at least 1 tower, all sources adjacent to containers,
  and storage exists.

`RoomStage` also declares `Stage5` through `StageMax`, but `RoomStagePlanner` currently does not calculate them.

## Planning Cache

`RoomPlanningAnalyzer.analyze(room)` writes `RoomMemory.planningCache`.

Cached fields include:

- update tick
- stage and controller level
- energy capacity
- source count and source open sides
- estimated sustainable income
- buffered energy in room, containers, storage, and terminal
- spawn, extension, and tower counts
- spawn, extension, and tower energy deficits
- storage and terminal presence
- controller downgrade timer
- construction site counts and remaining work by priority bucket

The cache is an optimization and planning input. Room logic should tolerate a missing cache and calculate a conservative
fallback.

## Room Intents

`RoomIntent` is a repeatable command and an actor intent.

Current recurring intents:

- `EnsureControllerSurvival`, priority `CRITICAL`
- `PlanWorkforce`, priority `HIGH`

Because these intents are repeatable, they remain in the intent queue after completing.

## EnsureControllerSurvival

Owner: `RoomIntentService.ensureControllerSurvival()`.

Goal: keep at least one creep assigned to prevent controller downgrade.

Flow:

1. Resolve room controller. If missing, retain the intent.
2. Query room-affiliated workers from `SystemActor`.
3. Check whether a `ControllerUpkeep` assignment already targets this controller.
4. Assign an available unassigned worker if possible.
5. Otherwise send `SpawnCommand.TrySpawnWorker` with a bootstrap profile.

Assignment created:

```text
CreepAssignment.ControllerUpkeep(roomName, controllerId)
```

## PlanWorkforce

Owner: `RoomIntentService.planWorkforce()`.

Goal: allocate workforce across:

- energy transfer
- construction
- controller progress

Inputs:

- controller
- room construction sites
- room-affiliated workers from `SystemActor`
- current assignments and worker capabilities
- `RoomMemory.planningCache`
- target counts already assigned per construction site and transfer target
- `RoomWorkAllocator.plan()`

The service first tries to assign available unassigned workers. If needed, it may reassign workers away from over-target
task categories. If total current assigned work is below the plan target, it sends a spawn command.

## Workforce Modes

`RoomWorkAllocator.selectMode()` chooses:

- `Bootstrap`: missing planning cache, controller level <= 2, or fewer than 5 extensions.
- `Recovery`: controller downgrade soon, no workers, or very low energy.
- `Steady`: normal operation.
- `Surplus`: storage exists and buffered energy is at least 5000.

Spawn profile by mode:

- `Bootstrap` and `Recovery`: `WorkerSpawnProfile.Bootstrap`
- `Steady`: `WorkerSpawnProfile.Standard`
- `Surplus`: `WorkerSpawnProfile.Heavy`

## Work Unit Planning

Work units are based on WORK parts.

`RoomWorkAllocator` calculates:

- planned worker work units from `BodyRecipe` and room energy capacity
- total target work units from source count, estimated income, source open sides, and mode utilization
- task demand for construction
- task demand for energy transfer
- remaining work units assigned to controller progress

Task priority order is demand-driven. Energy transfer and construction receive minimum and desired grants first;
leftover work goes to controller progress.

## Construction Planning

`ConstructionPlanningPolicy` selects active construction sites and a target site.

Site priority:

- spawn: highest
- extension and tower
- storage, terminal, container
- rampart
- road
- other

Active site window size increases with room mode:

- Bootstrap: 1
- Recovery: 2
- Steady: 3
- Surplus: 4

Demand considers remaining weighted construction work, room stage target build ticks, energy ratio, site priority, and
builder capacity around each site.

## Energy Transfer Planning

`EnergyTransferPlanningPolicy` selects refill targets among extensions, spawns, and towers.

Selection favors:

- extension and spawn targets over other targets
- unassigned targets
- higher free energy capacity

Active transfer target window size increases with mode:

- Bootstrap: 2
- Recovery: 3
- Steady: 4
- Surplus: 6

Current target concurrency is one creep per target.

## Resource Semaphores

Room-managed resource locks are implemented by:

- `RoomSemaphoreCoordinator`
- `RoomSemaphoreService`
- `RoomMemory.resourceSemaphore`
- `RoomMemory.resourceLockOwners`
- `CreepMemory.lockedObjectId`

Current resource type:

- `RoomResourceType.SOURCE`

Semaphore entries store:

- `current`: current number of holders
- `maximum`: max concurrent holders

`Semaphore.tryAcquire(id)` increments `current` if below `maximum`.

`Semaphore.tryRelease(id)` decrements `current` if above zero.

## Semaphore Definitions

`RoomSemaphoreCoordinator.createSyncPlan()` compares desired definitions to current room memory.

Definitions currently include:

- each source with maximum equal to open adjacent walkable sides
- source-adjacent containers after `Stage1`
- storage with max 8

When a resource definition changes or disappears, affected owners are unassigned and locks are released before the
semaphore is recreated or deleted.

## Resource Acquire Release Flow

Creep harvest flow:

1. `CreepAssignmentService` needs energy in HARVEST phase.
2. It checks `CreepMemory.lockedObjectId`.
3. If the locked source exists and has energy, it keeps using it.
4. If the lock is stale, it releases it.
5. It requests `RoomRequest.TryAcquireResourceByType(ownerId, near, SOURCE)` from the assignment room.
6. `RoomSemaphoreService` chooses the closest available source by path near the assignment target anchor.
7. On success, the room stores owner -> resource and sends `CreepCommand.SetLockedResourceId(resourceId)`.

Release flow:

1. Creep switches from HARVEST to WORK or clears/replaces assignment.
2. `CreepAssignmentService.releaseLockedResourceIfHeld()` sends `RoomRequest.ReleaseResourceById`.
3. The room releases the semaphore, removes owner mapping, and sends `SetLockedResourceId(null)`.

Normal lock lifetime is harvest phase only.

## Orphan Reconciliation

`RoomSemaphoreService.reconcileMissingResourceOwners()` scans `RoomMemory.resourceLockOwners` and releases locks whose
owner actor no longer exists according to `ActorSystem.contains(ownerId)`.

This is intentionally actor-based, not room-presence-based. A creep outside the room may still be a valid actor.

## Planning Change Rules

When changing room planning:

- keep policy in `RoomActor`, `RoomIntentService`, or `room/planning`
- keep spawn execution in `SpawnActor`
- query `SystemActor` for affiliation-sensitive creep counts
- document new assignments and memory schema
- avoid using live room presence as the only source for population accounting
- explain performance impact if adding scans or sorting in hot paths
