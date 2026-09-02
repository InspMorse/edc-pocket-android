package house.edc.pocket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonParseTest {
    @Test
    fun clipsFromArray() {
        val clips = parseClips(
            """[{"id":"1","text":"hello","from":"Mike","ts":"2026-09-02T10:00:00Z"}]""",
        )
        assertEquals("hello", clips.single().text)
        assertEquals("Mike", clips.single().from)
    }

    @Test
    fun clipsFromLatestAndHistory() {
        val clips = parseClips(
            """
            {
              "latest": {"id":"a","text":"now","from":"Mhairi"},
              "history": [
                {"id":"b","text":"earlier","from":"Mike"}
              ]
            }
            """.trimIndent(),
        )
        assertEquals(listOf("now", "earlier"), clips.map { it.text })
    }

    @Test
    fun clipsFromStringLatest() {
        val clips = parseClips("""{"latest":"plain clip"}""")
        assertEquals("plain clip", clips.first().text)
    }

    @Test
    fun todosFromWrappedList() {
        val todos = parseTodos(
            """{"todos":[{"id":"t1","text":"milk","done":true,"from":"Mike"}]}""",
        )
        assertEquals("milk", todos.single().text)
        assertTrue(todos.single().done)
    }

    @Test
    fun todosDoneAliases() {
        val todos = parseTodos("""[{"title":"eggs","checked":"yes"}]""")
        assertEquals("eggs", todos.single().text)
        assertTrue(todos.single().done)
        assertFalse(parseTodos("""[{"text":"bread","done":false}]""").single().done)
    }

    @Test
    fun dropsFromIncomingFiles() {
        val drops = parseDrops(
            """{"files":[{"filename":"pics/photo.jpg","bytes":2048,"from":"Mike"}]}""",
        )
        assertEquals("photo.jpg", drops.single().name)
        assertEquals(2048L, drops.single().size)
    }

    @Test
    fun emptyAndJunk() {
        assertTrue(parseClips("").isEmpty())
        assertTrue(parseTodos("<html>nope</html>").isEmpty())
        assertTrue(parseDrops("{}").isEmpty())
    }
}
