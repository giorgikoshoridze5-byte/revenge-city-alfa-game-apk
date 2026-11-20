package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "revenge_city_settings")

@Dao
interface PlayerProgressDao {
    @Query("SELECT * FROM player_progress WHERE id = 1")
    fun observeProgress(): Flow<PlayerProgressEntity?>

    @Query("SELECT * FROM player_progress WHERE id = 1")
    suspend fun getProgressOnce(): PlayerProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProgress(progress: PlayerProgressEntity)
}

@Database(entities = [PlayerProgressEntity::class], version = 1, exportSchema = false)
abstract class GameDatabase : RoomDatabase() {
    abstract fun progressDao(): PlayerProgressDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getInstance(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "revenge_city_save.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class GameRepository(private val context: Context) {
    private val dao = GameDatabase.getInstance(context).progressDao()

    private object Keys {
        val GRAPHICS = stringPreferencesKey("graphics_quality")
        val MUSIC_VOL = floatPreferencesKey("music_volume")
        val SFX_VOL = floatPreferencesKey("sfx_volume")
        val VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        val SENSITIVITY = floatPreferencesKey("touch_sensitivity")
        val TIME_SPEED = stringPreferencesKey("time_speed")
        val SHOW_FPS = booleanPreferencesKey("show_fps")
        val DYNAMIC_CAM = booleanPreferencesKey("dynamic_cam")
        val STREET_LIGHTS = booleanPreferencesKey("street_lights_enabled")
    }

    val progressFlow: Flow<PlayerProgressEntity> = dao.observeProgress().map { it ?: PlayerProgressEntity() }

    val settingsFlow: Flow<GameSettings> = context.settingsDataStore.data.map { prefs ->
        val gfx = runCatching {
            GraphicsQuality.valueOf(prefs[Keys.GRAPHICS] ?: GraphicsQuality.BALANCED.name)
        }.getOrDefault(GraphicsQuality.BALANCED)
        val spd = runCatching {
            TimeCycleSpeed.valueOf(prefs[Keys.TIME_SPEED] ?: TimeCycleSpeed.NORMAL.name)
        }.getOrDefault(TimeCycleSpeed.NORMAL)
        GameSettings(
            graphicsQuality = gfx,
            musicVolume = prefs[Keys.MUSIC_VOL] ?: 0.75f,
            sfxVolume = prefs[Keys.SFX_VOL] ?: 0.85f,
            voiceActingEnabled = prefs[Keys.VOICE_ENABLED] ?: true,
            touchSensitivity = prefs[Keys.SENSITIVITY] ?: 1.0f,
            timeCycleSpeed = spd,
            showFpsCounter = prefs[Keys.SHOW_FPS] ?: true,
            dynamicCameraTilt = prefs[Keys.DYNAMIC_CAM] ?: true,
            streetLightsEnabled = prefs[Keys.STREET_LIGHTS] ?: true
        )
    }

    suspend fun ensureInitialized() {
        if (dao.getProgressOnce() == null) {
            dao.upsertProgress(PlayerProgressEntity())
        }
    }

    suspend fun saveProgress(progress: PlayerProgressEntity) {
        dao.upsertProgress(progress)
    }

    suspend fun resetStoryProgress() {
        dao.upsertProgress(PlayerProgressEntity())
    }

    suspend fun updateSettings(transform: (GameSettings) -> GameSettings) {
        context.settingsDataStore.edit { prefs ->
            val current = GameSettings(
                graphicsQuality = runCatching {
                    GraphicsQuality.valueOf(prefs[Keys.GRAPHICS] ?: GraphicsQuality.BALANCED.name)
                }.getOrDefault(GraphicsQuality.BALANCED),
                musicVolume = prefs[Keys.MUSIC_VOL] ?: 0.75f,
                sfxVolume = prefs[Keys.SFX_VOL] ?: 0.85f,
                voiceActingEnabled = prefs[Keys.VOICE_ENABLED] ?: true,
                touchSensitivity = prefs[Keys.SENSITIVITY] ?: 1.0f,
                timeCycleSpeed = runCatching {
                    TimeCycleSpeed.valueOf(prefs[Keys.TIME_SPEED] ?: TimeCycleSpeed.NORMAL.name)
                }.getOrDefault(TimeCycleSpeed.NORMAL),
                showFpsCounter = prefs[Keys.SHOW_FPS] ?: true,
                dynamicCameraTilt = prefs[Keys.DYNAMIC_CAM] ?: true,
                streetLightsEnabled = prefs[Keys.STREET_LIGHTS] ?: true
            )
            val updated = transform(current)
            prefs[Keys.GRAPHICS] = updated.graphicsQuality.name
            prefs[Keys.MUSIC_VOL] = updated.musicVolume
            prefs[Keys.SFX_VOL] = updated.sfxVolume
            prefs[Keys.VOICE_ENABLED] = updated.voiceActingEnabled
            prefs[Keys.SENSITIVITY] = updated.touchSensitivity
            prefs[Keys.TIME_SPEED] = updated.timeCycleSpeed.name
            prefs[Keys.SHOW_FPS] = updated.showFpsCounter
            prefs[Keys.DYNAMIC_CAM] = updated.dynamicCameraTilt
            prefs[Keys.STREET_LIGHTS] = updated.streetLightsEnabled
        }
    }
}
