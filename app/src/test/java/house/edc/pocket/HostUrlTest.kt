package house.edc.pocket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HostUrlTest {
    @Test
    fun acceptsLanUrl() {
        assertNull(validateHostUrl("http://192.168.0.99:8765"))
    }

    @Test
    fun rejectsMissingScheme() {
        assertEquals("Use http:// or https://", validateHostUrl("192.168.0.99:8765"))
    }

    @Test
    fun rejectsEmpty() {
        assertEquals("URL cannot be empty", validateHostUrl("   "))
    }

    @Test
    fun normalizesTrailingSlash() {
        assertEquals("http://192.168.0.99:8765", normalizeHostUrl("http://192.168.0.99:8765/"))
    }

    @Test
    fun tailscaleHintOnAwayFailure() {
        val settings = EdcSettings(preset = HostPreset.TAILSCALE)
        assertTrue(hostFailureMessage(settings, "timeout").contains("Tailscale"))
    }
}
