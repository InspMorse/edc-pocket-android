package house.edc.pocket

object HostLinks {
    fun clipDashboardUrl(health: HostHealth?, entry: ClipEntry): String? {
        if (health?.capabilities?.dashboard == false) return null
        val templates = health?.linkTemplates ?: return null
        return buildItemUrl(
            base = health.dashboardUrl.ifBlank { templates.dashboardBase },
            template = templates.clipboardItem,
            id = entry.id,
            fallbackPath = "clipboard/${entry.id.encode()}",
        )
    }

    fun todoDashboardUrl(health: HostHealth?, item: TodoItem): String? {
        if (health?.capabilities?.dashboard == false) return null
        val templates = health?.linkTemplates ?: return null
        return buildItemUrl(
            base = health.dashboardUrl.ifBlank { templates.dashboardBase },
            template = templates.todoItem,
            id = item.id,
            fallbackPath = "todo/${item.id.encode()}",
        )
    }

    private fun buildItemUrl(
        base: String,
        template: String,
        id: String,
        fallbackPath: String,
    ): String? {
        val root = base.trim().trimEnd('/')
        if (root.isBlank() || id.isBlank()) return null
        if (template.isNotBlank()) {
            val path = template
                .replace("{id}", id.encode())
                .replace("{ID}", id.encode())
            return when {
                path.startsWith("http") -> path
                path.startsWith("/") -> root + path
                else -> "$root/$path"
            }
        }
        return "$root/#/$fallbackPath"
    }

    private fun String.encode(): String = java.net.URLEncoder.encode(this, "UTF-8")
}
