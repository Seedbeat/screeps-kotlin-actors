package actors

import actor.message.Request

sealed class SpawnRequest<T> : Request<T>
