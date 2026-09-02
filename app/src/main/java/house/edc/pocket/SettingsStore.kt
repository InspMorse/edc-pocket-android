package house.edc.pocket

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "edc_settings")

internal fun parsePinnedSessions(raw: String): List<String> =
    raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()

internal fun formatPinnedSessions(sessions: List<String>): String =
    sessions.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString(",")

enum class HostPreset(val label: String, val url: String) {
    LAN("Home Wi-Fi", "http://192.168.0.99:8765"),
    TAILSCALE("Away", "http://100.70.53.87:8765"),
    CUSTOM("Custom", ""),
}

data class EdcSettings(
    val identity: String = "Mike",
    val preset: HostPreset = HostPreset.LAN,
    val customUrl: String = "",
    val clipFilter: String = "All",
    val autoHost: Boolean = true,
    val backgroundPoll: BackgroundPollMode = BackgroundPollMode.OFF,
    val useHttps: Boolean = false,
    val onboardingComplete: Boolean = true,
    val listSortMode: ListSortMode = ListSortMode.OPEN_FIRST,
    val listPersonFilter: ListPersonFilter = ListPersonFilter.ALL,
    val shareDestination: ShareDestination = ShareDestination.ASK,
    val skipShareChooser: Boolean = false,
    val widgetShowTodoCount: Boolean = true,
    val widgetTapAction: WidgetTapAction = WidgetTapAction.OPEN_APP,
    val persistentClipPreview: Boolean = false,
    val nfcAction: NfcAction = NfcAction.COPY_CLIP,
    val pinnedSessions: List<String> = emptyList(),
    val profiles: List<HostProfile> = HostProfile.defaults(),
    val activeProfileId: String = "home",
    val magicDnsHost: String = "",
    val homeWifiSsids: List<String> = emptyList(),
    val guestMode: Boolean = false,
    val guestIdentity: String = "",
    val guestExpiresAt: Long = 0L,
    val biometricLock: Boolean = false,
    val showDashboardTab: Boolean = false,
    val tlsPinSha256: String = "",
    val telemetryOptIn: Boolean = false,
) {
    val effectiveIdentity: String
        get() = HostUrlResolver.effectiveIdentity(this)

    val guestActive: Boolean
        get() = HostUrlResolver.guestActive(this)

    val baseUrl: String
        get() = HostUrlResolver.baseUrl(this)
}

class SettingsStore(private val context: Context) {
    private val identityKey = stringPreferencesKey("identity")
    private val presetKey = stringPreferencesKey("preset")
    private val customKey = stringPreferencesKey("custom_url")
    private val clipFilterKey = stringPreferencesKey("clip_filter")
    private val autoHostKey = booleanPreferencesKey("auto_host")
    private val backgroundPollKey = stringPreferencesKey("background_poll")
    private val useHttpsKey = booleanPreferencesKey("use_https")
    private val onboardingKey = booleanPreferencesKey("onboarding_complete")
    private val listSortKey = stringPreferencesKey("list_sort")
    private val listPersonFilterKey = stringPreferencesKey("list_person_filter")
    private val shareDestinationKey = stringPreferencesKey("share_destination")
    private val skipShareChooserKey = booleanPreferencesKey("skip_share_chooser")
    private val widgetShowTodoKey = booleanPreferencesKey("widget_show_todo")
    private val widgetTapActionKey = stringPreferencesKey("widget_tap_action")
    private val persistentPreviewKey = booleanPreferencesKey("persistent_clip_preview")
    private val nfcActionKey = stringPreferencesKey("nfc_action")
    private val pinnedSessionsKey = stringPreferencesKey("pinned_sessions")
    private val profilesKey = stringPreferencesKey("host_profiles_json")
    private val activeProfileKey = stringPreferencesKey("active_profile_id")
    private val magicDnsKey = stringPreferencesKey("magic_dns_host")
    private val homeWifiKey = stringPreferencesKey("home_wifi_ssids")
    private val guestModeKey = booleanPreferencesKey("guest_mode")
    private val guestIdentityKey = stringPreferencesKey("guest_identity")
    private val guestExpiresKey = stringPreferencesKey("guest_expires_at")
    private val biometricLockKey = booleanPreferencesKey("biometric_lock")
    private val showDashboardKey = booleanPreferencesKey("show_dashboard_tab")
    private val tlsPinKey = stringPreferencesKey("tls_pin_sha256")
    private val telemetryKey = booleanPreferencesKey("telemetry_opt_in")

