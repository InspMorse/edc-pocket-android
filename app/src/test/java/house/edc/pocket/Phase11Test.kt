package house.edc.pocket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase11Test {
    @Test
    fun parseTodo_withNotesDueDateAndSubItems() {
        val raw = """
            {"todos":[{"id":"1","text":"milk","note":"2% only","due_date":"2026-09-10",
            "category":"Dairy","recurrence":"weekly",
            "sub_items":[{"text":"check expiry","done":false}]}]}
        """.trimIndent()
        val items = parseTodos(raw)
        assertEquals(1, items.size)
        val item = items.first()
        assertEquals("milk", item.text)
        assertEquals("2% only", item.note)
        assertEquals("2026-09-10", item.dueDate)
        assertEquals("Dairy", item.category)
        assertEquals("weekly", item.recurrence)
        assertEquals(1, item.subItems.size)
        assertEquals("check expiry", item.subItems.first().text)
    }

    @Test
    fun groceryAisle_infersFromText() {
        assertEquals(GroceryAisle.DAIRY, GroceryAisle.infer("buy milk"))
        assertEquals(GroceryAisle.PRODUCE, GroceryAisle.infer("bananas"))
    }

    @Test
    fun groupDropsBySession_splitsFolders() {
        val drops = listOf(
            DropItem("1", "a.jpg", "Mike", "", 0, "Drop/Sessions/Party/a.jpg"),
            DropItem("2", "b.jpg", "Mike", "", 0, "Drop/Sessions/Party/b.jpg"),
            DropItem("3", "loose.pdf", "Mike", "", 0, "loose.pdf"),
        )
        val groups = groupDropsBySession(drops)
        assertTrue(groups.any { it.folder == "Party" && it.drops.size == 2 })
    }

    @Test
    fun splitMarkdownSegments_findsCodeBlock() {
        val segments = splitMarkdownSegments("Hello\n```kotlin\nval x = 1\n```\nDone")
        assertEquals(3, segments.size)
        assertTrue(segments[1].isCodeBlock)
        assertEquals("val x = 1", segments[1].text)
    }

    @Test
    fun buildIncomingZip_containsFiles() {
        val zip = buildIncomingZip(listOf("a.txt" to "hello".toByteArray()))
        assertTrue(zip.size > 20)
        assertTrue(zip[0] == 0x50.toByte() && zip[1] == 0x4b.toByte())
    }

    @Test
    fun dropItem_detectsMediaTypes() {
        assertTrue(DropItem("1", "clip.mp4", "", "", 0).isVideo())
        assertTrue(DropItem("2", "doc.pdf", "", "", 0).isPdf())
        assertTrue(DropItem("3", "song.mp3", "", "", 0).isAudio())
    }

    @Test
    fun enrichTodos_mergesLocalExtras() {
        val items = listOf(TodoItem("1", "eggs", false, "Mike", ""))
        val extras = mapOf("1" to TodoExtra(note = "free range", category = "Dairy"))
        val merged = enrichTodos(items, extras)
        assertEquals("free range", merged.first().note)
        assertEquals("Dairy", merged.first().category)
    }
}
