package actor.message

data class Request(
    val action: String,
    val args: dynamic = null
) : IRequest
