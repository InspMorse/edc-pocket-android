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
)

data class HostSnapshot(
    val latest: ClipEntry? = null,
    val history: List<ClipEntry> = emptyList(),
    val todos: List<TodoItem> = emptyList(),
    val drops: List<DropItem> = emptyList(),
)
