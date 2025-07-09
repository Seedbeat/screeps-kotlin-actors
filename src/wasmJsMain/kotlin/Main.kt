import kotlin.wasm.WasmExport

/**
 * Entry point
 * is called by screeps
 *
 * must not be removed by DCE
 */

external interface Console {
    fun info(vararg o: String)
}

external val console: Console

@WasmExport
fun loop() {


    console.info("Hello from wasm!")
}
