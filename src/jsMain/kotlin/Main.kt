/**
 * Entry point
 * is called by screeps
 *
 * must not be removed by DCE
 */
@ExperimentalJsExport
@JsExport
fun loop() {
    Root.gameLoop()
}