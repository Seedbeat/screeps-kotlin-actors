package scheduler

interface IMission<R, T> : IEventBase {
    fun R.execute(): T
}