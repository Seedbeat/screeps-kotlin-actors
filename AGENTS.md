# AGENTS.md

## Project

`screeps-kotlin-actors` is a Kotlin/JS Screeps bot. The checked-out project is actor-first, and the runtime should be
understood from the actor packages.

Authoritative loop:

- `Main.loop()` calls `Root.gameLoop()`
- first tick after a VM reset calls `Root.onReset()`
- `Root.onReset()` calls `actors.base.Actors.init()`
- `Actors.init()` spawns `SystemActor` with actor id `SYSTEM` and sends `Lifecycle.Bootstrap`
- each game tick calls `Actors.tick()`
- `Actors.tick()` sends `Lifecycle.Tick` to `SYSTEM`, then runs `ActorSystem.tick()`

## Read These Docs

Start with:

- `docs/README.md` for project map and reading order
- `docs/actor-runtime.md` for scheduler, mailbox, request/response, and reset behavior
- `docs/domain-actors.md` for actor ownership and protocols
- `docs/room-planning-and-resources.md` for workforce planning and resource locks
- `docs/creep-assignments.md` for creep behavior and memory-backed assignments
- `docs/memory-and-persistence.md` for Memory schema and reset semantics
- `docs/build-and-deploy.md` for Gradle tasks and Screeps upload behavior
- `docs/development-guide.md` for safe change recipes and known gaps

Keep these files current when behavior or ownership changes enough that another LLM would be misled.

## Source Map

- `src/jsMain/kotlin/Main.kt`: exported Screeps `loop`
- `src/jsMain/kotlin/Root.kt`: top-level tick lifecycle, CPU logging, pixel generation
- `src/jsMain/kotlin/actor/`: runtime infrastructure
- `src/jsMain/kotlin/actors/`: system actor, actor base classes, child managers, intents
- `src/jsMain/kotlin/room/`: room actor, room protocols, semaphore service, planning integration
- `src/jsMain/kotlin/room/planning/`: room stage, planning cache, workforce allocation policies
- `src/jsMain/kotlin/creep/`: creep actor, assignments, assignment executor, bodies
- `src/jsMain/kotlin/spawn/`: spawn actor and spawn execution helper
- `src/jsMain/kotlin/memory/`: typed Memory extensions, codecs, memory nodes
- `src/jsMain/kotlin/room/context/`: cached room find helpers
- `src/jsMain/kotlin/store/`, `map/`, `heuristics/`, `utils/`: shared helpers
- `src/Constants.js`: Screeps constants shim used by the JS build

## Runtime Boundaries

Use these boundaries consistently:

- `actor/` is runtime core: actor registration, mailboxes, scheduling, wakeups, request continuations, snapshots.
- `actors/` and domain packages are business logic.
- Domain code should use `ActorBase`, `ActorApi`, `MessagingApi`, and `ActorSystem` facade methods.
- Domain code should not reach into `ActorKernel` directly.
- `ActorKernel` should not gain room, creep, spawn, or Screeps policy.

The main integration surface is `ActorBase`, which classifies payloads into lifecycle messages, commands, and requests.

## Actor Ownership

Current ownership tree:

- `SystemActor`
  - actor id: `SYSTEM`
  - owns `RoomActor` for rooms where `room.controller?.my == true`
  - owns `CreepActor` for all names in `Game.creeps`
  - answers global creep queries for planning
  - deletes stale entries in `Memory.creeps`
- `RoomActor`
  - actor id: room name
  - owns `SpawnActor` for `FIND_MY_SPAWNS` in that room
  - owns room scans, stage sync, planning cache sync, resource semaphore sync, and room intents
- `SpawnActor`
  - actor id: `StructureSpawn.id`
  - executes `SpawnCommand.TrySpawnWorker`
  - delegates actual `spawnCreep` setup to `spawn/Spawner.kt`
- `CreepActor`
  - actor id: creep name
  - owns creep-local assignment execution, assignment cleanup, and locked resource memory

Actor ownership must not depend on current room position. Creeps are global actors because a creep can move between
rooms while still belonging to the same long-lived runtime entity.

## Affiliation Terms

Use these meanings precisely:

