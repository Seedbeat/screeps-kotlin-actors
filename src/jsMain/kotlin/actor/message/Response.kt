package actor.message

data class Response<T>(override val result: T) : IResponse<T>
