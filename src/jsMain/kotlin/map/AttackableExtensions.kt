package map

import screeps.api.Attackable
import kotlin.math.roundToInt

val Attackable.health: Int get() = ((hits.toFloat() / hitsMax.toFloat()) * 100f).roundToInt()