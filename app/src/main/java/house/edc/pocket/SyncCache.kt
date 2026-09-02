package house.edc.pocket

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "cached_endpoints")
data class CachedEndpointEntity(
    @PrimaryKey val cacheKey: String,
    val body: String,
    val etag: String = "",
    val lastModified: String = "",
    val syncedAt: Long,
)

@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey val hostKey: String,
    val lastSyncedAt: Long = 0L,
)

data class CachedEndpoint(
    val body: String,
    val etag: String,
    val lastModified: String,
    val syncedAt: Long,
)

@Dao
interface SyncCacheDao {
    @Query("SELECT * FROM cached_endpoints WHERE cacheKey = :key LIMIT 1")
    suspend fun getEndpoint(key: String): CachedEndpointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putEndpoint(entity: CachedEndpointEntity)

    @Query("SELECT * FROM sync_meta WHERE hostKey = :hostKey LIMIT 1")
    suspend fun getMeta(hostKey: String): SyncMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putMeta(entity: SyncMetaEntity)

    @Query("DELETE FROM cached_endpoints WHERE cacheKey LIKE :hostKeyPrefix || '%'")
    suspend fun clearHost(hostKeyPrefix: String)

    @Query("DELETE FROM cached_endpoints")
    suspend fun clearAllEndpoints()

    @Query("DELETE FROM sync_meta")
    suspend fun clearAllMeta()
}

@Database(
    entities = [CachedEndpointEntity::class, SyncMetaEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class EdcSyncDatabase : RoomDatabase() {
    abstract fun cacheDao(): SyncCacheDao

    companion object {
        @Volatile
        private var instance: EdcSyncDatabase? = null

        fun get(context: Context): EdcSyncDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    EdcSyncDatabase::class.java,
                    "edc_sync_cache",
                ).build().also { instance = it }
            }
    }
}

class SyncCache(context: Context) {
    private val dao = EdcSyncDatabase.get(context).cacheDao()

    fun hostKey(baseUrl: String, identity: String): String =
        "${baseUrl.trimEnd('/')}|${identity.trim()}"

    private fun endpointKey(hostKey: String, endpoint: String): String = "$hostKey|$endpoint"

    suspend fun getEndpoint(hostKey: String, endpoint: String): CachedEndpoint? {
        val row = dao.getEndpoint(endpointKey(hostKey, endpoint)) ?: return null
        return CachedEndpoint(
            body = row.body,
            etag = row.etag,
            lastModified = row.lastModified,
            syncedAt = row.syncedAt,
        )
    }

    suspend fun putEndpoint(
        hostKey: String,
        endpoint: String,
        body: String,
        etag: String,
        lastModified: String,
        syncedAt: Long = System.currentTimeMillis(),
    ) {
        dao.putEndpoint(
            CachedEndpointEntity(
                cacheKey = endpointKey(hostKey, endpoint),
                body = body,
                etag = etag,
                lastModified = lastModified,
                syncedAt = syncedAt,
            ),
        )
    }

    suspend fun getLastSynced(hostKey: String): Long? =
        dao.getMeta(hostKey)?.lastSyncedAt?.takeIf { it > 0L }

    suspend fun setLastSynced(hostKey: String, at: Long = System.currentTimeMillis()) {
        dao.putMeta(SyncMetaEntity(hostKey = hostKey, lastSyncedAt = at))
    }

    suspend fun loadSnapshot(hostKey: String, baseUrl: String): HostSnapshot? {
        val clip = getEndpoint(hostKey, ENDPOINT_CLIPBOARD)?.body
        val todo = getEndpoint(hostKey, ENDPOINT_TODO)?.body
        val drop = getEndpoint(hostKey, ENDPOINT_INCOMING)?.body
        if (clip == null && todo == null && drop == null) return null
        val clips = clip?.let { parseClips(it) }.orEmpty()
        return HostSnapshot(
            latest = clips.firstOrNull(),
            history = clips,
            todos = todo?.let { parseTodos(it) }.orEmpty(),
            drops = drop?.let { parseDrops(it, baseUrl) }.orEmpty(),
        )
    }

    suspend fun clearHost(hostKey: String) {
        dao.clearHost(hostKey)
    }

    suspend fun clearAll() {
        dao.clearAllEndpoints()
        dao.clearAllMeta()
    }

    companion object {
        const val ENDPOINT_CLIPBOARD = "clipboard"
        const val ENDPOINT_TODO = "todo"
        const val ENDPOINT_INCOMING = "incoming"
    }
}

internal fun snapshotFingerprint(snapshot: HostSnapshot): String {
    val clipPart = snapshot.latest?.id.orEmpty() + ":" + snapshot.history.size
    val todoPart = snapshot.todos.joinToString(",") { "${it.id}:${it.done}" }
    return "$clipPart|$todoPart"
}
