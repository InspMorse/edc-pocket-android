package house.edc.pocket

import android.content.Context
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31])
class Phase8Test {
    private lateinit var server: MockWebServer
    private lateinit var context: Context
    private lateinit var client: EdcClient
    private lateinit var syncCoordinator: SyncCoordinator

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        context = RuntimeEnvironment.getApplication()
        client = EdcClient(context.contentResolver)
        syncCoordinator = SyncCoordinator(client, SyncCache(context))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun baseUrl(): String = server.url("/").toString().removeSuffix("/")

    @Test
    fun stalenessFormat_recentAndOld() {
        val now = 10_000_000_000L
        assertEquals("cached just now", formatStaleness(now - 30_000, now))
        assertEquals("cached 5 min ago", formatStaleness(now - 5 * 60_000, now))
        assertEquals("cached 2 hr ago", formatStaleness(now - 2 * 60 * 60_000, now))
    }

    @Test
    fun outboxRetryBackoff_growsWithAttempts() {
        assertEquals(5_000L, outboxRetryDelayMs(1))
        assertEquals(10_000L, outboxRetryDelayMs(2))
        assertEquals(20_000L, outboxRetryDelayMs(3))
        assertTrue(outboxRetryDelayMs(10) <= 5 * 60_000L)
    }

    @Test
    fun fetchEndpoint_honours304() {
        server.enqueue(
            MockResponse()
                .setResponseCode(304)
                .addHeader("ETag", "\"abc\""),
        )
        val response = client.fetchEndpoint(baseUrl(), "/api/clipboard", "Mike", ifNoneMatch = "\"abc\"")
        assertTrue(response.notModified)
        assertEquals("\"abc\"", response.etag)
    }

    @Test
    fun syncCoordinator_usesCacheWhenHostDown() = runBlocking {
        val settings = EdcSettings(
            preset = HostPreset.CUSTOM,
            customUrl = baseUrl(),
        )
        server.enqueue(MockResponse().setBody("""{"ok":true}"""))
        server.enqueue(MockResponse().setBody("""{"capabilities":{}}"""))
        server.enqueue(
            MockResponse()
                .setBody("""[{"id":"1","text":"cached","from":"Mike","ts":"2026-09-02T10:00:00Z"}]""")
                .addHeader("ETag", "\"v1\""),
        )
        server.enqueue(MockResponse().setBody("""{"todos":[]}"""))
        server.enqueue(MockResponse().setBody("""{"files":[]}"""))
        val first = syncCoordinator.sync(settings)
        assertEquals("cached", first.snapshot.latest?.text)

        server.shutdown()
        val offline = syncCoordinator.sync(settings)
        assertTrue(offline.stale)
        assertEquals("cached", offline.snapshot.latest?.text)
        assertNotNull(offline.lastSyncedAt)
    }

    @Test
    fun connectionDoctor_exportLog() = runBlocking {
        server.enqueue(MockResponse().setBody("""{"ok":true}"""))
        server.enqueue(MockResponse().setBody("""{"capabilities":{}}"""))
        server.enqueue(MockResponse().setBody("[]"))
        server.enqueue(MockResponse().setBody("""{"todos":[]}"""))
        server.enqueue(MockResponse().setBody("""{"files":[]}"""))
        val doctor = ConnectionDoctor(client)
        val settings = EdcSettings(preset = HostPreset.CUSTOM, customUrl = baseUrl())
        val report = doctor.run(settings)
        assertFalse(report.checks.isEmpty())
        assertTrue(doctor.exportLog(report).contains("EDC pocket connection doctor"))
    }

    @Test
    fun snapshotFingerprint_changesWhenTodosChange() {
        val snap = HostSnapshot(
            latest = ClipEntry("1", "hi", "Mike", ""),
            todos = listOf(TodoItem("t1", "milk", false, "Mike", "")),
        )
        val fp1 = snapshotFingerprint(snap)
        val fp2 = snapshotFingerprint(snap.copy(todos = listOf(TodoItem("t1", "milk", true, "Mike", ""))))
        assertFalse(fp1 == fp2)
    }
}
