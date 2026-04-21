# Project Documentation

Last source scan: 2026-04-21.

This project is a Kotlin/JS Screeps bot built around an actor runtime. The current tree is compact and actor-first.
Previous documentation mentioned legacy scheduler and role directories, but those paths are not present in this
checkout.

## Start Here

Read these in order:

1. `README.md`: this project map.
2. `actor-runtime.md`: how actors, mailboxes, requests, wakeups, and ticks work.
3. `domain-actors.md`: which actor owns which behavior and which messages are available.
4. `room-planning-and-resources.md`: room stages, workforce planning, and resource semaphores.
5. `creep-assignments.md`: assignment memory and per-creep execution.
6. `memory-and-persistence.md`: Memory schema, codecs, and reset semantics.
7. `build-and-deploy.md`: Gradle build, release, and deploy pipeline.
8. `development-guide.md`: safe change recipes, known gaps, and review checklist.

## Runtime Summary

Screeps calls the exported `loop()` in `src/jsMain/kotlin/Main.kt`.

The live path is:

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
           -> send Lifecycle.Tick(Game.time)
           -> ActorSystem.tick()
     -> Root.postLoop()
```

`EventScheduler`, `scheduler/`, `creep/roles/`, and `RoomContext.kt` do not exist in this checkout. Do not look for the
active runtime there.

## Current Behavior

The actor tree is:

```text
SYSTEM: SystemActor
  RoomActor(room.name) for every owned room
    SpawnActor(spawn.id) for every owned spawn in that room
  CreepActor(creep.name) for every creep in Game.creeps
```

Current gameplay implemented in the actor path:

- controlled room discovery
- creep actor discovery
- spawn actor discovery per room
- stale creep memory cleanup
- room stage calculation
- room planning cache calculation
- source resource semaphore sync, with definitions also created for source-adjacent containers and storage
- source lock acquire/release through room requests
- controller survival assignment
- workforce planning for energy transfer, construction, and controller progress
- worker spawning through `SpawnActor` and `Spawner`
- creep execution for harvest/work assignments

Current important gaps:

- actor snapshot restore does not rehydrate actor instances
- actor mailboxes, sleepers, and pending request continuations do not survive reset
- `SpawnRequest` and `CreepRequest` have no implemented handlers
- there are no test source files in this checkout
- room planning currently covers early workforce behavior, not full Screeps empire management

## Source Map

| Path                               | Purpose                                                        |
|------------------------------------|----------------------------------------------------------------|
| `src/jsMain/kotlin/Main.kt`        | exported Screeps entrypoint                                    |
| `src/jsMain/kotlin/Root.kt`        | top-level tick flow, reset hook, CPU logging, pixel generation |
| `src/jsMain/kotlin/actor/`         | runtime core: actors, kernel, scheduler, messages, snapshots   |
| `src/jsMain/kotlin/actors/`        | system actor, actor base classes, intents, child managers      |
| `src/jsMain/kotlin/room/`          | room actor, room commands/requests/responses, semaphores       |
| `src/jsMain/kotlin/room/planning/` | room stages, planning cache, workforce allocation              |
| `src/jsMain/kotlin/creep/`         | creep actor, assignments, assignment execution, body recipes   |
| `src/jsMain/kotlin/spawn/`         | spawn actor and spawn helper                                   |
| `src/jsMain/kotlin/memory/`        | typed Screeps Memory extensions and codecs                     |
| `src/jsMain/kotlin/room/context/`  | cached wrappers around expensive `Room.find` calls             |
| `src/jsMain/kotlin/store/`         | typed store helpers, especially energy store access            |
| `src/jsMain/kotlin/map/`           | position, terrain, and health helpers                          |
| `src/jsMain/kotlin/heuristics/`    | construction, room, and energy heuristics                      |
| `src/jsMain/kotlin/utils/`         | logging, caches, JS interop, misc helpers                      |
| `src/Constants.js`                 | Screeps constants shim used by the JS build                    |

## Ownership Rules

Keep ownership stable:

- `SystemActor` owns global runtime children.
- `RoomActor` owns room-level decisions and room-managed resources.
- `SpawnActor` executes spawn-side commands.
- `CreepActor` owns creep-local execution and cleanup.

Do not make creep actor ownership depend on the room where a creep currently stands. Use `CreepMemory.homeRoom` and
`CreepAssignment.roomName` for affiliation and planning, and use `Game.creeps[name].room.name` only as live derived
state.

## Build Summary

The project uses Kotlin Multiplatform JS with CommonJS output.

Important tasks:

```text
./gradlew build
./gradlew optimize
./gradlew release
./gradlew deploy
```

`gradle.properties` is ignored by git and may contain Screeps credentials. Do not copy credential values into
documentation or responses.
