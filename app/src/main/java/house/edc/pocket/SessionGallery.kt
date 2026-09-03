package house.edc.pocket

data class SessionGroup(
    val folder: String,
    val drops: List<DropItem>,
)

internal fun groupDropsBySession(drops: List<DropItem>): List<SessionGroup> {
    val (withSession, loose) = drops.partition { it.sessionFolder().isNotBlank() }
    val groups = withSession.groupBy { it.sessionFolder() }
        .map { (folder, items) ->
            SessionGroup(folder, items.sortedByDescending { it.ts.ifBlank { it.name } })
        }
        .sortedByDescending { it.drops.firstOrNull()?.ts.orEmpty() }
    return if (loose.isEmpty()) {
        groups
    } else {
        groups + SessionGroup("", loose)
    }
}
