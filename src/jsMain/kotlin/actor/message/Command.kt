package actor.message

data class Command(
    val action: String,
    val args: dynamic = null
) : ICommand
