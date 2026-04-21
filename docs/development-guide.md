# Development Guide

Last source scan: 2026-04-21.

This document gives practical rules for changing the project safely.

## Default Approach

When adding gameplay behavior:

1. Identify the owner actor.
2. Add or extend a protocol.
3. Keep memory mutation inside the owner.
4. Keep persistence explicit and reconstructible.
5. Verify build or explain why it was not run.
6. Update the relevant doc.

Prefer small actor-owned changes over broad reshuffles.

## Owner Selection

Use this ownership table:

| Behavior                   | Owner                                   |
|----------------------------|-----------------------------------------|
| global actor discovery     | `SystemActor`                           |
| global creep queries       | `SystemActor`                           |
| room policy                | `RoomActor`                             |
| room workforce planning    | `RoomActor` / `RoomIntentService`       |
| room resource locks        | `RoomActor` / `RoomSemaphoreService`    |
| spawn execution            | `SpawnActor`                            |
| creep assignment execution | `CreepActor` / `CreepAssignmentService` |
| body selection             | `BodyRecipe`                            |
| persistent memory shape    | `memory/*`                              |
| runtime scheduling         | `actor/*`                               |

Do not move creep actor ownership under rooms.

## Adding A Room Intent

Checklist:

1. Add a `RoomIntent` variant with priority and repeatability.
2. Decide whether it belongs in `RoomIntent.recurring`.
3. Implement it in `RoomIntentService`.
4. Add its `executeIntent()` branch in `RoomActor`.
5. Query `SystemActor` for affiliation-sensitive creep data when needed.
6. Keep spawn requests as `SpawnCommand.TrySpawnWorker`.
7. Update `room-planning-and-resources.md`.

## Adding A Creep Assignment

Checklist:

1. Add a `CreepAssignment` variant.
2. Add memory serialization in `memory/types/assignment`.
3. Implement execution in `CreepAssignmentService`.
4. Add body recipe handling in `BodyRecipe`.
5. Add capability checks in `CreepCapabilities` if needed.
6. Update room planning and status counting if it affects workforce math.
7. Document phase, lock, target, and reset behavior in `creep-assignments.md`.

## Adding A Resource Type

Checklist:

1. Add `RoomResourceType` value.
2. Update `RoomSemaphoreCoordinator` definitions.
3. Update `RoomSemaphoreService.tryAcquireAnyResource()`.
4. Update creep execution to acquire and release it through room requests.
5. Update memory docs if semaphore shape changes.
6. Document cleanup behavior for missing owners and changed definitions.

## Adding Persistence

Checklist:

1. Decide whether state belongs to global, room, creep, spawn, or a nested memory node.
2. Add typed memory accessors.
3. Use explicit fields, not opaque blobs.
4. Define default and invalid-data behavior.
5. Define cleanup behavior.
6. Define reset behavior.
7. Document the schema in `memory-and-persistence.md`.

Do not rely on `ActorKernel.snapshot()` for new domain persistence. It stores actor ids and type names only.

## Runtime Changes

Treat changes in `actor/` as higher risk.

Preserve:

- FIFO mailbox behavior
- ready queue duplicate suppression
- response matching by message id
- explicit request failure when target actors disappear
- lifecycle message ordering
- CPU reserve and max-step stop behavior

If changing scheduling, document:

- old order
- new order
- reason
- impact on request continuations
- impact on reset behavior

## Performance Rules

Screeps CPU is limited. Keep hot paths direct.

Be careful with:

- full-room scans
- sorting creep or construction site lists every tick
- building many temporary collections
- noisy per-tick logging
- hiding expensive `Room.find` calls behind innocent-looking helpers

Existing mitigations:

- `RoomFindContext` caches common room finds.
- `lazyPerTick` caches object lookups within a tick.
- `RoomActor` syncs stage, planning cache, and semaphores on different intervals.
- `ActorSystem.tick()` has max-step and CPU-reserve stops.

When optimizing, document what got cheaper and whether tick ordering changed.

## Logging Rules

Logging is useful but can burn CPU and console bandwidth.

Use logs for:

- lifecycle transitions
- protocol failures
- unexpected Screeps return codes
- state changes such as room stage updates
- cleanup failures

Avoid ungated noisy logs inside hot per-creep loops unless debugging intentionally.

Include actor id, room, object id, and message id where that context exists.

## Current Known Gaps

Known gaps at scan time:

- `ActorKernel.restore()` does not rehydrate actors.
- request handler exceptions in `ActorBase` are logged but do not currently fail pending request continuations.
- `SpawnActor.processRequest()` is `TODO()`.
- `CreepActor.processRequest()` is `TODO()`.
- no test sources exist.
- `RoomStagePlanner` only calculates stages 1 through 4.
- `RoomResourceType` only supports `SOURCE`.
- combat, defense, remote mining, hauling, claiming, market, labs, towers, and construction placement are not
  implemented as actor-owned gameplay.
- `SpawnMemoryExtensions`, `FlagMemoryExtensions`, and `PowerCreepMemoryExtensions` contain placeholder `test` fields.

Do not paper over these gaps in docs. If you implement one, remove it from this list and document the new behavior.

## Review Checklist

Before finishing a change:

- actor ownership is unchanged or intentionally documented
- messages cross actor boundaries through protocols
- memory schema is explicit
- reset behavior is clear
- room planning does not rely only on current room position
- resource locks release on normal phase transitions
- runtime scheduling changes are documented
- build or relevant verification was run, or the reason is stated
