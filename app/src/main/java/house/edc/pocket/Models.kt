package house.edc.pocket

data class ClipEntry(
    val id: String,
    val text: String,
    val from: String,
    val ts: String,
)

data class TodoItem(
    val id: String,
    val text: String,
    val done: Boolean,
    val from: String,
    val ts: String,
    val note: String = "",
    val dueDate: String = "",
    val category: String = "",
    val linkedClipUrl: String = "",
    val linkedClipText: String = "",
    val subItems: List<TodoSubItem> = emptyList(),
    val recurrence: String = "",
)

data class DropItem(
    val id: String,
    val name: String,
    val from: String,
    val ts: String,
    val size: Long,
    val path: String = "",
) {
    fun isImage(): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
            lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".heic")
    }

    fun isVideo(): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".webm") ||
            lower.endsWith(".mkv") || lower.endsWith(".m4v")
    }

    fun isPdf(): Boolean = name.lowercase().endsWith(".pdf")

    fun isAudio(): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".wav") ||
            lower.endsWith(".ogg") || lower.endsWith(".aac")
    }

    fun sessionFolder(): String {
        val full = path.ifBlank { name }.replace('\\', '/')
        val marker = Regex("Sessions/", RegexOption.IGNORE_CASE).find(full)
        if (marker != null) {
            val after = full.substring(marker.range.last + 1)
            val slash = after.indexOf('/')
            return if (slash > 0) after.substring(0, slash) else after
        }
        val slash = full.indexOf('/')
        return if (slash > 0) full.substring(0, slash) else ""
    }
}

data class UploadProgress(
    val done: Int,
    val total: Int,
)

data class HostSnapshot(
    val latest: ClipEntry? = null,
    val history: List<ClipEntry> = emptyList(),
    val todos: List<TodoItem> = emptyList(),
    val drops: List<DropItem> = emptyList(),
)

data class HostHealth(
    val ok: Boolean,
    val version: String = "",
    val hostName: String = "",
    val dashboardUrl: String = "",
    val capabilities: HostCapabilities = HostCapabilities.ALL,
    val knownUsers: List<String> = emptyList(),
    val linkTemplates: HostLinkTemplates = HostLinkTemplates(),
    val themeAccent: String = "",
    val logoUrl: String = "",
    val tlsPinSha256: String = "",
) {
    fun summary(): String {
        if (!ok) return "Host unreachable"
        val bits = listOfNotNull(
            hostName.takeIf { it.isNotBlank() },
            version.takeIf { it.isNotBlank() }?.let { "v$it" },
        )
        return if (bits.isEmpty()) "Host reached." else bits.joinToString(" · ")
    }
}

data class ReachableHost(
    val preset: HostPreset?,
    val baseUrl: String,
    val health: HostHealth,
)
