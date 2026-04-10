package spawn

import actor.message.Request

sealed interface SpawnRequest<T> : Request<T>
