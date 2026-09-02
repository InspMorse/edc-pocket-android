package house.edc.pocket

object SyncPolicy {
    fun foregroundPollMs(tab: PocketTab, health: HostHealth?, liveStreamActive: Boolean): Long {
        if (liveStreamActive) return 60_000L
        if (health?.capabilities?.push == true) return 30_000L
        return when (tab) {
            PocketTab.CLIP, PocketTab.LIST -> 5_000L
            else -> 15_000L
        }
    }

    fun effectiveBackgroundPoll(settings: EdcSettings, health: HostHealth?): BackgroundPollMode {
        if (health?.capabilities?.push == true &&
            settings.backgroundPoll == BackgroundPollMode.ACTIVE
        ) {
            return BackgroundPollMode.CONSERVATIVE
        }
        return settings.backgroundPoll
    }
}

enum class PocketTab(val label: String) {
    CLIP("Clip"),
    LIST("List"),
    SEND("Send"),
    SETTINGS("Settings"),
}
