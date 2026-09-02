package house.edc.pocket

enum class ListSortMode(val label: String) {
    OPEN_FIRST("Open first"),
    BY_DATE("Newest"),
    BY_PERSON("By person"),
}

enum class ListPersonFilter(val label: String) {
    ALL("All"),
    MINE("Mine"),
}

internal fun sortTodos(
    todos: List<TodoItem>,
    mode: ListSortMode,
    pinnedIds: Set<String>,
    identity: String,
): List<TodoItem> {
    fun pinRank(id: String) = if (id in pinnedIds) 0 else 1
    val byNewest = compareByDescending<TodoItem> { it.ts.ifBlank { "" } }
    return when (mode) {
        ListSortMode.OPEN_FIRST -> todos.sortedWith(
            compareBy<TodoItem>({ pinRank(it.id) }, { it.done }).then(byNewest),
        )
        ListSortMode.BY_DATE -> todos.sortedWith(
            compareBy<TodoItem>({ pinRank(it.id) }).then(byNewest),
        )
        ListSortMode.BY_PERSON -> todos.sortedWith(
            compareBy<TodoItem>({ pinRank(it.id) }, { it.from.lowercase() }, { it.done })
                .then(byNewest),
        )
    }
}

internal fun filterTodosByPerson(
    todos: List<TodoItem>,
    filter: ListPersonFilter,
    identity: String,
): List<TodoItem> = when (filter) {
    ListPersonFilter.ALL -> todos
    ListPersonFilter.MINE -> todos.filter { it.from.equals(identity, ignoreCase = true) || it.from.isBlank() }
}

internal fun sortClipHistory(
    history: List<ClipEntry>,
    pinnedKeys: Set<String>,
): List<ClipEntry> = history.sortedWith(
    compareBy<ClipEntry>({ PinStore.clipKey(it) !in pinnedKeys })
        .thenByDescending { it.ts.ifBlank { "" } },
)

internal fun clipEntriesSorted(
    latest: ClipEntry?,
    history: List<ClipEntry>,
    pinnedKeys: Set<String>,
): Pair<ClipEntry?, List<ClipEntry>> {
    val sortedHistory = sortClipHistory(history, pinnedKeys)
    val pinnedLatest = latest?.takeIf { PinStore.clipKey(it) in pinnedKeys }
    return latest to sortedHistory
}