- `CreepMemory.homeRoom`: persistent room affiliation used for room ownership and workforce planning.
- `CreepAssignment.roomName`: persistent task-affiliation room owned by assignment state.
- `Game.creeps[name].room.name`: live current room, derived from Screeps state.

Rules:

- Do not persist current room as actor truth.
- Do not derive `CreepActor` ownership from current room.
- Room population logic should query `SystemActor` when affiliation matters.
- Room logic may consider both `homeRoom == room.name` and `assignment?.roomName == room.name` when it needs workers
  currently serving the room.

## Message Model

Actors communicate through explicit payload protocols:

- lifecycle: `actors.base.Lifecycle`
- commands: `*Command`
- requests: `*Request`
- responses: `*Response`

Prefer a protocol addition over shared mutable state across actor boundaries.

Current request protocols:

- `SystemRequest.Query.Creeps`
- `SystemRequest.Query.CreepsByAssignment`
- `RoomRequest.TryAcquireResourceById`
- `RoomRequest.TryAcquireResourceByType`
- `RoomRequest.ReleaseResourceById`

Current command protocols:

- `RoomCommand.Scan`, `SyncStage`, `SyncPlanningCache`, `SyncSemaphores`
- `RoomIntent.EnsureControllerSurvival`, `RoomIntent.PlanWorkforce`
- `SpawnCommand.TrySpawnWorker`
- `CreepCommand.Assign`, `ClearAssignment`, `SetLockedResourceId`

`SpawnRequest` and `CreepRequest` are marker protocols with no implemented request handlers at the time of this scan. Do
not send request payloads to those actors until handlers exist.

## Kernel Invariants

Preserve these runtime semantics unless the user explicitly asks for a runtime redesign:

- actors are cooperative coroutines
- messages enter per-actor mailboxes
- only sleeping actors with queued mail are placed in the ready queue
- `ActorSystem.tick()` runs rounds of wakeups, scheduled continuations, and mailbox delivery
- `TickState` stops on max steps or CPU reserve
- `MessageId` resets each tick and produces ids like `Game.time|seed`
- responses match pending requests by message id
- request continuations must not hang indefinitely
- if a request target is missing or disappears before responding, fail the requester explicitly
- removing an actor destroys it, clears runtime waiters, and fails pending responses where it is the target
- scheduled continuations for removed actors are dropped

Be cautious with any change to mailbox ordering, continuation flushing, lifecycle ordering, or request failure behavior.

Current gap: target-missing and target-removed request failures are explicit, but exceptions thrown while processing a
request in `ActorBase` are logged and do not currently synthesize an error response. Keep request handlers total, or add
explicit failure semantics before adding request-heavy protocols.

## Room Planning

Room-wide policy belongs to `RoomActor` and `RoomIntentService`.

Current room intents:

- `EnsureControllerSurvival`: ensure at least one controller upkeep assignment exists or request a bootstrap worker.
- `PlanWorkforce`: allocate work units across energy transfer, construction, and controller progress.

Current workforce assignments:

- `CreepAssignment.ControllerUpkeep`
- `CreepAssignment.ControllerProgress`
- `CreepAssignment.Construction`
- `CreepAssignment.EnergyTransfer`

Rules:

- `SpawnActor` executes spawn commands but should not own population policy.
- Room planning should derive demand from room potential, planning cache, task demand, and affiliated workers, not only
  from live room creeps.
- `room.find(FIND_MY_CREEPS)` is not authoritative for cross-room planning.
- Non-emergency controller work uses `ControllerProgress`; survival work uses `ControllerUpkeep`.

## Room Resources

Room resource arbitration belongs to `RoomActor` through `RoomSemaphoreService`.

Current resource type:

- `RoomResourceType.SOURCE`

Current memory:

- `RoomMemory.resourceSemaphore`
- `RoomMemory.resourceLockOwners`
- `CreepMemory.lockedObjectId`

Rules:

- Prefer room-mediated acquire/release requests over ad hoc memory writes.
- A creep may hold a source lock only while it is in the harvest phase for its current assignment.
- Release source locks before switching into the work phase.
- `ClearAssignment` and actor destruction are fallback cleanup paths, not the normal lock lifetime.
- When reconciling orphaned owners, use actor existence (`ActorSystem.contains`) rather than room-local creep presence.
- If semaphore definitions change, review `RoomSemaphoreCoordinator`, `RoomSemaphoreService`, assignment execution, and
  memory schema together.

