package house.edc.pocket

import android.content.ContentResolver
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class EdcClientIntegrationTest {
    private lateinit var server: MockWebServer
    private lateinit var client: EdcClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val resolver = RuntimeEnvironment.getApplication().contentResolver
        client = EdcClient(resolver)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun baseUrl(): String = server.url("/").toString().removeSuffix("/")

    @Test
    fun probeHealth_parsesHealthResponse() {
        server.enqueue(
            MockResponse().setBody(
                """{"ok":true,"version":"0.22.1","host_name":"edc-test"}""",
            ),
        )
        val health = client.probeHealth(baseUrl(), "Mike")
        assertTrue(health.ok)
        assertEquals("0.22.1", health.version)
        assertEquals("edc-test", health.hostName)
        assertEquals("/api/health", server.takeRequest().path?.substringBefore("?"))
    }

    @Test
    fun probeHealth_mergesCapabilitiesEndpoint() {
        server.enqueue(
            MockResponse().setBody("""{"ok":true,"capabilities":{"todo_delete":true}}"""),
        )
        server.enqueue(
            MockResponse().setBody("""{"capabilities":{"todo_delete":false,"incoming":false}}"""),
        )
        val health = client.probeHealth(baseUrl(), "Mhairi")
        assertFalse(health.capabilities.todoDelete)
        assertFalse(health.capabilities.incoming)
        server.takeRequest()
        assertEquals("/api/capabilities", server.takeRequest().path?.substringBefore("?"))
    }

    @Test
    fun load_fetchesClipboardAndTodo() {
        server.enqueue(
            MockResponse().setBody(
                """[{"id":"1","text":"hello","from":"Mike","ts":"2026-09-02T10:00:00Z"}]""",
            ),
        )
        server.enqueue(MockResponse().setBody("""{"todos":[{"id":"t1","text":"milk","done":false}]}"""))
        server.enqueue(MockResponse().setBody("""{"files":[]}"""))
        val snap = client.load(baseUrl(), "Mike")
        assertEquals("hello", snap.latest?.text)
        assertEquals("milk", snap.todos.single().text)
    }

    @Test
    fun sendText_postsToClipboard() {
        server.enqueue(MockResponse().setResponseCode(200))
        client.sendText(baseUrl(), "Mike", "test clip")
        val req = server.takeRequest()
        assertEquals("POST", req.method)
        assertTrue(req.path!!.startsWith("/api/clipboard"))
        assertTrue(req.body.readUtf8().contains("test clip"))
    }

    @Test
    fun addTodo_postsNewItem() {
        server.enqueue(MockResponse().setResponseCode(200))
        client.addTodo(baseUrl(), "Mhairi", "eggs")
        val req = server.takeRequest()
        assertEquals("POST", req.method)
        assertTrue(req.path!!.startsWith("/api/todo"))
        assertTrue(req.body.readUtf8().contains("eggs"))
    }

    @Test
    fun findReachableHost_triesCandidates() {
        server.enqueue(MockResponse().setBody("""{"ok":true,"host_name":"mock"}"""))
        val settings = EdcSettings(
            preset = HostPreset.CUSTOM,
            customUrl = baseUrl(),
            autoHost = false,
        )
        val found = client.findReachableHost(settings, "Mike")
        assertEquals("mock", found?.health?.hostName)
        assertEquals(baseUrl(), found?.baseUrl)
    }

    @Test
    fun probeHealth_failsOnNonSuccess() {
        server.enqueue(MockResponse().setResponseCode(503))
        val result = runCatching { client.probeHealth(baseUrl(), "Mike") }
        assertTrue(result.isFailure)
    }
}
