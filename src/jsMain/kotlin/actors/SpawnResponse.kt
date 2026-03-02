package actors

import actor.message.IResponse

sealed class SpawnResponse<T> : IResponse<T> {

    data class PopulationResponse(
        override val result: Int
    ) : SpawnResponse<Int>()
}
