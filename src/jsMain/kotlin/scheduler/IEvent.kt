package scheduler

interface IEvent : IEventBase {
    fun execute()
}