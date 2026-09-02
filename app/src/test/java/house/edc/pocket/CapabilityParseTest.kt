package house.edc.pocket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityParseTest {
    @Test
    fun capabilitiesFromObjectFlags() {
        val caps = parseCapabilitiesJson(
            """
            {
              "capabilities": {
                "clipboard": true,
                "todo": true,
                "todo_delete": false,
                "incoming": false,
                "upload": true,
                "session": false,
                "dashboard": true
              }
            }
            """.trimIndent(),
        )
        assertTrue(caps.clipboard)
        assertTrue(caps.todo)
        assertFalse(caps.todoDelete)
        assertFalse(caps.incoming)
        assertTrue(caps.upload)
        assertFalse(caps.sessionUpload)
        assertTrue(caps.dashboard)
    }

    @Test
    fun capabilitiesFromFeatureList() {
        val caps = parseCapabilitiesJson(
            """{"features":["clipboard","todo","dashboard"]}""",
        )
        assertTrue(caps.clipboard)
        assertTrue(caps.todo)
        assertFalse(caps.incoming)
        assertFalse(caps.upload)
        assertTrue(caps.dashboard)
    }

    @Test
    fun knownUsersFromHealth() {
        val health = parseHealth(
            """{"ok":true,"users":["Mike","Mhairi","Guest"]}""",
        )
        assertEquals(listOf("Mike", "Mhairi", "Guest"), health.knownUsers)
    }

    @Test
    fun linkTemplatesFromHealth() {
        val health = parseHealth(
            """
            {
              "ok": true,
              "dashboard_url": "http://192.168.0.99:8765/",
              "links": {
                "clipboard": "/#/clip/{id}",
                "todo": "/#/todo/{id}"
              }
            }
            """.trimIndent(),
        )
        assertEquals("http://192.168.0.99:8765/", health.dashboardUrl)
        assertEquals("/#/clip/{id}", health.linkTemplates.clipboardItem)
        assertEquals("/#/todo/{id}", health.linkTemplates.todoItem)
    }
}

class HostLinksTest {
    @Test
    fun clipDashboardUrlUsesTemplate() {
        val health = HostHealth(
            ok = true,
            dashboardUrl = "http://192.168.0.99:8765",
            linkTemplates = HostLinkTemplates(clipboardItem = "/#/clip/{id}"),
        )
        val url = HostLinks.clipDashboardUrl(
            health,
            ClipEntry(id = "abc-123", text = "hi", from = "Mike", ts = ""),
        )
        assertEquals("http://192.168.0.99:8765/#/clip/abc-123", url)
    }

    @Test
    fun todoDashboardUrlFallbackPath() {
        val health = HostHealth(
            ok = true,
            dashboardUrl = "http://192.168.0.99:8765/",
        )
        val url = HostLinks.todoDashboardUrl(
            health,
            TodoItem(id = "t1", text = "milk", done = false, from = "Mike", ts = ""),
        )
        assertEquals("http://192.168.0.99:8765/#/todo/t1", url)
    }

    @Test
    fun dashboardDisabledReturnsNull() {
        val health = HostHealth(
            ok = true,
            dashboardUrl = "http://192.168.0.99:8765/",
            capabilities = HostCapabilities(dashboard = false),
        )
        assertEquals(
            null,
            HostLinks.clipDashboardUrl(
                health,
                ClipEntry(id = "1", text = "x", from = "", ts = ""),
            ),
        )
    }
}
