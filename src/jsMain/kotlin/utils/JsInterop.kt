package utils

import utils.log.ILogging
import utils.log.LogLevel
import utils.log.Logging

object JsInterop : ILogging by Logging<JsInterop>(LogLevel.INFO) {
    fun <T> execute(code: String): T {
        log.info("Execute: '$code'")
        return eval(code).unsafeCast<T>()
    }
}