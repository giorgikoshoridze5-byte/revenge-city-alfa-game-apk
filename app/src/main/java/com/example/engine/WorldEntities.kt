package com.example.engine

import com.example.data.NpcDialogueEntry
import com.example.data.VehicleModel
import com.example.data.VoiceProfile

enum class BuildingStyle {
    SKYSCRAPER_GLASS,
    URBAN_BLOCK,
    OLD_TOWN_STONE,
    SHIPPING_CONTAINER,
    HOUSE_PROPERTY,
    WEAPON_SHOP,
    CAR_STATION
}

data class BuildingBox(
    val x: Float,
    val z: Float,
    val width: Float,
    val depth: Float,
    val height: Float,
    val r: Float,
    val g: Float,
    val b: Float,
    val style: BuildingStyle,
    val hasRoofAccent: Boolean = false,
    val hasDome: Boolean = false
)

data class RoadStrip(
    val x: Float,
    val z: Float,
    val width: Float,
    val depth: Float,
    val isBridge: Boolean = false,
    val isHighway: Boolean = false
)

data class RoadDash(
    val x: Float,
    val z: Float,
    val width: Float,
    val depth: Float
)

data class GreenParkPad(
    val x: Float,
    val z: Float,
    val width: Float,
    val depth: Float
)

data class StreetLamp(
    val x: Float,
    val z: Float
)

data class TreeProp(
    val x: Float,
    val z: Float,
    val height: Float,
    val radius: Float
)

class PhysicsProp(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var vz: Float = 0f,
    var yaw: Float = 0f,
    var tilt: Float = 0f,
    val size: Float = 1.1f,
    val r: Float,
    val g: Float,
    val b: Float,
    val isBarrel: Boolean = false
)

class VehicleEntity(
    val id: Int,
    val model: VehicleModel,
    var x: Float,
    var z: Float,
    var yaw: Float,
    var speed: Float = 0f,
    var lateralVel: Float = 0f,
    var steerAngle: Float = 0f,
    var pitch: Float = 0f,
    var roll: Float = 0f,
    var wheelSpin: Float = 0f,
    var r: Float = model.r,
    var g: Float = model.g,
    var b: Float = model.b,
    var isPolice: Boolean = false,
    var isTraffic: Boolean = false,
    var isPlayerOwned: Boolean = false,
    var health: Float = 100f,
    var sirenTimer: Float = 0f,
    var trafficDirX: Float = 0f,
    var trafficDirZ: Float = 1f
)

enum class NpcKind {
    STORY_NPC,
    PEDESTRIAN,
    SYNDICATE_ENEMY,
    POLICE_OFFICER,
    SYNDICATE_BOSS
}

class NpcEntity(
    val id: Int,
    val kind: NpcKind,
    val name: String,
    var x: Float,
    var z: Float,
    var yaw: Float = 0f,
    var hp: Int = 100,
    val maxHp: Int = 100,
    var walkPhase: Float = 0f,
    var attackCooldown: Float = 0f,
    var patrolTimer: Float = 0f,
    val homeX: Float = x,
    val homeZ: Float = z,
    val shirtR: Float,
    val shirtG: Float,
    val shirtB: Float,
    val voiceProfile: VoiceProfile = VoiceProfile.BOY_CHILL,
    val dialogue: NpcDialogueEntry? = null
)

class Projectile(
    var x: Float,
    var y: Float,
    var z: Float,
    val vx: Float,
    val vy: Float,
    val vz: Float,
    var life: Float,
    val fromPlayer: Boolean,
    val damage: Int,
    val r: Float,
    val g: Float,
    val b: Float
)

class Particle(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    var life: Float,
    val maxLife: Float,
    val size: Float,
    val r: Float,
    val g: Float,
    val b: Float
)

class PickupOrb(
    val x: Float,
    val z: Float,
    val cashAmount: Int,
    val healAmount: Int = 0,
    var life: Float = 45f
)

enum class InteractableType {
    MISSION_START,
    WEAPON_SHOP,
    CAR_STATION,
    BUYABLE_HOUSE,
    TALK_NPC
}

data class NearbyInteractable(
    val type: InteractableType,
    val id: String,
    val label: String,
    val actionLabel: String
)

data class MinimapDot(
    val x: Float,
    val z: Float,
    val kind: String
)
