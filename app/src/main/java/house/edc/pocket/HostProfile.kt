package house.edc.pocket

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class HostProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val useHttps: Boolean = false,
) {
    internal fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("url", url)
        put("useHttps", useHttps)
    }

    companion object {
        fun defaults(): List<HostProfile> = listOf(
            HostProfile(id = "home", name = "Home", url = HostPreset.LAN.url),
            HostProfile(id = "away", name = "Away", url = HostPreset.TAILSCALE.url),
        )

        internal fun fromJson(obj: JSONObject): HostProfile = HostProfile(
            id = obj.optString("id", UUID.randomUUID().toString()),
            name = obj.optString("name"),
            url = obj.optString("url"),
            useHttps = obj.optBoolean("useHttps", false),
        )

        internal fun listFromJson(raw: String): List<HostProfile> {
            if (raw.isBlank()) return emptyList()
            val arr = JSONArray(raw)
            return buildList {
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { add(fromJson(it)) }
                }
            }
        }

        internal fun listToJson(profiles: List<HostProfile>): String {
            val arr = JSONArray()
            profiles.forEach { arr.put(it.toJson()) }
            return arr.toString()
        }
    }
}

data class DiscoveredHost(
    val name: String,
    val baseUrl: String,
    val source: String,
)

object HostUrlResolver {
    fun baseUrl(settings: EdcSettings): String {
        val profile = settings.activeProfileId.takeIf { it.isNotBlank() }
            ?.let { id -> settings.profiles.find { it.id == id } }
            ?.takeIf { it.url.isNotBlank() }
        val raw = when {
            settings.preset == HostPreset.CUSTOM && settings.customUrl.isNotBlank() ->
                settings.customUrl.trim().trimEnd('/')
            profile != null -> profile.url.trim().trimEnd('/')
            settings.preset == HostPreset.TAILSCALE && settings.magicDnsHost.isNotBlank() -> {
                val host = settings.magicDnsHost.trim().trimEnd('/')
                if (host.contains("://")) host else "http://$host:8765"
            }
            settings.preset == HostPreset.CUSTOM -> settings.customUrl.trim().trimEnd('/')
            else -> settings.preset.url
        }
        val useHttps = profile?.useHttps == true || settings.useHttps
        return if (useHttps) raw.replaceFirst("http://", "https://") else raw
    }

    fun effectiveIdentity(settings: EdcSettings): String {
        if (settings.guestMode &&
            settings.guestExpiresAt > System.currentTimeMillis() &&
            settings.guestIdentity.isNotBlank()
        ) {
            return settings.guestIdentity
        }
        return settings.identity
    }

    fun guestActive(settings: EdcSettings): Boolean =
        settings.guestMode &&
            settings.guestExpiresAt > System.currentTimeMillis() &&
            settings.guestIdentity.isNotBlank()
}
