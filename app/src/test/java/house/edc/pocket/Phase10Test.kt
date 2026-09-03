package house.edc.pocket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase10Test {
    @Test
    fun hostUrlResolver_usesActiveProfile() {
        val settings = EdcSettings(
            activeProfileId = "away",
            profiles = listOf(
                HostProfile(id = "home", name = "Home", url = "http://192.168.0.99:8765"),
                HostProfile(id = "away", name = "Away", url = "http://100.70.53.87:8765"),
            ),
        )
        assertEquals("http://100.70.53.87:8765", HostUrlResolver.baseUrl(settings))
    }

    @Test
    fun hostUrlResolver_magicDnsForTailscalePreset() {
        val settings = EdcSettings(
            preset = HostPreset.TAILSCALE,
            activeProfileId = "",
            profiles = emptyList(),
            magicDnsHost = "edc.tail123456.ts.net",
        )
        assertEquals("http://edc.tail123456.ts.net:8765", HostUrlResolver.baseUrl(settings))
    }

    @Test
    fun guestIdentity_expires() {
        val active = EdcSettings(
            guestMode = true,
            guestIdentity = "Guest",
            guestExpiresAt = System.currentTimeMillis() + 60_000L,
            identity = "Mike",
        )
        assertEquals("Guest", HostUrlResolver.effectiveIdentity(active))
        val expired = active.copy(guestExpiresAt = System.currentTimeMillis() - 1L)
        assertEquals("Mike", HostUrlResolver.effectiveIdentity(expired))
    }

    @Test
    fun parsePairQr_jsonAndUri() {
        val json = parsePairQr("""{"url":"http://house:8765","name":"Holiday"}""")
        assertEquals("http://house:8765", json?.url)
        assertEquals("Holiday", json?.name)
        val uri = parsePairQr("edc://pair?url=http%3A%2F%2Fhouse%3A8765&name=Cottage")
        assertNotNull(uri)
        assertTrue(uri!!.url.contains("house"))
    }

    @Test
    fun tlsPinNormalize_stripsPrefix() {
        assertEquals("abc123", TlsPinning.normalizePin("sha256/abc123"))
    }

    @Test
    fun hostProfile_defaultsIncludeHomeAndAway() {
        val defaults = HostProfile.defaults()
        assertEquals(2, defaults.size)
        assertTrue(defaults.any { it.id == "home" })
        assertTrue(defaults.any { it.id == "away" })
    }
}
