package actors

import actor.message.Response

sealed class SpawnResponse<T> : Response<T> {

    data class PopulationResponse(
        override val result: Int
    ) : SpawnResponse<Int>()
}
