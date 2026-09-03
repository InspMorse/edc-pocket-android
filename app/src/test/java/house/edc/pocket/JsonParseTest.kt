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

    @Test
    fun clipsUseUpdatedByAndCreatedAt() {
        val clips = parseClips(
            """{"items":[{"text":"link","updated_by":"Mhairi","created_at":"2026-09-02T12:00:00Z"}]}""",
        )
        assertEquals("Mhairi", clips.single().from)
        assertEquals("2026-09-02T12:00:00Z", clips.single().ts)
    }

    @Test
    fun healthFromHostJson() {
        val health = parseHealth(
            """
            {
              "ok": true,
              "version": "0.22.1",
              "host_name": "edc-home",
              "dashboard_url": "http://192.168.0.99:8765/"
            }
            """.trimIndent(),
        )
        assertTrue(health.ok)
        assertEquals("0.22.1", health.version)
        assertEquals("edc-home", health.hostName)
        assertEquals("edc-home · v0.22.1", health.summary())
    }

    @Test
    fun healthFromActiveHost() {
        val health = parseHealth(
            """{"ok":true,"active_host":{"name":"away-node"},"app_version":"0.22.0"}""",
        )
        assertEquals("away-node", health.hostName)
        assertEquals("0.22.0", health.version)
    }

    @Test
    fun dropsResolveRelativePath() {
        val drops = parseDrops(
            """{"items":[{"filename":"photo.jpg","path":"api/incoming/abc","from":"Mike"}]}""",
            "http://192.168.0.99:8765",
        )
        assertEquals("api/incoming/abc", drops.single().path)
    }
}
