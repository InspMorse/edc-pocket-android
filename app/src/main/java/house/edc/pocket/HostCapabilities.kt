package house.edc.pocket

data class HostCapabilities(
    val clipboard: Boolean = true,
    val todo: Boolean = true,
    val todoDelete: Boolean = true,
    val todoText: Boolean = true,
    val incoming: Boolean = true,
    val upload: Boolean = true,
    val sessionUpload: Boolean = true,
    val dashboard: Boolean = true,
) {
    fun summaryLines(): List<String> = buildList {
        add(if (clipboard) "Clipboard" else "Clipboard (off)")
        add(if (todo) "List" else "List (off)")
        if (todo) add(if (todoDelete) "Remove done" else "Remove done (off)")
        if (todo) add(if (todoText) "Share list text" else "Share list text (off)")
        add(if (incoming) "Incoming" else "Incoming (off)")
        add(if (upload) "Photo upload" else "Photo upload (off)")
        if (upload) add(if (sessionUpload) "Session folders" else "Session folders (off)")
        add(if (dashboard) "Dashboard links" else "Dashboard links (off)")
    }

    companion object {
        val ALL = HostCapabilities()
    }
}
