package actor

class ActorRequestException(
    actorId: String,
    reason: String
) : IllegalStateException("Actor request failed for '$actorId': $reason")
