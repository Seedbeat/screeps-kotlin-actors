package actor.enums

enum class QueueMessageResult {
    SCHEDULED_RESPONSE,
    QUEUED_TO_MAILBOX,
    ACTOR_NOT_FOUND
}