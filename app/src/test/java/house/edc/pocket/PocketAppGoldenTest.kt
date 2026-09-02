package house.edc.pocket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PocketAppGoldenTest {
    @Test
    fun navTabLabels_goldenBaseline() {
        assertEquals("Clip", PocketTab.CLIP.label)
        assertEquals("List", PocketTab.LIST.label)
        assertEquals("Send", PocketTab.SEND.label)
        assertEquals("Settings", PocketTab.SETTINGS.label)
    }

    @Test
    fun auditKinds_coverSendPath() {
        val labels = AuditKind.entries.map { it.label }
        assertTrue(labels.contains("Send clip"))
        assertTrue(labels.contains("Outbox"))
        assertTrue(labels.contains("Rate limit"))
    }

    @Test
    fun trustDiagnostics_copyFormat_nonempty() {
        val line = AuditEntry(
            kind = AuditKind.SYNC,
            identity = "Mike",
            detail = "refresh ok",
            success = true,
        ).formatLine()
        assertTrue(line.contains("Sync"))
        assertTrue(line.contains("Mike"))
    }
}
