# AGENTS.md

## Project

`screeps-kotlin-actors` is a Kotlin/JS Screeps bot migrating from the old scheduler/role system to an actor-based runtime.

The active runtime path is:

- `Main.loop()` -> `Root.gameLoop()`
- `Root.onReset()` -> `actors.base.Actors.init()`
- `Root.loop()` -> `actors.base.Actors.tick()`
- `EventScheduler.execute()` is disabled in `Root`

The actor runtime is authoritative. Legacy scheduler code remains as migration source material.

---

## Runtime

### Active modules

- actor runtime: `src/jsMain/kotlin/actor/`
- domain actors: `src/jsMain/kotlin/actors/`
- runtime entry helpers: `src/jsMain/kotlin/actors/base/Actors.kt`

Module boundary:

- `actor/` is runtime infrastructure
- `actors/` is business logic
- the main integration surface between `actors/` and `actor/` is `ActorBase`
- business logic should not talk to `ActorKernel` directly
- `ActorKernel` should behave as runtime core and interact through `ActorSystem`

### Ownership

- `SystemActor`
  - actor id `SYSTEM`
  - owns `RoomActor` for player-controlled rooms
  - owns `CreepActor` for all player creeps in `Game.creeps`
  - answers global creep-affiliation queries for planning
- `RoomActor`
  - actor id is the room name
  - owns `SpawnActor` for that room
  - owns room-wide planning, semaphore sync, and room resource coordination
- `SpawnActor`
  - actor id is `StructureSpawn.id`
  - executes spawn-side actions
  - still delegates actual spawn execution to legacy `spawn/Spawner.kt`
- `CreepActor`
  - actor id is the creep name
  - lifecycle is global, not derived from current room position
  - owns creep-local cleanup and request handling

### Affiliation

Use these meanings consistently:

- `CreepMemory.homeRoom`
  - persistent room affiliation for room-level ownership and population planning
- `CreepMemory.assignmentRoom`
  - persistent task-assignment room
- `Game.creeps[name].room.name`
  - live current room

Rules:

- actor ownership must not depend on current room position
- current room is derived runtime state, not authoritative persistence
- room population logic should query `SystemActor` when affiliation matters

### Message model

Actors communicate only through explicit protocols:

- lifecycle: `actors.base.Lifecycle`
- commands: `*Command`
- requests: `*Request`
- responses: `*Response`

Prefer a protocol change over direct shared-state coupling across actor boundaries.

### Kernel invariants

- actor scheduling is cooperative and coroutine-based
- `ActorKernel` owns mailbox delivery, next-tick wakeups, and request/response continuation scheduling
- `ActorKernel` is runtime core, not business-logic API
- business logic in `actors/` should reach runtime services through `ActorBase` and `ActorSystem`, not through direct `ActorKernel` calls
- request continuations must not hang indefinitely
- if a request target disappears before responding, the requester must fail explicitly
- preserve scheduling and message-ordering semantics unless a change is explicitly intended and documented

---

## Planning and resources

### Room planning

- room-level population targets belong to `RoomActor`
- `SpawnActor` executes spawn-side actions and should not own room-wide population policy
- do not use `room.find(FIND_MY_CREEPS)` as the authoritative room population source when remote or cross-room behavior matters

### Room resources

- `RoomActor` owns semaphore sync
- `RoomSemaphoreCoordinator` computes resource semaphore definitions
- `RoomRequest.TryAcquireResource` acquires room-managed resource locks
- `RoomRequest.ReleaseResource` releases room-managed resource locks
- `RoomActor` reconciles orphaned lock owners when the owning actor no longer exists

Rules:

- prefer room-mediated acquire/release over ad hoc memory writes
- if semaphore shape or lock semantics change, review both actor code and legacy helpers
- when checking whether a lock owner still exists, use actor/runtime ownership rules rather than room-local creep presence

---

## Persistence

Persistence is partial. Be precise.

Current state:

- `ActorSystem.tick()` writes `Memory.actorKernelSnapshot`
- `ActorKernel.snapshot()` stores actor ids and actor type names
- `ActorKernel.restore()` does not rehydrate actor instances yet
- creep affiliation persists through `CreepMemory.homeRoom` and `CreepMemory.assignmentRoom`

Implications:

- do not assume arbitrary actor-local state survives reset
- actor snapshotting is runtime bookkeeping, not full restoration
- current room position should be derived from `Game`, not persisted as actor truth
- if you add persistent actor state, you must define restore behavior as well

If you change persistence:

- document the schema change
- explain reset behavior
- document any migration or backfill for new memory fields
- avoid silent incompatibilities with existing memory

---

## Legacy boundary

Treat these as legacy migration material:

