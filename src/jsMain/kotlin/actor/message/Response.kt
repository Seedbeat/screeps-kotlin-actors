package actor.message

interface Response<T> : Payload {
    val result: T
}