# Creep Assignments

Last source scan: 2026-04-21.

This document describes creep-local behavior in `src/jsMain/kotlin/creep/`.

## CreepActor

`CreepActor` is globally owned by `SystemActor` and keyed by creep name. It is not owned by the room where the creep is
currently standing.

On each `Lifecycle.Tick`, `CreepActor` delegates to `CreepAssignmentService.executeAssignment()`.

Supported commands:

- `CreepCommand.Assign`
- `CreepCommand.ClearAssignment`
- `CreepCommand.SetLockedResourceId`

`CreepRequest` exists but has no implemented handlers.

## Assignment Types

`CreepAssignment` is a sealed interface. Every assignment has `roomName`.

Current assignments:

- `ControllerUpkeep(roomName, controllerId, phase)`
- `ControllerProgress(roomName, controllerId, phase)`
- `Construction(roomName, constructionSiteId, phase)`
- `EnergyTransfer(roomName, targetId, goal, phase)`

All current assignment types are `PhaseAssignment` and use:

- `HARVEST`
- `WORK`

`EnergyTransfer.Goal` variants:

- `UntilFull`
- `Amount(amount)`
- `Percent(percentage)`

## Assignment Memory

Assignments persist through `CreepMemory.assignment`.

The memory codec is implemented by:

- `memory/types/assignment/CreepAssignmentMemory.kt`
- `memory/types/assignment/values/*`
- `memory/types/assignment/goal/*`

The sealed memory node writes a `_type` discriminator using the Kotlin class simple name and then writes explicit fields
for that variant.

Assignment fields are explicit so behavior can be reconstructed after reset even though actor instances are not
rehydrated.

## Affiliation Fields

Use these meanings:

- `CreepMemory.homeRoom`: long-lived room affiliation.
- `CreepAssignment.roomName`: room that owns the active task.
- `CreepStatus.currentRoom`: live room from `creep.room.name`.

Room planning currently counts workers where either `homeRoom` or assignment room matches the room being planned.

## CreepStatus

`CreepStatus` is used by `SystemActor` query responses.

It contains:

- `actorId`
- `homeRoom`
- `currentRoom`
- `assignment`
- `capabilities`
- `lockedResourceId`

`capabilities` are derived from body parts:

- `work`: number of WORK parts
- `carry`: number of CARRY parts times carry capacity
- `move`: number of MOVE parts

Current capability booleans for upkeep, construction, and transfer all require at least one WORK, CARRY, and MOVE.

## Harvest Work Pattern

All current assignments use `executeHarvestBasedWork()`.

HARVEST phase:

1. If creep energy is full, switch to WORK.
2. Resolve or acquire a source lock in the assignment room.
3. Harvest the source.
4. Move toward the source if not in range.
5. If source is empty and creep has energy, switch to WORK.

WORK phase:

1. If creep energy is empty, switch to HARVEST.
2. Release any held source lock.
3. Execute the assignment action.
4. Move toward target if not in range.
5. If action reports not enough resources, switch to HARVEST.
6. If target is invalid or full in a terminal way, clear assignment.

Source locks should not be held in WORK phase.

## Assignment Actions

`ControllerUpkeep`:

- target: `StructureController`
- action: `upgradeController`

`ControllerProgress`:

- target: `StructureController`
- action: `upgradeController`

`Construction`:

- target: `ConstructionSite`
- action: `build`

`EnergyTransfer`:

- target: `Structure` cast to `StoreOwner`
- action: `transfer(RESOURCE_ENERGY)`
- goal determines full transfer, fixed amount, or percentage threshold

## Target Validation

`resolveRoomObject()` loads target objects by id and verifies:

- the object exists
- `obj.pos.roomName == assignment.roomName`

If the target is missing, the assignment is cleared. If the target exists in a different room than the assignment says,
an error is logged and the target is treated as invalid.

## Lock Lifecycle

Relevant memory:

- `CreepMemory.lockedObjectId`
- `RoomMemory.resourceLockOwners`
- `RoomMemory.resourceSemaphore`

Normal lifecycle:

1. Creep in HARVEST phase requests a source lock from the assignment room.
2. Room records owner -> resource and replies with resource id.
3. Creep stores `lockedObjectId`.
4. Creep harvests.
5. On phase change to WORK, assignment replacement, or assignment clear, creep releases the lock through the room actor.
6. Room clears owner mapping and sends `SetLockedResourceId(null)`.

Fallback cleanup:

- `CreepActor.onDestroy()` clears assignment and locked resource memory.
- `RoomSemaphoreService.reconcileMissingResourceOwners()` releases locks whose owner actor no longer exists.
- Semaphore recreation unassigns affected owners and releases affected locks.

## Spawned Workers

`Spawner.spawn(assignment, profile)` chooses a body through `BodyRecipe.selectBodySpecByAssignment()`.

Current worker body base:

- core: MOVE, WORK, CARRY
- standard/heavy add repeat segments when energy allows

Profiles:

- `Bootstrap`: cheapest functional worker
- `Standard`: limited scaling for normal operation
- `Heavy`: larger scaling for surplus operation

New creeps receive memory from `createCreepMemory`:

- `homeRoom = spawn.room.name`
- `assignment = requested assignment`

Name format is:

```text
<body-label>_<Game.time in base 32>
```

## Adding An Assignment

When adding a new assignment:

1. Add a `CreepAssignment` variant.
2. Decide whether it is phase-based.
3. Add memory node serialization and deserialization.
4. Implement execution in `CreepAssignmentService`.
5. Add body recipe and capability logic if needed.
6. Update room planning to create or count it.
7. Document lock behavior and reset behavior.

Do not add opaque serialized assignment objects. Keep fields explicit and reconstructible.
