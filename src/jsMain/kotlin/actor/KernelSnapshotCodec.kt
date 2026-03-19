package actor

import actors.base.Codec
import utils.Object

object KernelSnapshotCodec : Codec<KernelSnapshot> {

    override fun serialize(data: KernelSnapshot): dynamic {

//        console.log("toRaw", snapshot)

        val root = Object.plain {
            time = data.time
            actors = data.actors.map { actor ->
                Object.plain {
                    id = actor.id
                    type = actor.type
                }
            }.toTypedArray()
        }

        return root
    }

    override fun deserialize(raw: dynamic): KernelSnapshot = try {
        val rawActors = (raw.actors as? Array<dynamic>) ?: emptyArray()

        val snapshot = KernelSnapshot(
            time = raw.time as? Int ?: -1,
            actors = rawActors.mapNotNull { actor ->
                val id = actor.id as? String ?: return@mapNotNull null
                val type = actor.type as? String ?: return@mapNotNull null
                KernelSnapshot.ActorSnapshot(id = id, type = type)
            }
        )

//        console.log("fromRaw", snapshot)

        return snapshot

    } catch (_: Throwable) {
        KernelSnapshot()
    }
}
