package house.edc.pocket

import android.content.Context

data class CachedClip(
    val text: String,
    val from: String,
    val ts: String,
    val fingerprint: String,
)

class LatestClipStore(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun peek(): CachedClip? {
        val text = prefs.getString(KEY_TEXT, null) ?: return null
        if (text.isBlank()) return null
        return CachedClip(
            text = text,
            from = prefs.getString(KEY_FROM, "").orEmpty(),
            ts = prefs.getString(KEY_TS, "").orEmpty(),
            fingerprint = prefs.getString(KEY_FP, "").orEmpty(),
        )
    }

    fun save(entry: ClipEntry) {
        val fp = fingerprint(entry)
        prefs.edit()
            .putString(KEY_TEXT, entry.text)
            .putString(KEY_FROM, entry.from)
            .putString(KEY_TS, entry.ts)
            .putString(KEY_FP, fp)
            .apply()
    }

    fun lastNotifiedFingerprint(): String = prefs.getString(KEY_NOTIFIED_FP, "").orEmpty()

    fun setLastNotifiedFingerprint(fingerprint: String) {
        prefs.edit().putString(KEY_NOTIFIED_FP, fingerprint).apply()
    }

    fun fingerprint(entry: ClipEntry): String = "${entry.id}|${entry.text}|${entry.ts}"

    companion object {
        private const val PREFS = "latest_clip"
        private const val KEY_TEXT = "text"
        private const val KEY_FROM = "from"
        private const val KEY_TS = "ts"
        private const val KEY_FP = "fp"
        private const val KEY_NOTIFIED_FP = "notified_fp"
    }
}
