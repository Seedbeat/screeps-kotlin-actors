@file:Suppress("NOTHING_TO_INLINE")

package utils

import kotlin.Number

object Number {
    inline fun <T : Number> T.toFixed(digits: Int): T = this.asDynamic().toFixed(digits)
}