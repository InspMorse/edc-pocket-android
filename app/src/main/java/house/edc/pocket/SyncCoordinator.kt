package house.edc.pocket

data class EndpointResponse(
    val code: Int,
    val body: String,
    val etag: String = "",
    val lastModified: String = "",
) {
    val notModified: Boolean get() = code == 304
    val ok: Boolean get() = code in 200..299 || notModified
}

data class SyncOutcome(
    val snapshot: HostSnapshot,
    val health: HostHealth? = null,
    val stale: Boolean = false,
    val lastSyncedAt: Long? = null,
    val fromNetwork: Boolean = true,
    val conflictHint: Boolean = false,
) {
    companion object {
        fun empty() = SyncOutcome(snapshot = HostSnapshot(), fromNetwork = false)
    }
}

class SyncCoordinator(
    private val client: EdcClient,
    private val cache: SyncCache,
) {
    suspend fun sync(settings: EdcSettings, healthHint: HostHealth? = null): SyncOutcome {
        val base = settings.baseUrl
        val identity = settings.identity
        if (base.isBlank()) return SyncOutcome.empty()

        val hostKey = cache.hostKey(base, identity)
        val health = healthHint ?: runCatching { client.probeHealth(base, identity) }.getOrNull()
        val supportsConditional = health?.capabilities?.conditionalFetch != false

        val clipCached = cache.getEndpoint(hostKey, SyncCache.ENDPOINT_CLIPBOARD)
        val todoCached = cache.getEndpoint(hostKey, SyncCache.ENDPOINT_TODO)
        val dropCached = cache.getEndpoint(hostKey, SyncCache.ENDPOINT_INCOMING)

        val clipResp = fetchSafe(base, "/api/clipboard", identity, clipCached, supportsConditional)
        val todoResp = fetchSafe(base, "/api/todo", identity, todoCached, supportsConditional)
        val dropResp = fetchSafe(base, "/api/incoming", identity, dropCached, supportsConditional)

        val anyNetwork = clipResp != null || todoResp != null || dropResp != null
        if (!anyNetwork) {
            val cachedSnap = cache.loadSnapshot(hostKey, base)
            return SyncOutcome(
                snapshot = cachedSnap ?: HostSnapshot(),
                health = health,
                stale = cachedSnap != null,
                lastSyncedAt = cache.getLastSynced(hostKey),
                fromNetwork = false,
            )
        }

        val now = System.currentTimeMillis()
        val clipBody = resolveBody(clipResp, clipCached)
        val todoBody = resolveBody(todoResp, todoCached)
        val dropBody = resolveBody(dropResp, dropCached)

        if (clipResp != null && !clipResp.notModified) {
            cache.putEndpoint(
                hostKey,
                SyncCache.ENDPOINT_CLIPBOARD,
                clipBody.orEmpty(),
                clipResp.etag,
                clipResp.lastModified,
                now,
            )
        }
        if (todoResp != null && !todoResp.notModified) {
            cache.putEndpoint(
                hostKey,
                SyncCache.ENDPOINT_TODO,
                todoBody.orEmpty(),
                todoResp.etag,
                todoResp.lastModified,
                now,
            )
        }
        if (dropResp != null && !dropResp.notModified) {
            cache.putEndpoint(
                hostKey,
                SyncCache.ENDPOINT_INCOMING,
                dropBody.orEmpty(),
                dropResp.etag,
                dropResp.lastModified,
                now,
            )
        }

        val clips = clipBody?.let { parseClips(it) }.orEmpty()
        val snapshot = HostSnapshot(
            latest = clips.firstOrNull(),
            history = clips,
            todos = todoBody?.let { parseTodos(it) }.orEmpty(),
            drops = dropBody?.let { parseDrops(it, base) }.orEmpty(),
        )

        val clipOk = clipResp?.ok == true || clipCached != null
        val todoOk = todoResp?.ok == true || todoCached != null
        if (!clipOk && !todoOk) {
            val cachedSnap = cache.loadSnapshot(hostKey, base)
            return SyncOutcome(
                snapshot = cachedSnap ?: HostSnapshot(),
                health = health,
                stale = cachedSnap != null,
                lastSyncedAt = cache.getLastSynced(hostKey),
                fromNetwork = false,
            )
        }

        cache.setLastSynced(hostKey, now)
        return SyncOutcome(
            snapshot = snapshot,
            health = health,
            stale = false,
            lastSyncedAt = now,
            fromNetwork = true,
        )
    }

    suspend fun loadCached(settings: EdcSettings): HostSnapshot? {
        val base = settings.baseUrl
        if (base.isBlank()) return null
        return cache.loadSnapshot(cache.hostKey(base, settings.identity), base)
    }

    suspend fun lastSyncedAt(settings: EdcSettings): Long? {
        val base = settings.baseUrl
        if (base.isBlank()) return null
        return cache.getLastSynced(cache.hostKey(base, settings.identity))
    }

    private suspend fun fetchSafe(
        base: String,
        path: String,
        identity: String,
        cached: CachedEndpoint?,
        supportsConditional: Boolean,
    ): EndpointResponse? = runCatching {
        client.fetchEndpoint(
            base = base,
            path = path,
            identity = identity,
            ifNoneMatch = if (supportsConditional) cached?.etag.orEmpty() else "",
            ifModifiedSince = if (supportsConditional) cached?.lastModified.orEmpty() else "",
        )
    }.getOrNull()

    private fun resolveBody(response: EndpointResponse?, cached: CachedEndpoint?): String? {
        if (response == null) return cached?.body
        if (response.notModified) return cached?.body
        if (response.code in 200..299) return response.body
        return cached?.body
    }
}
