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
