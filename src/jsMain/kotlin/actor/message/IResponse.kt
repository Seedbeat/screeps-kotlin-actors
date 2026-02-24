package actor.message

interface IResponse<T> : IPayload {
    val result: T
}