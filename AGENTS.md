# AGENTS.md

## Project

`screeps-kotlin-actors` is a Kotlin/JS Screeps bot in the middle of a migration from the old scheduler/role system to a new actor-based runtime.

The current runtime path is actor-first:

- `Main.loop()` -> `Root.gameLoop()`
- `Root.onReset()` -> `SystemActor.init()`
- `Root.loop()` -> `SystemActor.tick()`
- `EventScheduler.execute()` is currently disabled in `Root`
- Any logic of `EventScheduler` pipeline should not be edited due to be read-only migration material

This means the actor system is the active orchestration layer, while much of the old logic remains in the repo as migration source material.

---

## Current architecture

### Active runtime

- Low-level actor runtime lives in `src/jsMain/kotlin/actor/`
- Domain actors live in `src/jsMain/kotlin/actors/`
- The live tick pipeline is driven by `ActorSystem.tick()`
- Actor scheduling is cooperative and coroutine-based
- Mail delivery, request/response continuation scheduling, and next-tick wakeups are handled by `ActorKernel`

### Actor tree

The current hierarchy is:

- `SystemActor`
  - one actor with id `SYSTEM`
  - owns room actors for all player-controlled rooms
- `RoomActor`
  - actor id is the room name
  - owns spawn actors and creep actors for that room
  - is the current place for room-level orchestration, semaphore sync, and intent planning
- `SpawnActor`
  - actor id is `StructureSpawn.id`
  - currently handles population checks and delegates spawn execution to legacy `Spawner`
- `CreepActor`
  - actor id is the creep name
  - currently provides cleanup and request handling, but does not yet replace old per-role behavior

There is also an experimental `Harvester` actor class, but it is not part of the active actor tree yet.

### Message model

Actors communicate only through explicit message contracts:

- lifecycle messages: `actors.base.Lifecycle`
- commands: `*Command`
- requests: `*Request`
- responses: `*Response`

Keep protocols explicit. If behavior crosses actor boundaries, prefer a message type over direct shared-state mutation.

### Intent model

Room-level planning currently uses:

- `ActorIntentBase`
- `ActorIntentQueueBase`
- `RoomIntent`

At the moment, `RoomActor` plans `EnsurePopulation` intents and forwards them to `SpawnActor`.

### Binding model

Actor-to-game-object binding is done through:

- `GameRoomBinding`
- `GameObjectBinding`
- `GameCreepBinding`

If a bound Screeps object disappears, the actor is expected to stop being useful quickly and fail safely.

---

## Legacy code status

The following areas are legacy logic and should be treated as source material for migration:

- `src/jsMain/kotlin/scheduler/`
- `src/jsMain/kotlin/scheduler/events/`
- `src/jsMain/kotlin/scheduler/missions/`
- `src/jsMain/kotlin/creep/roles/`
- `src/jsMain/kotlin/room/RoomContext.kt`

Important status notes:

- `EventScheduler` is not called from the live loop right now
- old creep role functions such as `creep.roles.harvest()` are not the active per-tick execution path
- `RoomContext` still contains valuable search, targeting, and room-analysis logic that may be reused during migration
- `spawn/Spawner.kt` is legacy code, but it is still used by `SpawnActor`, so it is partially active

Do not add new gameplay behavior to the old scheduler path unless the user explicitly asks for a legacy-only fix.

If you need to port behavior, move intent and ownership into actors instead of reconnecting the old scheduler.

---

## Migration guidance

The migration direction for new behavior is:

1. find the behavior in legacy `scheduler` / `creep.roles` / `RoomContext`
2. identify which actor should own that decision
3. move orchestration into actor commands, requests, or intents
4. keep direct memory mutation localized and explicit
5. preserve gameplay semantics while changing execution model

Preferred ownership:

- global orchestration -> `SystemActor`
- room-wide planning and shared resources -> `RoomActor`
- spawn population and spawn-side actions -> `SpawnActor`
- creep-local state and cleanup -> `CreepActor`
- future specialized creep behaviors -> dedicated actor classes or expanded creep actor protocol

Do not revive `CreepExecutor` as the long-term solution. The intended target is actor-owned creep behavior.

---

## Resource and semaphore rules

Room resource locking is moving under actor ownership.

Current actor-side resource coordination:

- `RoomActor` owns semaphore sync
- `RoomSemaphoreCoordinator` computes resource semaphore definitions
- `RoomRequest.TryAcquireResource` asks a room actor for a resource lock
- `RoomCommand.ReleaseResource` releases a room-managed lock
- `CreepActor.onDestroy()` performs cleanup and releases held resource locks

