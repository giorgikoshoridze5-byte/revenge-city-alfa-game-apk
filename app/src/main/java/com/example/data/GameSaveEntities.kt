package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_progress")
data class PlayerProgressEntity(
    @PrimaryKey val id: Int = 1,
    val cash: Int = 350,
    val health: Int = 100,
    val armor: Int = 50,
    val ownedWeaponsCsv: String = WeaponType.PISTOL.id,
    val equippedWeapon: String = WeaponType.PISTOL.id,
    val ownedVehiclesCsv: String = VehicleModel.SEDAN.id,
    val activeVehicle: String = VehicleModel.SEDAN.id,
    val ownedHousesCsv: String = "",
    val completedMissionsCsv: String = "",
    val totalEliminations: Int = 0
)

enum class GraphicsQuality(val label: String, val drawDistance: Float, val maxTraffic: Int, val maxPeds: Int, val targetFps: Int) {
    LOW("Performance (30 FPS)", 120f, 6, 8, 30),
    BALANCED("Balanced (60 FPS)", 180f, 10, 14, 60),
    ULTRA("Ultra Physical (60 FPS)", 250f, 14, 20, 60)
}

enum class TimeCycleSpeed(val label: String, val hoursPerRealMinute: Float) {
    FROZEN("Frozen Daylight", 0f),
    NORMAL("Normal (24m / day)", 1.0f),
    FAST("Cinematic Fast (4m / day)", 6.0f)
}

data class GameSettings(
    val graphicsQuality: GraphicsQuality = GraphicsQuality.BALANCED,
    val musicVolume: Float = 0.75f,
    val sfxVolume: Float = 0.85f,
    val voiceActingEnabled: Boolean = true,
    val touchSensitivity: Float = 1.0f,
    val timeCycleSpeed: TimeCycleSpeed = TimeCycleSpeed.NORMAL,
    val showFpsCounter: Boolean = true,
    val dynamicCameraTilt: Boolean = true,
    val streetLightsEnabled: Boolean = true
)
