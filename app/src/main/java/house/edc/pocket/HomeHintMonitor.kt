package house.edc.pocket

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build

class HomeHintMonitor(context: Context) {
    private val app = context.applicationContext
    private val wifi = app.getSystemService(WifiManager::class.java)

    fun currentSsid(): String? {
        val info = wifi.connectionInfo ?: return null
        val raw = info.ssid?.trim().orEmpty()
        if (raw.isBlank() || raw == "<unknown ssid>") return null
        return raw.trim('"')
    }

    fun isAtHome(settings: EdcSettings): Boolean {
        if (settings.homeWifiSsids.isEmpty()) return false
        val ssid = currentSsid() ?: return false
        return settings.homeWifiSsids.any { it.equals(ssid, ignoreCase = true) }
    }

    fun hintLabel(settings: EdcSettings): String? = when {
        isAtHome(settings) -> "At home Wi‑Fi"
        settings.preset == HostPreset.TAILSCALE -> "Away · Tailscale"
        settings.preset == HostPreset.LAN -> "Home LAN"
        else -> null
    }
}
