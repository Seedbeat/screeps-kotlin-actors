package actor

import actors.base.ICodecJson
import actors.base.ICodecRaw

object KernelSnapshotCodec : ICodecRaw<KernelSnapshot>, ICodecJson<KernelSnapshot> {

    override fun serializeRaw(data: KernelSnapshot): dynamic {

//        console.log("toRaw", snapshot)

        val root = js("{}")
        root.time = data.time
        root.actors = data.actors.map { actor ->
            val plain = js("{}")
            plain.id = actor.id
            plain.type = actor.type
            plain
        }.toTypedArray()

        return root
    }

    override fun deserializeRaw(raw: dynamic): KernelSnapshot = try {
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

    override fun serializeJson(data: KernelSnapshot): String {
        val raw = serializeRaw(data)
        return JSON.stringify(raw)
    }

    override fun deserializeJson(json: String): KernelSnapshot = try {
        val raw = JSON.parse<dynamic>(json)
        deserializeRaw(raw)
    } catch (_: Throwable) {
        KernelSnapshot()
    }
}
