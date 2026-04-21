# screeps-kotlin-actors

Kotlin/JS Screeps bot built around an actor-first runtime.

This project is at an early starting point and is still in active progress.

Screeps calls the exported `loop()`, the root runtime sends lifecycle messages, and domain actors coordinate rooms,
spawns, creeps, planning, assignments, and resource locks through explicit protocols.

## What It Does

- Discovers owned rooms, creeps, and spawns.
- Tracks room stage, planning cache, and early workforce demand.
- Assigns workers to controller upkeep, controller progress, construction, and energy transfer.
- Coordinates source locks through room-owned resource semaphores.
- Spawns and drives worker creeps through actor-owned behavior.
- Cleans up stale creep memory when creeps disappear.

The current bot focuses on early-room automation and a clear actor architecture. It is not a complete Screeps empire
manager yet.

## How It Works

The project centers on a small actor system. Actors are stable runtime units with explicit ownership, mailboxes, and
lifecycle messages. Each tick, the runtime wakes actors, delivers queued work, and lets domain actors cooperate without
sharing mutable state directly.

Game concepts map to actors: the system actor owns global discovery, room actors own room-level policy, spawn actors own
spawn execution, and creep actors own creep-local behavior.

## Tick Flow

```text
Main.loop()
  -> Root.gameLoop()
     -> Root.onReset() on the first tick after VM reset
        -> Actors.init()
           -> spawn SystemActor("SYSTEM")
           -> send Lifecycle.Bootstrap(Game.time)
     -> Root.preLoop()
     -> Root.loop()
        -> Actors.tick()
           -> send Lifecycle.Tick(Game.time) to SYSTEM
           -> ActorSystem.tick()
     -> Root.postLoop()
```

```text
SYSTEM: SystemActor
  RoomActor(room.name)
    SpawnActor(spawn.id)
  CreepActor(creep.name)
```

Creep actors are global. Their ownership does not depend on the room where the creep currently stands.

## Docs

Start with [docs/README.md](docs/README.md). The deeper docs cover:

- actor runtime and scheduling
- domain actor ownership
- room planning and resource locks
- creep assignments
- Memory persistence and reset behavior
- development guidelines

## Tech

- Kotlin Multiplatform JS `2.3.10`
- CommonJS Screeps output
- Screeps Kotlin types `2.2.0`
- kotlinx.coroutines `1.10.2`
