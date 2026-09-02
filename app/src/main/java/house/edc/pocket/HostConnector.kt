package house.edc.pocket

class HostConnector(
    private val client: EdcClient,
) {
    fun preferredPresets(settings: EdcSettings, network: NetworkKind): List<HostPreset> {
        if (settings.preset == HostPreset.CUSTOM || !settings.autoHost) return emptyList()
        return when (network) {
            NetworkKind.WIFI -> listOf(HostPreset.LAN, HostPreset.TAILSCALE)
            NetworkKind.VPN,
            NetworkKind.CELLULAR,
            -> listOf(HostPreset.TAILSCALE, HostPreset.LAN)
            NetworkKind.OTHER -> listOf(HostPreset.LAN, HostPreset.TAILSCALE)
            NetworkKind.NONE -> emptyList()
        }
    }

    suspend fun syncHost(
        settings: EdcSettings,
        store: SettingsStore,
        network: NetworkKind,
    ): ReachableHost? {
        val order = preferredPresets(settings, network)
        if (order.isEmpty()) return null
        for (preset in order) {
            val health = runCatching {
                client.probeHealth(preset.url, settings.identity)
            }.getOrNull() ?: continue
            if (!health.ok) continue
            if (settings.preset != preset) store.rememberWorkingPreset(preset)
            return ReachableHost(preset, preset.url, health)
        }
        return null
    }

    suspend fun reprobe(settings: EdcSettings): HostHealth? {
        val base = settings.baseUrl
        if (base.isBlank()) return null
        return runCatching { client.probeHealth(base, settings.identity) }.getOrNull()
    }
}
