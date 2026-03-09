# Actor Runtime

## Overview

This project is a Screeps bot. The current development focus is migrating from the old scheduler/role model to an actor-first runtime.

The actor runtime is the live execution model:

- `Main.loop()` -> `Root.gameLoop()`
- `Root.onReset()` -> `actors.base.Actors.init()`
- `Root.loop()` -> `actors.base.Actors.tick()`

The old scheduler and role code still exist, but they are migration source material rather than the authoritative runtime.

This document explains why the current actor architecture is shaped the way it is. For operational rules and guardrails, see `AGENTS.md`.

## Layering

The codebase is intentionally split into two layers:

- `src/jsMain/kotlin/actor/`
  - runtime infrastructure
  - scheduling, mailbox delivery, request/response continuation management, snapshot bookkeeping
- `src/jsMain/kotlin/actors/`
  - business logic
  - room orchestration, spawn-side actions, creep-local behavior, resource coordination

The main boundary between these layers is `ActorBase`.

Why this boundary exists:

- business logic should not depend on runtime internals
- runtime code should not accumulate domain-specific decisions
- kernel changes are easier to reason about when `ActorKernel` stays a runtime core rather than becoming a shared utility surface

`ActorKernel` is intentionally not a public business-logic API. Domain actors should interact with runtime services through `ActorBase` and `ActorSystem`.

## Ownership model

The active ownership model is:

- `SystemActor`
  - owns `RoomActor`
  - owns `CreepActor`
- `RoomActor`
  - owns `SpawnActor`
- `SpawnActor`
  - executes spawn-side actions
- `CreepActor`
  - owns creep-local cleanup and request handling

The key design choice is that `SystemActor` owns all `CreepActor`.

Why:

- creep existence is global
- a creep can move between rooms, including non-owned rooms and transit rooms
- ownership should be stable across movement
- room-level planning still needs access to creep counts without tying creep lifecycle to room position

The earlier model where `RoomActor` owned creep actors based on room presence breaks down for:

- remote hauling
- scouting
- claim/reserve flows
- military creeps outside owned rooms
- any cross-room logistics

If a creep actor were owned by the room where the creep is currently standing, leaving that room could delete the actor even though the creep still exists and still matters to the bot.

## Affiliation model

The actor model separates creep ownership from creep affiliation.

Use these meanings consistently:

- `homeRoom`
  - persistent room affiliation
  - the room that should count the creep for long-lived room-level ownership and population planning
- `assignmentRoom`
  - persistent task-affiliation room
  - useful when a creep works for another room without changing long-term ownership
- `currentRoom`
  - live room position from `Game.creeps[name].room.name`

Why this split exists:

- `currentRoom` changes constantly and should not define actor ownership
- `homeRoom` is stable enough for planning
- `assignmentRoom` lets room-level logic reason about cross-room work without collapsing everything into physical position

This is why `currentRoom` is derived from `Game` rather than stored as authoritative memory state.

## Planning model

Room-wide decisions belong to `RoomActor`.

That includes:

- room-level population targets
- room semaphore sync
- room resource coordination
- room intents

Spawn-side execution belongs to `SpawnActor`.

That means:

- `RoomActor` decides whether another creep is needed
- `SpawnActor` performs the spawn-side action
- legacy `spawn/Spawner.kt` remains an execution helper for now

Global creep counting belongs to `SystemActor`.

Why this split exists:

- room-wide planning should stay in the room actor
- counting creeps by room affiliation should not depend on physical room presence
- spawn actors should not own room-wide policy
- multiple spawns in one room should not each make their own policy decisions independently

This is why room population logic queries `SystemActor` rather than using `room.find(FIND_MY_CREEPS)` as the sole source of truth.

## Resource coordination

Room resource coordination is moving under actor ownership rather than ad hoc memory mutation.

Current design:

- `RoomActor` owns room semaphore state
- room-side acquire and release go through room protocols
- lock ownership is reconciled against actor/runtime existence rather than room-local creep presence

Why:

- resource coordination is a room-wide concern
- room-level arbitration is easier to reason about than distributed writes
- cleanup must remain correct even when creeps move between rooms or disappear

## Kernel semantics

The runtime is cooperative and coroutine-based.

`ActorKernel` is responsible for:

- mailbox delivery
- scheduling wakeups for next tick
- scheduling request/response continuations
- snapshot bookkeeping for runtime actor identity

Two semantics matter a lot:

1. message ordering must stay understandable and deterministic
2. request failure must be explicit

Explicit request failure means:

- a request continuation must not hang forever
- if the target actor disappears before responding, the requester must fail

This was an intentional fix because silent hangs are much harder to debug than explicit failures and they can wedge room orchestration.

Kernel bugfixes that preserve scheduling and ordering are fine. Kernel redesigns that alter ordering or lifecycle semantics should be treated as a higher-risk change.

## Persistence

Persistence is intentionally partial right now.

What is persisted:

- `Memory.actorKernelSnapshot`
- actor ids and actor type names
- creep memory such as `homeRoom` and `assignmentRoom`

What is not fully restored:

- actor instances
- arbitrary actor-local state
- mailbox state
- sleeping continuations
- pending request/response continuations

So current persistence is runtime bookkeeping plus memory-backed domain state, not full actor restoration.

That is why any new persistent actor state needs both:

- a schema story
- a restore story

without assuming snapshot writing alone is enough.

## Migration strategy

When porting behavior from legacy code:

1. identify the invariant first
2. decide the correct owner actor
3. re-express orchestration through commands, requests, or intents
4. keep memory mutation explicit
5. preserve gameplay semantics unless a redesign is intentional

Typical mistakes to avoid:

- reattaching new behavior to the old scheduler path as a shortcut
- deriving creep lifecycle from current room presence
- letting spawn actors own room-wide population policy
- treating `ActorKernel` as a business-logic utility
- writing persistence without a restore story

## Practical reading guide

If you are trying to understand the live runtime, start here:

1. `Root.kt`
2. `actors/base/Actors.kt`
3. `actor/ActorSystem.kt`
4. `actor/ActorKernel.kt`
5. `actors/SystemActor.kt`
6. `actors/RoomActor.kt`
7. `actors/SpawnActor.kt`
8. `actors/CreepActor.kt`

If you are trying to migrate legacy behavior, use the old scheduler and role code as source material, but keep the actor runtime as the authoritative execution model.
