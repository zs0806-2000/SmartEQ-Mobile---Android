package com.smarteq.pro.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.smarteq.pro.data.model.DynamicsSettings
import com.smarteq.pro.data.model.EqPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val bandCount: Int,
    /** JSON-encoded List<Float>, stored as text to avoid a fragile column-per-band schema. */
    val bandGainsJson: String,
    val bassDb: Float,
    val trebleDb: Float,
    val compThresholdDb: Float,
    val compRatio: Float,
    val compAttackSec: Float,
    val compReleaseSec: Float,
    val compKneeDb: Float,
    val linkedOutputDeviceAddress: String?,
    val createdAtEpochMs: Long,
)

private val presetJson = Json { ignoreUnknownKeys = true }

fun PresetEntity.toDomain(): EqPreset = EqPreset(
    id = id,
    name = name,
    bandCount = bandCount,
    bandGainsDb = presetJson.decodeFromString(bandGainsJson),
    dynamics = DynamicsSettings(bassDb, trebleDb, compThresholdDb, compRatio, compAttackSec, compReleaseSec, compKneeDb),
    linkedOutputDeviceAddress = linkedOutputDeviceAddress,
    isBuiltIn = false,
)

fun EqPreset.toEntity(): PresetEntity = PresetEntity(
    id = id,
    name = name,
    bandCount = bandCount,
    bandGainsJson = presetJson.encodeToString(bandGainsDb),
    bassDb = dynamics.bassDb,
    trebleDb = dynamics.trebleDb,
    compThresholdDb = dynamics.compThresholdDb,
    compRatio = dynamics.compRatio,
    compAttackSec = dynamics.compAttackSec,
    compReleaseSec = dynamics.compReleaseSec,
    compKneeDb = dynamics.compKneeDb,
    linkedOutputDeviceAddress = linkedOutputDeviceAddress,
    createdAtEpochMs = System.currentTimeMillis(),
)

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY createdAtEpochMs ASC")
    fun observeAll(): Flow<List<PresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preset: PresetEntity)

    @Update
    suspend fun update(preset: PresetEntity)

    @Delete
    suspend fun delete(preset: PresetEntity)

    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM presets WHERE linkedOutputDeviceAddress = :deviceAddress LIMIT 1")
    suspend fun findLinkedToDevice(deviceAddress: String): PresetEntity?
}

@Database(entities = [PresetEntity::class], version = 1, exportSchema = false)
abstract class SmartEqDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao

    companion object {
        fun build(context: android.content.Context): SmartEqDatabase =
            Room.databaseBuilder(context, SmartEqDatabase::class.java, "smarteq_presets.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
