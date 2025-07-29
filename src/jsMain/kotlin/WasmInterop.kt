@JsModule("./wasm.js")
external fun instantiateWasmJs(name: String): WasmModule

external object WasmModule {
    val instance: dynamic
    val exports: WasmExports
}

external interface WasmExports {
    fun loop(): Int
}

val wasm: WasmExports by lazy { instantiateWasmJs("wasm").exports }