package house.edc.pocket

data class EndpointCheck(
    val name: String,
    val path: String,
    val ok: Boolean,
    val code: Int,
    val latencyMs: Long,
    val detail: String = "",
)

data class ConnectionReport(
    val hostUrl: String,
    val identity: String,
    val checks: List<EndpointCheck>,
    val generatedAt: Long = System.currentTimeMillis(),
) {
    fun allOk(): Boolean = checks.all { it.ok }
}

class ConnectionDoctor(
    private val client: EdcClient,
) {
    suspend fun run(settings: EdcSettings): ConnectionReport {
        val base = settings.baseUrl
        val identity = settings.effectiveIdentity
        if (base.isBlank()) {
            return ConnectionReport(
                hostUrl = "",
                identity = identity,
                checks = listOf(
                    EndpointCheck(
                        name = "Host URL",
                        path = "",
                        ok = false,
                        code = 0,
                        latencyMs = 0,
                        detail = "No host URL configured",
                    ),
                ),
            )
        }

        val endpoints = listOf(
            "Health" to "/api/health",
            "Capabilities" to "/api/capabilities",
            "Clipboard" to "/api/clipboard",
            "List" to "/api/todo",
            "Incoming" to "/api/incoming",
        )
        val checks = endpoints.map { (name, path) ->
            val start = System.nanoTime()
            val response = runCatching {
                client.fetchEndpoint(base, path, identity)
            }.getOrNull()
            val latencyMs = (System.nanoTime() - start) / 1_000_000
            if (response == null) {
                EndpointCheck(
                    name = name,
                    path = path,
                    ok = false,
                    code = 0,
                    latencyMs = latencyMs,
                    detail = "Request failed",
                )
            } else {
                EndpointCheck(
                    name = name,
                    path = path,
                    ok = response.code in 200..299,
                    code = response.code,
                    latencyMs = latencyMs,
                    detail = when {
                        response.notModified -> "Not modified (304)"
                        response.etag.isNotBlank() -> "ETag ${response.etag}"
                        else -> ""
                    },
                )
            }
        }
        return ConnectionReport(hostUrl = base, identity = identity, checks = checks)
    }

    fun exportLog(report: ConnectionReport): String = buildString {
        appendLine("EDC pocket connection doctor")
        appendLine("Generated: ${report.generatedAt}")
        appendLine("Host: ${report.hostUrl}")
        appendLine("Identity: ${report.identity}")
        appendLine()
        report.checks.forEach { check ->
            appendLine(
                "${check.name} ${check.path} — ${check.code} ${check.latencyMs}ms" +
                    if (check.detail.isNotBlank()) " (${check.detail})" else "",
            )
        }
        appendLine()
        appendLine(if (report.allOk()) "All endpoints OK" else "Some endpoints failed")
    }
}
