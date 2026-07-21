package com.roadtrippin.shared.data

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.roadtrippin.shared.domain.AppSettings
import com.roadtrippin.shared.domain.Trip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val payload: String,
    val updatedAt: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

@Dao
interface AppStateDao {
    @Query("SELECT * FROM app_state WHERE id = 1")
    fun observe(): Flow<AppStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppStateEntity)

    @Query("DELETE FROM app_state")
    suspend fun clear()
}

@Database(entities = [AppStateEntity::class], version = 1, exportSchema = true)
@ConstructedBy(RoadtrippinDatabaseConstructor::class)
abstract class RoadtrippinDatabase : RoomDatabase() {
    abstract fun appStateDao(): AppStateDao
}

@Suppress("KotlinNoActualForExpect")
expect object RoadtrippinDatabaseConstructor : RoomDatabaseConstructor<RoadtrippinDatabase> {
    override fun initialize(): RoadtrippinDatabase
}

expect object DatabaseFactory {
    fun build(): RoomDatabase.Builder<RoadtrippinDatabase>
}

fun buildRoadtrippinDatabase(): RoadtrippinDatabase = DatabaseFactory.build()
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.Default)
    .build()

@Serializable
data class SyncMutation(
    val id: String,
    val createdAt: Long,
    val attempts: Int = 0,
)

@Serializable
data class SyncTombstone(
    val entityType: String,
    val entityId: String,
    val deletedAt: Long,
    val mutationId: String,
)

@Serializable
data class PersistedAppState(
    val settings: AppSettings = AppSettings(),
    val trips: List<Trip> = emptyList(),
    val activeTripId: String? = null,
    val outbox: List<SyncMutation> = emptyList(),
    val tombstones: List<SyncTombstone> = emptyList(),
    val confirmedCloudAccountId: String? = null,
)

interface AppStateRepository {
    fun observe(): Flow<PersistedAppState?>
    suspend fun save(state: PersistedAppState)
    suspend fun clear()
}

class RoomAppStateRepository(
    database: RoadtrippinDatabase = buildRoadtrippinDatabase(),
) : AppStateRepository {
    private val dao = database.appStateDao()
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override fun observe(): Flow<PersistedAppState?> = dao.observe().map { entity ->
        entity?.let { runCatching { json.decodeFromString<PersistedAppState>(it.payload) }.getOrNull() }
    }

    override suspend fun save(state: PersistedAppState) {
        dao.upsert(
            AppStateEntity(
                payload = json.encodeToString(state),
                updatedAt = state.trips.maxOfOrNull(Trip::modifiedAt) ?: 0L,
            )
        )
    }

    override suspend fun clear() = dao.clear()
}

object AppStateRepositories {
    val room: AppStateRepository by lazy { RoomAppStateRepository() }
}
