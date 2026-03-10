package actors.base

interface ICodecJson<T> {
    fun serializeJson(data: T): String
    fun deserializeJson(json: String): T
}