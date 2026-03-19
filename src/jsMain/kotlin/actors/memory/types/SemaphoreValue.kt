package actors.memory.types

import kotlinx.js.JsPlainObject

@JsPlainObject
external interface SemaphoreValue {
    var current: Int
    var maximum: Int
}