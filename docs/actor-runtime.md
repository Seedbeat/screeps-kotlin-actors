# Actor Runtime

Last source scan: 2026-04-21.

This document describes the runtime in `src/jsMain/kotlin/actor/` and the base actor integration in
`src/jsMain/kotlin/actors/`.

## Runtime Layers

The code is split into two layers:

- `actor/`: runtime infrastructure.
- `actors/` and domain packages: game/business logic.

The boundary matters. `ActorKernel` is the runtime core and should stay free of Screeps room, spawn, and creep policy.
Domain actors should communicate through `ActorBase`, `ActorApi`, `MessagingApi`, and `ActorSystem` facade methods
rather than direct `ActorKernel` calls.

## Entrypoint And Tick Flow

`Main.loop()` is exported to Screeps:

```kotlin
@JsExport
fun loop() {
    Root.gameLoop()
}
```

`Root.gameLoop()`:

1. Calls `onReset()` once after VM reset.
2. Calls `preLoop()`.
3. Calls `loop()`.
4. Calls `postLoop()`.
5. Sets `wasReset = false`.

On a reset tick, `onReset()` runs before the normal tick body. That means `Actors.init()` queues `Lifecycle.Bootstrap`,
then `Actors.tick()` queues `Lifecycle.Tick`, then `ActorSystem.tick()` drains runtime work. Bootstrap is queued before
Tick on that reset tick.

`Root.postLoop()` generates a pixel when `Game.cpu.bucket >= 10000`.

## Actors.init And Actors.tick

`actors.base.Actors` is the runtime entry helper:

- `SYSTEM` is the constant actor id for the system actor.
- `init()` spawns `SystemActor("SYSTEM")` and sends `Lifecycle.Bootstrap(Game.time)`.
- `tick()` sends `Lifecycle.Tick(Game.time)` to `SYSTEM`, then calls `ActorSystem.tick()`.

`ActorSystem.spawn()` refuses duplicate ids and returns `null` when an actor already exists.

## Actor Coroutine Model

`Actor` is an abstract coroutine-backed runtime object:

- `start()` resumes the actor's `run()` coroutine.
- `run()` is implemented by subclasses or by `ActorBase`.
- `receive<T>()` suspends until a mailbox message is delivered.
- `nextTick(ticks)` suspends until `Game.time + ticks`.
- `yieldNow()` schedules a continuation in the current tick.
- `checkpoint()` yields to the next tick when the current tick is near CPU reserve.

If `run()` returns or fails, the actor destroys itself and asks `ActorSystem` to remove the actor id. `destroy()` is
idempotent and calls `onDestroy()` once.

## Mailboxes

Each actor owns a mutable FIFO mailbox.

Message flow:

1. `ActorSystem.send()` creates or receives a `Message`.
2. `ActorKernel.queueActorMessage()` checks whether the payload is a response for a pending request.
3. If it is not a pending response, the target actor's mailbox receives the message.
4. If the target actor is sleeping in `receive()` and has mail, it is enqueued in the ready queue.
5. `ActorKernel.deliverOneReadyActorMessage()` resumes one sleeping actor with one mailbox message.

Only sleeping actors are placed in the ready queue. An actor that is still running will eventually call `receive()`
again and then become eligible for delivery.

## Payload Types

The runtime payload marker hierarchy is:

- `Payload`
- `Command : Payload`
- `Request<T> : Payload`
- `Response<T> : Payload`

`Message` contains:

- `messageId`
- `from`
- `payload`

`BaseMessage` is the concrete message implementation.

## ActorBase

`ActorBase<ObjectType, CommandType, RequestType, ResponseType>` is the main base class for domain actors.

It combines:

- `Actor`
- `ActorBinding<ObjectType>`
- `ActorApi`

Its `run()` loop:

1. Receives a message.
2. Checks `isBound()`.
3. Dispatches by payload type.
4. Replies when `processRequest()` returns a response.

If the actor is no longer bound to a live Screeps object or room, `ActorBase.run()` returns. That causes normal actor
teardown through the base `Actor` continuation.

Domain actors implement:

- `processLifecycle(Lifecycle)`
- `processCommand(CommandType)`
- `processRequest(RequestType)`

`systemSend()` and `systemRequest()` route to actor id `SYSTEM`.

## Actor Bindings

Actor bindings map actor ids to Screeps objects:

- `NoBinding` for actors without a Screeps object, currently `SystemActor`.
- `GameRoomBinding(name)` for `RoomActor`.
- `GameObjectBinding(id)` for identified game objects such as spawns.
- `GameCreepBinding(name)` for `CreepActor`.