    val settings: Flow<EdcSettings> = context.dataStore.data.map { prefs ->
        val legacyInstall = prefs.asMap().keys.any { it != onboardingKey }
        EdcSettings(
            identity = prefs[identityKey] ?: "Mike",
            preset = runCatching { HostPreset.valueOf(prefs[presetKey] ?: "LAN") }
                .getOrDefault(HostPreset.LAN),
            customUrl = prefs[customKey] ?: "",
            clipFilter = prefs[clipFilterKey] ?: "All",
            autoHost = prefs[autoHostKey] ?: true,
            backgroundPoll = runCatching {
                BackgroundPollMode.valueOf(prefs[backgroundPollKey] ?: "OFF")
            }.getOrDefault(BackgroundPollMode.OFF),
            useHttps = prefs[useHttpsKey] ?: false,
            onboardingComplete = prefs[onboardingKey] ?: legacyInstall,
            listSortMode = runCatching {
                ListSortMode.valueOf(prefs[listSortKey] ?: "OPEN_FIRST")
            }.getOrDefault(ListSortMode.OPEN_FIRST),
            listPersonFilter = runCatching {
                ListPersonFilter.valueOf(prefs[listPersonFilterKey] ?: "ALL")
            }.getOrDefault(ListPersonFilter.ALL),
            shareDestination = runCatching {
                ShareDestination.valueOf(prefs[shareDestinationKey] ?: "ASK")
            }.getOrDefault(ShareDestination.ASK),
            skipShareChooser = prefs[skipShareChooserKey] ?: false,
            widgetShowTodoCount = prefs[widgetShowTodoKey] ?: true,
            widgetTapAction = runCatching {
                WidgetTapAction.valueOf(prefs[widgetTapActionKey] ?: "OPEN_APP")
            }.getOrDefault(WidgetTapAction.OPEN_APP),
            persistentClipPreview = prefs[persistentPreviewKey] ?: false,
            nfcAction = runCatching {
                NfcAction.valueOf(prefs[nfcActionKey] ?: "COPY_CLIP")
            }.getOrDefault(NfcAction.COPY_CLIP),
            pinnedSessions = parsePinnedSessions(prefs[pinnedSessionsKey].orEmpty()),
            profiles = HostProfile.listFromJson(prefs[profilesKey].orEmpty())
                .ifEmpty { HostProfile.defaults() },
            activeProfileId = prefs[activeProfileKey] ?: "home",
            magicDnsHost = prefs[magicDnsKey] ?: "",
            homeWifiSsids = parsePinnedSessions(prefs[homeWifiKey].orEmpty()),
            guestMode = prefs[guestModeKey] ?: false,
            guestIdentity = prefs[guestIdentityKey] ?: "",
            guestExpiresAt = prefs[guestExpiresKey]?.toLongOrNull() ?: 0L,
            biometricLock = prefs[biometricLockKey] ?: false,
            showDashboardTab = prefs[showDashboardKey] ?: false,
            tlsPinSha256 = prefs[tlsPinKey] ?: "",
            telemetryOptIn = prefs[telemetryKey] ?: false,
        )
    }

    suspend fun setIdentity(value: String) {
        context.dataStore.edit { it[identityKey] = value }
    }

    suspend fun setPreset(value: HostPreset) {
        context.dataStore.edit { it[presetKey] = value.name }
    }

    suspend fun setCustomUrl(value: String) {
        context.dataStore.edit { it[customKey] = normalizeHostUrl(value) }
    }

    suspend fun rememberWorkingPreset(preset: HostPreset) {
        context.dataStore.edit { it[presetKey] = preset.name }
    }

    suspend fun setClipFilter(value: String) {
        context.dataStore.edit { it[clipFilterKey] = value }
    }

    suspend fun setAutoHost(value: Boolean) {
        context.dataStore.edit { it[autoHostKey] = value }
    }

