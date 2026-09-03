package house.edc.pocket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase12Test {
    @Test
    fun auditEntry_formatLine() {
        val entry = AuditEntry(
            kind = AuditKind.SEND_CLIP,
            identity = "Mike",
            detail = "hello",
            success = true,
        )
        assertTrue(entry.formatLine().contains("Send clip"))
        assertTrue(entry.formatLine().contains("Mike"))
    }

    @Test
    fun parseRateHintFromHealth_readsMessage() {
        val hint = parseRateHintFromHealth(
            org.json.JSONObject("""{"rate_limit_message":"Slow down","retry_after":30}"""),
        )
        assertEquals("Slow down", hint?.message)
        assertEquals(30L, hint?.retryAfterSec)
    }

    @Test
    fun featureFlags_disableClipboard() {
        val health = HostHealth(
            ok = true,
            capabilities = HostCapabilities(clipboard = true),
            featureFlags = mapOf("clipboard" to false),
        )
        assertFalse(FeatureFlags.effectiveCapabilities(health).clipboard)
        assertTrue(FeatureFlags.disabledSummary(health).isNotEmpty())
    }

    @Test
    fun auditRoundTrip_json() {
        val entries = listOf(
            AuditEntry(kind = AuditKind.SYNC, identity = "Mike", detail = "ok", success = true),
        )
        val raw = auditEntriesToJson(entries)
        val parsed = auditEntriesFromJson(raw)
        assertEquals(1, parsed.size)
        assertEquals(AuditKind.SYNC, parsed.first().kind)
    }

    @Test
    fun telemetrySummary_countsKinds() {
        val summary = telemetrySummary(
            listOf(
                TelemetryEvent(kind = TelemetryKind.SYNC_OK, detail = "a"),
                TelemetryEvent(kind = TelemetryKind.SYNC_OK, detail = "b"),
            ),
        )
        assertTrue(summary.contains("sync_ok"))
    }
}