Bindings use `lazyPerTick`, so object resolution is cached within a tick and refreshed on the next tick.

## ActorSystem.tick

`ActorSystem.tick(maxSteps = 100, cpuReserve = 3.0)` is the scheduler entrypoint.

On a reset tick it attempts to restore `Memory.actorKernelSnapshot`:

- if a snapshot exists, it calls `ActorKernel.restore(snapshot)`
- if no snapshot exists, it logs a warning

Current restore behavior is intentionally incomplete. `ActorKernel.restore()` logs that actor rehydration is not
implemented and does not recreate actors.

After restore handling, `ActorSystem.tick()`:

1. resets `MessageId`
2. creates a `TickState`
3. repeatedly runs scheduler rounds until stopped or idle
4. writes `Memory.actorKernelSnapshot = ActorKernel.snapshot()`

Each scheduler round:

1. wakes actors whose `nextTick` target has arrived
2. flushes one scheduled continuation
3. delivers mailbox messages until no ready actor remains or the tick state stops

`TickState` stops when:

- delivered/flushed step count reaches `maxSteps`
- remaining CPU is less than or equal to `cpuReserve`

## Message Ids

`MessageId.resetSeed()` runs at the start of each `ActorSystem.tick()`.

New ids are formatted as:

```text
Game.time|seed
```

When replying to a request, actors use the original `messageId`. This lets `ActorKernel` match the response to the
pending continuation.

## Request Response Semantics

`ActorSystem.request()`:

1. checks that the target actor exists
2. creates a message id
3. stores a pending response continuation
4. queues the request message to the target
5. suspends the requester

Responses are normal messages whose payload implements `Response<*>`. Before queuing a response to a mailbox, the kernel
checks whether the message id is pending. If so, it schedules the requester's continuation with the response result.

Failure rules:

- if the target actor does not exist before request queueing, `ActorRequestException` is thrown
- if the target disappears while queueing, the pending continuation is failed
- if the target actor is removed before responding, pending responses targeting it are failed
- if the requester actor is removed, pending responses owned by that requester are dropped
- scheduled continuations for removed actors are dropped

Current gap: if `ActorBase` catches an exception while processing a request, it logs the exception and continues without
replying. That can leave the request continuation pending. New request handlers should be total, or the runtime should
gain an explicit request error response path before request-heavy protocols are added.

These rules are important. Silent request hangs can wedge room planning.

## ActorKernel State

`ActorKernel` owns:

- `actors`: actor id to actor instance
- `readyActorIdsQueue`: FIFO ready queue
- `readyActorIdsSet`: duplicate suppression for ready queue entries
- `sleeping`: actor id to `receive()` continuation
- `pendingWaiting`: request message id to pending response waiter
- `scheduledContinuations`: FIFO continuation queue
- `waitingNextTick`: actor id to wake tick and continuation

`ActorKernel.snapshot()` stores only actor id and actor type name. It does not store mailboxes, sleeping state,
scheduled continuations, or actor-local fields.

## Actor Removal

`ActorKernel.removeActor(actorId)`:

- calls `destroy()` on the actor if present
- removes ready queue bookkeeping
- removes receive waiters
- removes next-tick waiters
- fails pending responses where the removed actor is the target
- removes pending responses where the removed actor is the requester
- removes the actor instance

Domain `onDestroy()` hooks should perform local cleanup only. They must not assume other actors still exist.

## Intent Runtime

`ActorIntentBase` extends `ActorBase` with a small intent queue:

- intents are keyed by `intentId`
- adding an intent with an existing id replaces the stored intent and keeps the entry
- `processIntents(time)` processes up to `maxIntentsPerTick` entries, default `5`
- selection prefers never-attempted intents, then higher score, then older attempt time
- score is priority base weight plus aging every three ticks

Intent result meanings:

- `COMPLETED`: work is done; non-repeatable intents are removed
- `DROPPED`: intent is abandoned; non-repeatable intents are removed
- `RETAINED`: keep it for a later attempt

`RoomIntent` currently uses repeatable intents, so completed room intents remain available for future ticks.

## Runtime Change Rules

Be careful with changes to:

- mailbox ordering
- scheduled continuation ordering
- response matching
- actor removal behavior
- `Lifecycle.Bootstrap` and `Lifecycle.Tick` ordering
- snapshot or restore semantics
- `TickState` stop conditions

If behavior changes intentionally, document the ordering and reset impact in this file and in `AGENTS.md`.
