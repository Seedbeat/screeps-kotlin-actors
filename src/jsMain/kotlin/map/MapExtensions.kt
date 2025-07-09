package map

import screeps.api.Flag
import screeps.api.Game
import screeps.api.get

val waitPosition: Flag? get() = Game.flags["PausePoint"]