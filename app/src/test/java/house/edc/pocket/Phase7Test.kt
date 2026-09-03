package house.edc.pocket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase7Test {
    @Test
    fun sortTodosPinnedFirst() {
        val todos = listOf(
            TodoItem("a", "one", done = false, from = "Mike", ts = "2026-01-01"),
            TodoItem("b", "two", done = false, from = "Mhairi", ts = "2026-01-02"),
        )
        val sorted = sortTodos(todos, ListSortMode.BY_DATE, pinnedIds = setOf("a"), identity = "Mike")
        assertEquals("a", sorted.first().id)
    }

    @Test
    fun filterTodosByPerson() {
        val todos = listOf(
            TodoItem("1", "mine", done = false, from = "Mike", ts = ""),
            TodoItem("2", "theirs", done = false, from = "Mhairi", ts = ""),
        )
        val mine = filterTodosByPerson(todos, ListPersonFilter.MINE, "Mike")
        assertEquals(1, mine.size)
        assertEquals("mine", mine.single().text)
    }

    @Test
    fun richClipDetectsPhoneAndLink() {
        assertNotNull(firstPhone("Contact: +1-555-123-4567 today"))
        assertEquals("github.com", linkPreviewLabel("https://www.github.com/InspMorse/edc-pocket-android"))
    }

    @Test
    fun hostFailureTailscaleHint() {
        val settings = EdcSettings(preset = HostPreset.TAILSCALE, customUrl = HostPreset.TAILSCALE.url)
        val msg = hostFailureMessage(settings, "timeout")
        assertTrue(msg.contains("Tailscale"))
    }

    @Test
    fun parseThemeAccentHex() {
        val color = parseThemeAccent("#22D3EE")
        assertNotNull(color)
    }
}
