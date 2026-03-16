package actors.base

data class IntentStore(
    val queue: List<Intent> = emptyList()
)

