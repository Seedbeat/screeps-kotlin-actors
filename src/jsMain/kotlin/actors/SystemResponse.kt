package actors

import actor.message.Response
import creep.CreepStatus

sealed interface SystemResponse<T> : Response<T> {
    sealed class Query<T> : SystemResponse<T> {
        data class CreepsResponse(override val result: List<CreepStatus>) : SystemResponse<List<CreepStatus>>
    }
}
