package house.edc.pocket

internal fun hostFailureMessage(
    settings: EdcSettings,
    cause: String?,
    stale: Boolean = false,
): String {
    if (stale) return "House host asleep — showing what we saved last."
    if (settings.baseUrl.isBlank()) return "Pick a host in Settings (or finish setup)."
    val raw = cause?.lowercase().orEmpty()
    return when {
        settings.preset == HostPreset.TAILSCALE &&
            (raw.contains("timeout") || raw.contains("unreachable") || raw.contains("failed")) ->
            "Can't reach the Away host — is Tailscale connected on this phone?"
        settings.preset == HostPreset.LAN &&
            (raw.contains("timeout") || raw.contains("connection refused") || raw.contains("failed")) ->
            "Home host isn't answering — is the house server running on Wi‑Fi?"
        settings.preset == HostPreset.CUSTOM && raw.contains("timeout") ->
            "Custom host timed out — check the URL and network."
        cause.isNullOrBlank() -> "Host unreachable"
        else -> cause
    }
}

internal fun clipEmptyMessage(search: String, filter: String): String = when {
    search.isNotBlank() -> "No clips match \"$search\"."
    filter != "All" -> "Nothing from $filter on the house clipboard yet."
    else -> "Nothing on the house clipboard — send something from the Send tab or share from another app."
}

internal fun listEmptyMessage(filter: ListPersonFilter, identity: String): String = when (filter) {
    ListPersonFilter.MINE -> "Nothing you ($identity) added to the list yet."
    ListPersonFilter.ALL -> "The shared list is empty — add milk, jobs, or notes above."
}

internal fun incomingEmptyMessage(): String =
    "No files in Incoming yet — send a photo from the Send tab or share from your gallery."
