package house.edc.pocket

import android.content.Context

object PushRegistration {
    fun isConfigured(context: Context): Boolean {
        val id = context.resources.getIdentifier("google_app_id", "string", context.packageName)
        return id != 0
    }

    suspend fun registerIfPossible(
        context: Context,
        client: EdcClient,
        settings: EdcSettings,
        health: HostHealth,
    ): String? {
        if (!health.capabilities.push) return null
        if (!isConfigured(context)) return null
        val base = settings.baseUrl
        if (base.isBlank()) return null
        // Host API stub — real FCM token wiring lands when google-services.json is added.
        return runCatching {
            client.registerPushToken(base, settings.identity, token = "stub-unconfigured")
            "stub-unconfigured"
        }.getOrNull()
    }
}
