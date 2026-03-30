package actors

import actor.message.Response

sealed interface SpawnResponse<T> : Response<T>