    suspend fun setBackgroundPoll(value: BackgroundPollMode) {
        context.dataStore.edit { it[backgroundPollKey] = value.name }
    }

    suspend fun setUseHttps(value: Boolean) {
        context.dataStore.edit { it[useHttpsKey] = value }
    }

    suspend fun completeOnboarding() {
        context.dataStore.edit { it[onboardingKey] = true }
    }

    suspend fun setListSortMode(value: ListSortMode) {
        context.dataStore.edit { it[listSortKey] = value.name }
    }

    suspend fun setListPersonFilter(value: ListPersonFilter) {
        context.dataStore.edit { it[listPersonFilterKey] = value.name }
    }

    suspend fun setShareDestination(value: ShareDestination) {
        context.dataStore.edit { it[shareDestinationKey] = value.name }
    }

    suspend fun setSkipShareChooser(value: Boolean) {
        context.dataStore.edit { it[skipShareChooserKey] = value }
    }

    suspend fun setWidgetShowTodoCount(value: Boolean) {
        context.dataStore.edit { it[widgetShowTodoKey] = value }
    }

    suspend fun setWidgetTapAction(value: WidgetTapAction) {
        context.dataStore.edit { it[widgetTapActionKey] = value.name }
    }

    suspend fun setPersistentClipPreview(value: Boolean) {
        context.dataStore.edit { it[persistentPreviewKey] = value }
    }

    suspend fun setNfcAction(value: NfcAction) {
        context.dataStore.edit { it[nfcActionKey] = value.name }
    }

    suspend fun setPinnedSessions(value: List<String>) {
        context.dataStore.edit { it[pinnedSessionsKey] = formatPinnedSessions(value) }
    }

    suspend fun setProfiles(value: List<HostProfile>) {
        context.dataStore.edit { it[profilesKey] = HostProfile.listToJson(value) }
    }

    suspend fun setActiveProfileId(value: String) {
        context.dataStore.edit { it[activeProfileKey] = value }
    }

    suspend fun setMagicDnsHost(value: String) {
        context.dataStore.edit { it[magicDnsKey] = value.trim() }
    }

    suspend fun setHomeWifiSsids(value: List<String>) {
        context.dataStore.edit { it[homeWifiKey] = formatPinnedSessions(value) }
    }

    suspend fun setGuestMode(enabled: Boolean, identity: String, expiresAt: Long) {
        context.dataStore.edit {
            it[guestModeKey] = enabled
            it[guestIdentityKey] = identity
            it[guestExpiresKey] = expiresAt.toString()
        }
    }

    suspend fun clearGuestMode() {
        context.dataStore.edit {
            it[guestModeKey] = false
            it[guestIdentityKey] = ""
            it[guestExpiresKey] = "0"
        }
    }

    suspend fun setBiometricLock(value: Boolean) {
        context.dataStore.edit { it[biometricLockKey] = value }
    }

    suspend fun setShowDashboardTab(value: Boolean) {
        context.dataStore.edit { it[showDashboardKey] = value }
    }

    suspend fun setTlsPinSha256(value: String) {
        context.dataStore.edit { it[tlsPinKey] = TlsPinning.normalizePin(value) }
    }

    suspend fun setTelemetryOptIn(value: Boolean) {
        context.dataStore.edit { it[telemetryKey] = value }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun applyPairPayload(payload: PairQrPayload) {
        context.dataStore.edit { prefs ->
            val current = HostProfile.listFromJson(prefs[profilesKey].orEmpty())
                .ifEmpty { HostProfile.defaults() }
            val name = payload.name.ifBlank { "Paired host" }
            val id = name.lowercase().replace(Regex("[^a-z0-9]+"), "_").ifBlank { "paired" }
            val updated = current.filter { it.id != id } + HostProfile(
                id = id,
                name = name,
                url = normalizeHostUrl(payload.url),
                useHttps = payload.url.startsWith("https://", ignoreCase = true),
            )
            prefs[profilesKey] = HostProfile.listToJson(updated)
            prefs[activeProfileKey] = id
            if (payload.pinSha256.isNotBlank()) {
                prefs[tlsPinKey] = TlsPinning.normalizePin(payload.pinSha256)
            }
        }
    }
}