- `src/jsMain/kotlin/scheduler/`
- `src/jsMain/kotlin/scheduler/events/`
- `src/jsMain/kotlin/scheduler/missions/`
- `src/jsMain/kotlin/creep/roles/`
- `src/jsMain/kotlin/room/RoomContext.kt`

Notes:

- `EventScheduler` is not part of the live loop
- old creep role functions are not the active per-tick path
- `RoomContext` may still contain useful search or analysis helpers
- `Root.room()` / `Root.rooms()` and `RoomContext` are legacy support surfaces, not authoritative runtime state
- `spawn/Spawner.kt` is legacy code but still partially active through `SpawnActor`

Do not add new gameplay behavior to the old scheduler path unless explicitly asked for a legacy-only fix.

---

## Migration rules

When moving behavior:

1. find the invariant in legacy code
2. decide which actor should own the decision
3. express orchestration through actor commands, requests, or intents
4. keep direct memory mutation localized and explicit
5. preserve gameplay semantics unless redesign is explicitly requested

Preferred ownership:

- global orchestration -> `SystemActor`
- global creep registry / creep-affiliation queries -> `SystemActor`
- room-wide planning and shared resources -> `RoomActor`
- spawn-side actions -> `SpawnActor`
- creep-local state and cleanup -> `CreepActor`

Do not revive `CreepExecutor` as the long-term solution.

---

## Coding rules

### General Kotlin rules

- prefer explicit Kotlin over clever abstractions
- keep hot-path code allocation-aware
- use `val` unless mutation is required
- keep nullability meaningful
- favor sealed protocols and enums when they clarify contracts

### Actor rules

- each actor should own a narrow slice of behavior
- message names should describe intent, not implementation detail
- request/response pairs should stay type-specific
- do not bypass actor boundaries unless there is a concrete cleanup or fallback need
- keep tick order understandable

### Legacy migration rules

- do not copy old code blindly into actors
- extract the invariant first, then re-express it in actor form
- when old code and actor code overlap, prefer the actor path for new work

### Logging rules

- keep logs useful under Screeps CPU limits
- avoid noisy per-tick logs in hot loops unless intentionally gated
- include actor id, message id, and room/object context when debugging protocol failures

---

## Performance

This project runs under Screeps CPU constraints. Preserve these properties:

- avoid unnecessary allocations in per-tick loops
- avoid rebuilding collections if per-tick caching already exists
- prefer bounded queue work over unstructured scans
- do not hide expensive room or game queries behind pretty abstractions
- preserve processing order unless an ordering change is intentional and documented

When optimizing, explain:

1. what path got cheaper
2. why behavior stays the same
3. whether tick ordering changed

---

## Safe changes

Usually safe:

- extending an existing actor protocol
- moving one behavior into the correct actor
- extracting small helpers inside a module
- clarifying intent selection logic
- improving cleanup and invariant checks
- updating docs to match the actor migration

Usually unsafe:

- re-enabling legacy scheduler execution as a shortcut
- changing actor id conventions
- changing lifecycle flow for `Bootstrap` / `Tick`
- introducing hidden shared mutable state between actors
- writing persistence without a restore story
- broad package reshuffles during migration

If unsure, do the smaller patch.

---

## Build

Current assumptions:

- Kotlin Multiplatform is configured for JS
- bundling uses the existing Gradle pipeline
- important tasks include `build`, `optimize`, `release`, and `deploy`
- Screeps upload format and artifact names should stay stable unless explicitly changed

Do not add new build complexity without a concrete reason.

---

## Documentation and communication

When changing actor behavior or migrating legacy logic, document:

- what changed
- which actor now owns it
- behavioral impact
- persistence impact
- performance impact
- remaining migration work

Update `AGENTS.md` or `docs/*` when architecture, invariants, migration assumptions, or runtime boundaries have changed enough that the existing documentation would mislead future work.

If tests were not run or do not exist for that area, say so explicitly.

Comments should explain why an invariant exists, especially around:

- actor lifecycle
- message ordering
- mailbox scheduling
- request failure semantics
- semaphore recreation
- cleanup on object disappearance

---

## Stop and ask

Stop for human review when the task requires:

- redesigning the actor hierarchy
- replacing persistence format
- deciding between incompatible migration strategies
- moving a large block of legacy room/creep logic without clear ownership
- changing core scheduling or message-ordering semantics in `ActorSystem` or `ActorKernel`

You do not need to stop for small correctness fixes in `ActorKernel` if they preserve the existing scheduling model and message ordering.

If the request is ambiguous, preserve current actor-first execution and choose the least risky migration step.

---

## Final rule

This repository is no longer centered on the old scheduler. Migrate behavior into the actor runtime without breaking Screeps semantics, and prefer a small actor-owned migration step over a shortcut through the legacy path.
