package house.edc.pocket

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
internal fun TrustDiagnosticsSection(
    settings: EdcSettings,
    auditEntries: List<AuditEntry>,
    telemetrySummaryText: String,
    rateLimitHint: HostRateHint?,
    featureFlagNotes: List<String>,
    onTelemetryOptIn: (Boolean) -> Unit,
    onExportData: suspend () -> Unit,
    onExportAudit: () -> Unit,
    onClearAudit: () -> Unit,
    onResetApp: suspend () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmReset by remember { mutableStateOf(false) }
    SectionLabel("Trust & diagnostics")
    Text(
        text = "Diagnose “why didn’t my send arrive?” from audit logs — no adb required.",
        style = MaterialTheme.typography.bodySmall,
        color = EdcMuted,
    )
    rateLimitHint?.let { hint ->
        Text(
            text = hint.message +
                if (hint.retryAfterSec > 0) " · retry in ${hint.retryAfterSec}s" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
    featureFlagNotes.forEach { line ->
        Text(text = line, style = MaterialTheme.typography.bodySmall, color = EdcMuted)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Anonymised telemetry", fontWeight = FontWeight.Medium)
            Text(
                text = "Local-only counts of connect/sync/outbox outcomes. Never leaves the phone unless you export.",
                style = MaterialTheme.typography.bodySmall,
                color = EdcMuted,
            )
            Text(
                text = telemetrySummaryText,
                style = MaterialTheme.typography.labelSmall,
                color = EdcMuted,
            )
        }
        Switch(checked = settings.telemetryOptIn, onCheckedChange = onTelemetryOptIn)
    }
    SectionLabel("Audit log (${auditEntries.size})")
    auditEntries.takeLast(8).reversed().forEach { entry ->
        Text(
            text = entry.formatLine(),
            style = MaterialTheme.typography.labelSmall,
            color = if (entry.success) EdcMuted else MaterialTheme.colorScheme.error,
        )
    }
    OutlinedButton(onClick = onExportAudit, modifier = Modifier.fillMaxWidth()) {
        Text("Copy audit log")
    }
    OutlinedButton(onClick = onClearAudit, modifier = Modifier.fillMaxWidth()) {
        Text("Clear audit log")
    }
    Button(
        onClick = { scope.launch { onExportData() } },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Export settings + cache summary")
    }
    OutlinedButton(
        onClick = { confirmReset = true },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Clear all app data")
    }
    Text(
        text = "Reset cancels background workers, clears cache, outbox, pins, and settings.",
        style = MaterialTheme.typography.bodySmall,
        color = EdcMuted,
    )
    Text(
        text = "Beta builds: sideload app-release.apk or use Play internal testing — see BETA.md.",
        style = MaterialTheme.typography.bodySmall,
        color = EdcMuted,
    )
    Text(
        text = "Host API policy: HOST_API.md · Security cadence: SECURITY.md",
        style = MaterialTheme.typography.bodySmall,
        color = EdcMuted,
    )
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Clear all app data?") },
            text = { Text("This removes cached clips, list, settings, and queued sends on this phone.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmReset = false
                        scope.launch { onResetApp() }
                    },
                ) { Text("Clear everything") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            },
        )
    }
}

internal fun copyAuditLog(context: android.content.Context, entries: List<AuditEntry>) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText("EDC audit", formatAuditExport(entries)))
}
