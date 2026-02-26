package actor

object KernelSnapshotCodec {

    fun toRaw(snapshot: KernelSnapshot): dynamic {

        console.log("toRaw", snapshot)

        val root = js("{}")
        root.time = snapshot.time
        root.actors = snapshot.actors.map { actor ->
            val plain = js("{}")
            plain.id = actor.id
            plain.type = actor.type
            plain
        }.toTypedArray()

        return root
    }

    fun toJson(snapshot: KernelSnapshot): String {
        val raw = toRaw(snapshot)
        return JSON.stringify(raw)
    }

    fun fromRaw(raw: dynamic): KernelSnapshot = try {
        val rawActors = (raw.actors as? Array<dynamic>) ?: emptyArray()

        val snapshot = KernelSnapshot(
            time = raw.time as? Int ?: -1,
            actors = rawActors.mapNotNull { actor ->
                val id = actor.id as? String ?: return@mapNotNull null
                val type = actor.type as? String ?: return@mapNotNull null
                KernelSnapshot.ActorSnapshot(id = id, type = type)
            }
        )

        console.log("fromRaw", snapshot)

        return snapshot

    } catch (_: Throwable) {
        KernelSnapshot()
    }

    fun fromJson(json: String): KernelSnapshot = try {
        val raw = JSON.parse<dynamic>(json)
        fromRaw(raw)
    } catch (_: Throwable) {
        KernelSnapshot()
    }
}