## Persistence

Persistence is partial. Be precise.

Current persisted state:

- `Memory.actorKernelSnapshot`: actor ids and actor type names only
- `Memory.settings`
- `RoomMemory.stage`
- `RoomMemory.planningCache`
- `RoomMemory.resourceSemaphore`
- `RoomMemory.resourceLockOwners`
- `CreepMemory.homeRoom`
- `CreepMemory.assignment`
- `CreepMemory.lockedObjectId`

Current reset behavior:

- `Actors.init()` always spawns `SYSTEM` and sends `Lifecycle.Bootstrap`.
- `ActorSystem.tick()` attempts `ActorKernel.restore(Memory.actorKernelSnapshot)` after reset.
- `ActorKernel.restore()` logs that actor rehydration is not implemented; it does not rebuild actor instances.
- Child managers reconstruct live actors from `Game.rooms`, `Game.creeps`, and room spawns.
- Snapshot writing is bookkeeping, not full runtime restoration.

Rules:

- Do not assume actor-local fields, mailboxes, sleepers, or pending continuations survive reset.
- If you add persistent actor state, define schema, write path, read path, and reset behavior.
- Keep persisted assignment state reconstructible from explicit fields.
- Keep persisted data reconstructible from explicit fields.

## Build And Verification

Gradle/Kotlin setup:

- Kotlin Multiplatform JS
- Kotlin version `2.3.10`
- CommonJS output
- Screeps Kotlin types dependency `io.github.exav:screeps-kotlin-types:2.2.0`
- coroutines dependency `kotlinx-coroutines-core:1.10.2`
- Closure Compiler dev npm dependency

Important tasks:

- `./gradlew build`
- `./gradlew optimize`
- `./gradlew release`
- `./gradlew deploy`

There are no test source files in this checkout at the time of this scan. Use `./gradlew build` as the basic
verification path for source changes. For docs-only changes, use markdown/link sanity checks and `git diff --check`.

`gradle.properties` is ignored and may contain Screeps credentials. Do not quote local token values in docs, logs,
commits, or final responses.

## Coding Rules

- Prefer explicit Kotlin over clever abstractions.
- Use `val` unless mutation is required.
- Keep nullability meaningful.
- Favor sealed protocols and enums where they clarify contracts.
- Keep hot-path code allocation-aware.
- Avoid noisy per-tick logs unless intentionally gated.
- Include actor id, message id, and room/object context when logging protocol failures.
- Keep direct Memory mutation localized and documented.
- Preserve tick order unless an ordering change is intentional and documented.

## Safe Changes

Usually safe:

- extending an existing protocol with a focused command or request
- adding a new room intent with clear room ownership
- adding a new assignment type with memory codec support and reset behavior
- extracting small helpers inside an existing module
- tightening cleanup or invariant checks
- updating docs to match the actor runtime

Usually unsafe:

- changing actor id conventions
- moving creep ownership under rooms
- changing `Bootstrap` or `Tick` flow
- changing `ActorSystem.tick()` or `ActorKernel` scheduling order
- adding hidden shared mutable state between actors
- writing persistence without restore/reset semantics
- adding spawn-side room policy to `SpawnActor`

If unsure, make the smaller actor-owned patch.

## Stop And Ask

Stop for human review when the task requires:

- redesigning the actor hierarchy
- replacing the persistence format
- changing persisted schema semantics
- moving a large behavior block without clear ownership
- changing core scheduling, mailbox ordering, or continuation semantics
- changing recovery behavior for persisted memory

Small correctness fixes that preserve scheduling and ownership do not require a stop.

## Documentation Requirement

When changing actor behavior, room planning, resource locks, or persistence, update docs with:

- what changed
- which actor owns the behavior
- protocol or memory schema impact
- reset behavior impact
- performance impact
- remaining known gaps

Final rule: preserve the actor-first runtime. Add behavior to the actor-owned path, keep runtime/domain boundaries
clear, and document enough that the next agent can continue without rediscovering the architecture.
