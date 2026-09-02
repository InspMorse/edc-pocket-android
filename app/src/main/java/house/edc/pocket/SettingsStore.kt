package house.edc.pocket

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "edc_settings")

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
) {
    val baseUrl: String
        get() {
            val raw = when (preset) {
                HostPreset.CUSTOM -> customUrl.trim().trimEnd('/')
                else -> preset.url
            }
            return if (useHttps) raw.replaceFirst("http://", "https://") else raw
        }
}

class SettingsStore(private val context: Context) {
    private val identityKey = stringPreferencesKey("identity")
    private val presetKey = stringPreferencesKey("preset")
    private val customKey = stringPreferencesKey("custom_url")
    private val clipFilterKey = stringPreferencesKey("clip_filter")
    private val autoHostKey = booleanPreferencesKey("auto_host")
    private val backgroundPollKey = stringPreferencesKey("background_poll")
    private val useHttpsKey = booleanPreferencesKey("use_https")

    val settings: Flow<EdcSettings> = context.dataStore.data.map { prefs ->
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
}