Rules:

- prefer room-mediated acquire/release over ad hoc memory writes
- if you change semaphore shape or lock semantics, review both actor code and legacy helpers
- when recreating semaphores, make sure affected creep locks are released safely

---

## Persistence rules

Persistence is not fully implemented yet. Be precise here.

Current state:

- `ActorSystem.tick()` writes `Memory.actorKernelSnapshot`
- `ActorKernel.snapshot()` currently stores actor ids and actor type names
- `ActorKernel.restore()` does not rehydrate actor instances yet and logs a warning instead

Implications:

- do not assume arbitrary actor-local state survives reset
- current persistence is closer to runtime bookkeeping than full actor restoration
- if you add real persistent actor state, you must also design restore behavior, not just snapshot writing

If you touch persistence:

- document the schema change
- update the codec/snapshot path
- explain reset behavior
- avoid silent incompatibilities with existing memory

---

## Coding rules

### General Kotlin rules

- prefer explicit Kotlin over clever abstractions
- keep hot-path code allocation-aware
- use `val` unless mutation is required
- keep nullability meaningful
- favor sealed protocols and enums when they clarify actor contracts

### Actor rules

- each actor should own a narrow slice of behavior
- message names should describe intent, not implementation detail
- request/response pairs should stay type-specific
- do not bypass actor boundaries unless there is a concrete cleanup or fallback need
- keep tick order understandable

### Legacy migration rules

- do not copy old code blindly into actors
- extract the invariant first, then re-express it in actor form
- preserve the old game behavior unless the user asked to redesign it
- when old code and actor code overlap, prefer the actor path for new work

### Logging rules

- keep logs useful under Screeps CPU limits
- avoid noisy per-tick logs in hot loops unless they are intentionally gated
- log actor id, message id, and room/object context when debugging protocol failures

---

## Performance rules

This project runs under Screeps CPU constraints. Preserve these properties:

- avoid unnecessary allocations in per-tick loops
- avoid rebuilding collections if per-tick caching already exists
- prefer bounded queue work over unstructured scans
- do not hide expensive room or game queries behind pretty abstractions
- if you change scheduling or mailbox behavior, preserve processing order unless intentionally documented

When optimizing, explain:

1. what path got cheaper
2. why behavior stays the same
3. whether tick ordering changed

---

## Safe and unsafe changes

Safe changes usually include:

- extending an existing actor protocol
- moving one legacy behavior into the correct actor
- extracting small helpers inside a module
- clarifying intent selection logic
- improving cleanup and invariant checks
- updating docs to match the actor migration

Unsafe changes usually include:

- re-enabling legacy scheduler execution as a shortcut
- changing actor id conventions
- changing lifecycle flow for `Bootstrap` / `Tick`
- introducing hidden shared mutable state between actors
- writing persistence without a restore story
- broad package reshuffles during migration

If unsure, do the smaller patch.

---

## Build and tooling

Current build assumptions:

- Kotlin Multiplatform is configured for JS
- bundling is done through the existing Gradle pipeline
- important tasks include `build`, `optimize`, `release`, and `deploy`
- Screeps upload format and artifact names should stay stable unless explicitly changed

Do not add new build complexity without a concrete reason.

---

## Documentation expectations

When changing actor behavior or migrating legacy logic, update docs accordingly.

Document:

- what old behavior was migrated
- which actor now owns it
- whether persistence changed
- whether legacy code is still authoritative or now only reference material

Comments should explain why an invariant exists, especially around:

- actor lifecycle
- message ordering
- mailbox scheduling
- semaphore recreation
- cleanup on object disappearance

---

## Communication expectations

When summarizing a change, include:

1. what changed
2. why that actor or module owns it
3. behavioral impact
4. persistence impact
5. performance impact
6. remaining migration work

If tests were not run or do not exist for that area, say so explicitly.

---

## When to stop and ask

Stop and ask for human review when the task requires:

- redesigning the actor hierarchy
- replacing persistence format
- deciding between incompatible migration strategies
- moving a large block of legacy room/creep logic without clear ownership
- changing core scheduling semantics in `ActorSystem` or `ActorKernel`

If the request is ambiguous, preserve current actor-first execution and choose the least risky migration step.

---

## Final rule

This repository is no longer centered on the old scheduler. It is centered on migrating behavior into the actor runtime without breaking Screeps semantics.

Prefer a small actor-owned migration step over a clever shortcut through the legacy path.
