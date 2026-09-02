package house.edc.pocket

import java.util.concurrent.TimeUnit

internal fun formatStaleness(lastSyncedAt: Long?, now: Long = System.currentTimeMillis()): String? {
    if (lastSyncedAt == null || lastSyncedAt <= 0L) return null
    val ageMs = (now - lastSyncedAt).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ageMs)
    return when {
        minutes < 1 -> "cached just now"
        minutes == 1L -> "cached 1 min ago"
        minutes < 60 -> "cached ${minutes} min ago"
        else -> {
            val hours = TimeUnit.MILLISECONDS.toHours(ageMs)
            if (hours == 1L) "cached 1 hr ago" else "cached ${hours} hr ago"
        }
    }
}
