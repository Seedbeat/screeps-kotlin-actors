/**
 * Entry point
 * is called by screeps
 *
 * must not be removed by DCE
 */

external interface Console {
    fun log(vararg o: Int)
}

external val console: Console

@ExperimentalJsExport
@JsExport
fun loop(): Int {
    return 127
}
