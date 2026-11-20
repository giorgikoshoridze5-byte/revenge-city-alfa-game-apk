package com.example.engine

import com.example.data.BuyableHouse
import com.example.data.CutsceneCameraFocus
import com.example.data.GameCatalog
import com.example.data.GraphicsQuality
import com.example.data.MissionDefinition
import com.example.data.MissionObjectiveType
import com.example.data.TimeCycleSpeed
import com.example.data.VehicleModel
import com.example.data.WeaponType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class HudSnapshot(
    val cash: Int,
    val health: Int,
    val armor: Int,
    val wantedStars: Int,
    val timeOfDayHours: Float,
    val districtName: String,
    val speedKmh: Int,
    val inVehicle: Boolean,
    val currentVehicleName: String?,
    val canEnterVehicle: Boolean,
    val equippedWeapon: WeaponType,
    val nearbyInteractable: NearbyInteractable?,
    val activeMissionTitle: String?,
    val activeStageTitle: String?,
    val activeObjectiveText: String?,
    val missionTimerSec: Float?,
    val playerX: Float,
    val playerZ: Float,
    val playerYaw: Float,
    val minimapDots: List<MinimapDot>,
    val gpsTargetX: Float?,
    val gpsTargetZ: Float?,
    val gpsLabel: String?,
    val gpsDistanceMeters: Int?,
    val fps: Int
)

interface SimulationListener {
    fun onPlayWeaponSound(weapon: WeaponType)
    fun onPlayHorn()
    fun onPlayCollision()
    fun onPlayCashPickup()
    fun onStartCutscene(mission: MissionDefinition, isOutro: Boolean)
    fun onMissionCompleted(mission: MissionDefinition)
    fun onMissionFailed(reason: String)
    fun onPlayerWasted()
    fun onCashGained(amount: Int)
    fun onHealthChanged(health: Int, armor: Int)
}

class WorldSimulation(private val listener: SimulationListener) {

    val lock = Any()
    val world: CityWorldData = CityWorldBuilder.build()

    // Player (George Nikoladze) state
    var playerX: Float = -18f
    var playerZ: Float = 8f
    var playerYaw: Float = 0.2f
    var playerWalkPhase: Float = 0f
    var playerHealth: Int = 100
    var playerArmor: Int = 50
    var playerCash: Int = 350
    var equippedWeapon: WeaponType = WeaponType.PISTOL
    var ownedWeapons: Set<WeaponType> = setOf(WeaponType.PISTOL)
    var ownedHouses: Set<String> = emptySet()
    var completedMissions: Set<String> = emptySet()

    // Vehicle state
    var drivingVehicle: VehicleEntity? = null

    // Wanted & combat
    var wantedStars: Int = 0
    private var wantedDecayTimer: Float = 0f
    private var policeSpawnTimer: Float = 0f
    private var weaponCooldown: Float = 0f
    var muzzleFlashTimer: Float = 0f

    // Day-Night Cycle (starts at golden late afternoon 17:15)
    var timeOfDayHours: Float = 17.25f
    var animClock: Float = 0f

    // Settings
    var graphicsQuality: GraphicsQuality = GraphicsQuality.BALANCED
    var timeCycleSpeed: TimeCycleSpeed = TimeCycleSpeed.NORMAL
    var touchSensitivity: Float = 1.0f
    var dynamicCameraTilt: Boolean = true
    var streetLightsEnabled: Boolean = true
    @Volatile var isPortraitViewport: Boolean = true

    // Input state (updated from Compose touch controls)
    @Volatile var joyX: Float = 0f
    @Volatile var joyY: Float = 0f
    @Volatile var steerLeft: Boolean = false
    @Volatile var steerRight: Boolean = false
    @Volatile var isSprinting: Boolean = false
    @Volatile var isFiring: Boolean = false
    @Volatile var isGas: Boolean = false
    @Volatile var isBrake: Boolean = false
    @Volatile var isHandbrake: Boolean = false

    // Active speech state for 3D animated mouth
    @Volatile var talkingSpeakerName: String? = null
    @Volatile var isTalkingNow: Boolean = false

    // Camera state
    var camYaw: Float = 0.2f
    var camPitch: Float = 0.38f
    var camPosX: Float = -18f
    var camPosY: Float = 6f
    var camPosZ: Float = 18f
    var camLookX: Float = -18f
    var camLookY: Float = 1.8f
    var camLookZ: Float = 8f

    // Cutscene camera override
    var isCutsceneActive: Boolean = false
    var cutsceneFocus: CutsceneCameraFocus = CutsceneCameraFocus.TWO_SHOT
    var cutsceneNpcX: Float = 0f
    var cutsceneNpcZ: Float = 0f

    // Active Mission runtime
    var activeMission: MissionDefinition? = null
    private var activeStageIdx: Int = 0
    private var missionTimerSec: Float = 0f
    private var pickupTargetActive: Boolean = false
    private var pickupX: Float = 0f
    private var pickupZ: Float = 0f

    // Dynamic transient lists
    val projectiles = ArrayList<Projectile>(48)
    val particles = ArrayList<Particle>(120)
    val pickups = ArrayList<PickupOrb>(24)

    // Custom GPS Tracker Waypoint
    var customGpsX: Float? = null
    var customGpsZ: Float? = null
    var customGpsLabel: String? = null

    private var nextEntityId = 500
    private val rng = Random(99)

    // FPS tracking
    var currentFps: Int = 60

    fun setGpsWaypoint(x: Float, z: Float, label: String) {
        synchronized(lock) {
            customGpsX = x.coerceIn(-CityWorldBuilder.WORLD_HALF_EXTENT + 5f, CityWorldBuilder.WORLD_HALF_EXTENT - 5f)
            customGpsZ = z.coerceIn(-CityWorldBuilder.WORLD_HALF_EXTENT + 5f, CityWorldBuilder.WORLD_HALF_EXTENT - 5f)
            customGpsLabel = label
        }
    }

    fun clearGpsWaypoint() {
        synchronized(lock) {
            customGpsX = null
            customGpsZ = null
            customGpsLabel = null
        }
    }

    fun getGpsTarget(): Triple<Float, Float, String>? {
        synchronized(lock) {
            val cx = customGpsX
            val cz = customGpsZ
            if (cx != null && cz != null) {
                return Triple(cx, cz, customGpsLabel ?: "GPS Pin")
            }
            val st = activeMission?.stages?.getOrNull(activeStageIdx) ?: return null
            return Triple(st.targetX, st.targetZ, st.title)
        }
    }

    fun syncPersistentState(
        cash: Int,
        health: Int,
        armor: Int,
        weapon: WeaponType,
        ownedWeps: Set<WeaponType>,
        ownedHouseIds: Set<String>,
        completedMissionIds: Set<String>
    ) {
        synchronized(lock) {
            playerCash = cash
            playerHealth = health.coerceIn(1, 100)
            playerArmor = armor.coerceIn(0, 100)
            equippedWeapon = weapon
            ownedWeapons = ownedWeps
            ownedHouses = ownedHouseIds
            completedMissions = completedMissionIds
        }
    }

    fun orbitCamera(dx: Float, dy: Float) {
        val sens = 0.0055f * touchSensitivity
        camYaw -= dx * sens
        camPitch = (camPitch + dy * sens).coerceIn(0.12f, 1.05f)
    }

    fun triggerFireOnce() {
        synchronized(lock) {
            if (drivingVehicle == null && weaponCooldown <= 0f) {
                fireWeaponLocked()
            }
        }
    }

    fun toggleVehicleEntry() {
        synchronized(lock) {
            joyX = 0f
            joyY = 0f
            steerLeft = false
            steerRight = false
            isSprinting = false
            isFiring = false
            isGas = false
            isBrake = false
            isHandbrake = false
            val currentCar = drivingVehicle
            if (currentCar != null) {
                // Exit car to the left side (or right side if blocked by building)
                val leftAngle = currentCar.yaw + (PI.toFloat() / 2f)
                val rightAngle = currentCar.yaw - (PI.toFloat() / 2f)
                val (lx, lz, hitLeft) = resolveCircleBuildingCollisionLocked(
                    currentCar.x + sin(leftAngle) * 2.4f,
                    currentCar.z + cos(leftAngle) * 2.4f,
                    0.62f
                )
                val (rx, rz, hitRight) = resolveCircleBuildingCollisionLocked(
                    currentCar.x + sin(rightAngle) * 2.4f,
                    currentCar.z + cos(rightAngle) * 2.4f,
                    0.62f
                )
                if (!hitLeft || hitRight) {
                    playerX = lx
                    playerZ = lz
                } else {
                    playerX = rx
                    playerZ = rz
                }
                currentCar.speed *= 0.4f
                drivingVehicle = null
            } else {
                val nearest = findNearestVehicleLocked(4.8f)
                if (nearest != null) {
                    nearest.isTraffic = false
                    drivingVehicle = nearest
                    camYaw = nearest.yaw
                }
            }
        }
    }

    fun soundHorn() {
        synchronized(lock) {
            if (drivingVehicle != null) {
                listener.onPlayHorn()
            }
        }
    }

    fun spawnOwnedVehicleNearPlayer(model: VehicleModel, customX: Float? = null, customZ: Float? = null) {
        synchronized(lock) {
            val (sx, sz) = resolveCircleWorldCollisionLocked(
                customX ?: (playerX + 5.5f),
                customZ ?: (playerZ + 4.5f),
                1.65f
            )
            val existing = world.vehicles.firstOrNull { it.isPlayerOwned && it.model == model }
            if (existing != null) {
                existing.x = sx
                existing.z = sz
                existing.speed = 0f
                existing.health = 100f
            } else {
                world.vehicles.add(
                    VehicleEntity(
                        id = nextEntityId++,
                        model = model,
                        x = sx,
                        z = sz,
                        yaw = playerYaw,
                        isPlayerOwned = true
                    )
                )
            }
        }
    }

    fun repairActiveOrNearestVehicle() {
        synchronized(lock) {
            val car = drivingVehicle ?: findNearestVehicleLocked(14f)
            car?.health = 100f
        }
    }

    fun healAtHouse(house: BuyableHouse) {
        synchronized(lock) {
            playerHealth = 100
            playerArmor = maxOf(playerArmor, 50)
            playerX = house.spawnX
            playerZ = house.spawnZ
            drivingVehicle = null
            wantedStars = 0
            listener.onHealthChanged(playerHealth, playerArmor)
        }
    }

    fun startMissionDirectly(mission: MissionDefinition) {
        synchronized(lock) {
            // Clear any prior mission enemies
            world.npcs.removeAll { it.kind == NpcKind.SYNDICATE_ENEMY || it.kind == NpcKind.SYNDICATE_BOSS }
            activeMission = mission
            activeStageIdx = 0
            isCutsceneActive = true
            cutsceneFocus = CutsceneCameraFocus.TWO_SHOT
            val npc = world.npcs.firstOrNull { it.dialogue?.name == mission.npcName }
            cutsceneNpcX = npc?.x ?: (mission.startX + 2.4f)
            cutsceneNpcZ = npc?.z ?: mission.startZ
            if (drivingVehicle != null) {
                drivingVehicle = null
            }
            playerX = mission.startX
            playerZ = mission.startZ
            playerYaw = atan2(cutsceneNpcX - playerX, cutsceneNpcZ - playerZ)
        }
        listener.onStartCutscene(mission, isOutro = false)
    }

    fun beginMissionGameplayAfterIntro() {
        synchronized(lock) {
            isCutsceneActive = false
            setupCurrentMissionStageLocked()
        }
    }

    fun finishOutroCutscene() {
        synchronized(lock) {
            isCutsceneActive = false
            val mission = activeMission
            activeMission = null
            pickupTargetActive = false
            missionTimerSec = 0f
            wantedStars = 0
            if (mission != null) {
                completedMissions = completedMissions + mission.id
                listener.onMissionCompleted(mission)
            }
        }
    }

    fun respawnAfterWasted() {
        synchronized(lock) {
            playerHealth = 100
            playerArmor = 25
            wantedStars = 0
            drivingVehicle = null
            playerX = -18f
            playerZ = 8f
            activeMission = null
            pickupTargetActive = false
            world.npcs.removeAll { it.kind == NpcKind.SYNDICATE_ENEMY || it.kind == NpcKind.SYNDICATE_BOSS || it.kind == NpcKind.POLICE_OFFICER }
            listener.onHealthChanged(playerHealth, playerArmor)
        }
    }

    fun step(dtRaw: Float) {
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        synchronized(lock) {
            animClock += dt

            // Day-Night Cycle update
            if (timeCycleSpeed.hoursPerRealMinute > 0f) {
                timeOfDayHours = (timeOfDayHours + dt * (timeCycleSpeed.hoursPerRealMinute / 60f)) % 24f
            }

            if (isCutsceneActive) {
                updateCutsceneCameraLocked(dt)
                return
            }

            if (weaponCooldown > 0f) weaponCooldown -= dt
            if (muzzleFlashTimer > 0f) muzzleFlashTimer -= dt

            // Player update
            val car = drivingVehicle
            if (car != null) {
                updateDrivenVehicleLocked(car, dt)
                playerX = car.x
                playerZ = car.z
                playerYaw = car.yaw
            } else {
                updatePlayerOnFootLocked(dt)
                if (isFiring && weaponCooldown <= 0f) {
                    fireWeaponLocked()
                }
            }

            // Traffic & AI vehicles
            updateTrafficVehiclesLocked(dt)

            // Physics props (crates, barrels)
            updatePhysicsPropsLocked(dt)

            // NPCs, Enemies, Police
            updateNpcsLocked(dt)

            // Projectiles & Particles & Pickups
            updateProjectilesLocked(dt)
            updateParticlesLocked(dt)
            updatePickupsLocked(dt)

            // Wanted system & Police spawns
            updateWantedLocked(dt)

            // Mission progression
            updateMissionStageLocked(dt)

            // Smooth 3rd-person follow camera
            updateFollowCameraLocked(dt)
        }
    }

    private fun updatePlayerOnFootLocked(dt: Float) {
        val mag = sqrt(joyX * joyX + joyY * joyY).coerceAtMost(1f)
        if (mag > 0.08f) {
            val moveAngle = camYaw + atan2(-joyX, -joyY)
            val speed = (if (isSprinting) 9.8f else 6.2f) * mag
            val totalDx = sin(moveAngle) * speed * dt
            val totalDz = cos(moveAngle) * speed * dt
            val stepDist = sqrt(totalDx * totalDx + totalDz * totalDz)
            val subSteps = (stepDist / 0.35f).toInt().coerceIn(1, 5)
            val stepDx = totalDx / subSteps
            val stepDz = totalDz / subSteps
            for (s in 0 until subSteps) {
                val resolved = resolveCircleWorldCollisionLocked(playerX + stepDx, playerZ + stepDz, 0.62f)
                playerX = resolved.first
                playerZ = resolved.second
            }
            playerYaw = lerpAngle(playerYaw, moveAngle, dt * 12f)
            playerWalkPhase += dt * speed * 1.9f
        } else {
            val resolved = resolveCircleWorldCollisionLocked(playerX, playerZ, 0.62f)
            playerX = resolved.first
            playerZ = resolved.second
            playerWalkPhase = 0f
        }
    }

    private fun updateDrivenVehicleLocked(car: VehicleEntity, dt: Float) {
        val accelInput = (if (isGas) 1f else 0f) - (if (isBrake) 1f else 0f)
        val clampedAccel = accelInput.coerceIn(-1f, 1f)
        val arrowSteer = (if (steerLeft) 1f else 0f) - (if (steerRight) 1f else 0f)
        val steerInput = if (arrowSteer != 0f) arrowSteer else -joyX.coerceIn(-1f, 1f)

        // Steering
        val targetSteer = steerInput * 0.65f
        car.steerAngle += (targetSteer - car.steerAngle) * (dt * 9f).coerceAtMost(1f)

        // Longitudinal velocity
        if (clampedAccel > 0.05f) {
            car.speed += clampedAccel * car.model.acceleration * dt
        } else if (clampedAccel < -0.05f) {
            car.speed += clampedAccel * (car.model.acceleration * 1.25f) * dt
        } else {
            car.speed *= (1f - (dt * 1.6f).coerceAtMost(0.5f))
        }
        if (isHandbrake) {
            car.speed *= (1f - (dt * 2.8f).coerceAtMost(0.5f))
        }
        car.speed = car.speed.coerceIn(-car.model.maxSpeed * 0.4f, car.model.maxSpeed)

        // Turning proportional to speed
        val speedFactor = (abs(car.speed) / 7f).coerceIn(0f, 1f)
        val directionSign = if (car.speed >= -0.2f) 1f else -1f
        val turnRate = car.steerAngle * car.model.handling * speedFactor * directionSign
        car.yaw += turnRate * dt

        // Drift / lateral slip
        val grip = if (isHandbrake) 2.2f else 6.5f
        car.lateralVel += (turnRate * car.speed * 0.28f - car.lateralVel * grip) * dt

        // Suspension body pitch & roll
        val targetPitch = (-clampedAccel * 0.045f).coerceIn(-0.07f, 0.07f)
        val targetRoll = (turnRate * abs(car.speed) * 0.0055f).coerceIn(-0.11f, 0.11f)
        car.pitch += (targetPitch - car.pitch) * (dt * 8f).coerceAtMost(1f)
        car.roll += (targetRoll - car.roll) * (dt * 8f).coerceAtMost(1f)
        car.wheelSpin += car.speed * dt * 1.6f

        // Tire drift smoke particles
        if ((isHandbrake || abs(car.lateralVel) > 2.8f) && abs(car.speed) > 8f && particles.size < 110) {
            particles.add(
                Particle(
                    x = car.x - sin(car.yaw) * (car.model.length * 0.42f),
                    y = 0.25f,
                    z = car.z - cos(car.yaw) * (car.model.length * 0.42f),
                    vx = (rng.nextFloat() - 0.5f) * 1.5f,
                    vy = 1.2f,
                    vz = (rng.nextFloat() - 0.5f) * 1.5f,
                    life = 0.45f,
                    maxLife = 0.45f,
                    size = 0.65f,
                    r = 0.72f, g = 0.74f, b = 0.78f
                )
            )
        }

        // Move and sub-step collide car chassis (center + front/rear bumpers) against all buildings
        val forwardX = sin(car.yaw)
        val forwardZ = cos(car.yaw)
        val rightX = cos(car.yaw)
        val rightZ = -sin(car.yaw)

        val moveDx = (forwardX * car.speed + rightX * car.lateralVel) * dt
        val moveDz = (forwardZ * car.speed + rightZ * car.lateralVel) * dt
        val moveDist = sqrt(moveDx * moveDx + moveDz * moveDz)
        val subSteps = (moveDist / 0.45f).toInt().coerceIn(1, 8)
        val stepDx = moveDx / subSteps
        val stepDz = moveDz / subSteps

        val radius = car.model.width * 0.58f
        val bumperRadius = car.model.width * 0.50f
        val bumperOffset = car.model.length * 0.34f
        var hitBuildingAny = false

        for (s in 0 until subSteps) {
            car.x += stepDx
            car.z += stepDz

            // Center hull
            val (cx, cz, hitC) = resolveCircleBuildingCollisionLocked(car.x, car.z, radius)
            car.x = cx
            car.z = cz
            if (hitC) hitBuildingAny = true

            // Front bumper hull
            val fX = car.x + forwardX * bumperOffset
            val fZ = car.z + forwardZ * bumperOffset
            val (rfx, rfz, hitF) = resolveCircleBuildingCollisionLocked(fX, fZ, bumperRadius)
            if (hitF) {
                car.x += (rfx - fX)
                car.z += (rfz - fZ)
                hitBuildingAny = true
            }

            // Rear bumper hull
            val rX = car.x - forwardX * bumperOffset
            val rZ = car.z - forwardZ * bumperOffset
            val (rrx, rrz, hitR) = resolveCircleBuildingCollisionLocked(rX, rZ, bumperRadius)
            if (hitR) {
                car.x += (rrx - rX)
                car.z += (rrz - rZ)
                hitBuildingAny = true
            }
        }

        if (hitBuildingAny) {
            if (abs(car.speed) > 6f) {
                listener.onPlayCollision()
                spawnSparksLocked(car.x + forwardX * bumperOffset, 0.7f, car.z + forwardZ * bumperOffset, 7, 1f, 0.75f, 0.2f)
                car.speed = -car.speed * 0.32f
            } else {
                car.speed *= 0.2f
            }
            car.lateralVel *= 0.2f
        }

        // Vehicle vs Props (knock crates & barrels flying!)
        for (p in world.props) {
            val dx = p.x - car.x
            val dz = p.z - car.z
            val d2 = dx * dx + dz * dz
            val minD = radius + p.size * 0.55f
            if (d2 < minD * minD && d2 > 0.0001f) {
                val d = sqrt(d2)
                val nxDir = dx / d
                val nzDir = dz / d
                val impulse = (abs(car.speed) * 0.85f).coerceAtLeast(5f)
                p.vx += nxDir * impulse
                p.vy += 4.2f + impulse * 0.18f
                p.vz += nzDir * impulse
                spawnSparksLocked(p.x, 0.6f, p.z, 4, 0.9f, 0.6f, 0.3f)
            }
        }

        // Vehicle vs Other Vehicles
        for (other in world.vehicles) {
            if (other.id == car.id) continue
            val dx = other.x - car.x
            val dz = other.z - car.z
            val d2 = dx * dx + dz * dz
            val minD = 2.6f
            if (d2 < minD * minD && d2 > 0.001f) {
                val d = sqrt(d2)
                val overlap = minD - d
                val nxDir = dx / d
                val nzDir = dz / d
                car.x -= nxDir * overlap * 0.5f
                car.z -= nzDir * overlap * 0.5f
                other.x += nxDir * overlap * 0.5f
                other.z += nzDir * overlap * 0.5f
                if (abs(car.speed) > 8f) {
                    listener.onPlayCollision()
                    spawnSparksLocked((car.x + other.x) * 0.5f, 0.8f, (car.z + other.z) * 0.5f, 6, 1f, 0.8f, 0.3f)
                    car.speed *= 0.65f
                }
            }
        }

        // Vehicle hitting hostile NPCs
        if (abs(car.speed) > 7f) {
            val iter = world.npcs.iterator()
            while (iter.hasNext()) {
                val npc = iter.next()
                val dx = npc.x - car.x
                val dz = npc.z - car.z
                if (dx * dx + dz * dz < 2.2f * 2.2f) {
                    if (npc.kind == NpcKind.STORY_NPC) {
                        npc.x += (dx + 0.1f) * 1.1f
                        npc.z += (dz + 0.1f) * 1.1f
                    } else {
                        npc.hp -= 55
                        npc.x += sin(car.yaw) * 2.5f
                        npc.z += cos(car.yaw) * 2.5f
                        spawnSparksLocked(npc.x, 0.9f, npc.z, 5, 0.9f, 0.2f, 0.2f)
                        if (npc.hp <= 0) {
                            onNpcEliminatedLocked(npc)
                            iter.remove()
                        } else if (npc.kind == NpcKind.PEDESTRIAN) {
                            escalateWantedLocked(1)
                        }
                    }
                }
            }
        }
    }

    private fun updateTrafficVehiclesLocked(dt: Float) {
        val maxTraffic = graphicsQuality.maxTraffic
        var activeTraffic = 0
        for (v in world.vehicles) {
            if (v.id == drivingVehicle?.id) continue
            if (v.isPolice && wantedStars > 0) {
                v.sirenTimer += dt * 7f
                val dx = playerX - v.x
                val dz = playerZ - v.z
                val dist = sqrt(dx * dx + dz * dz).coerceAtLeast(0.1f)
                val desiredYaw = atan2(dx, dz)
                v.yaw = lerpAngle(v.yaw, desiredYaw, dt * 2.5f)
                val chaseSpeed = if (dist > 11f) 16f else 4f
                val nx = v.x + sin(v.yaw) * chaseSpeed * dt
                val nz = v.z + cos(v.yaw) * chaseSpeed * dt
                val (rx, rz, _) = resolveCircleBuildingCollisionLocked(nx, nz, 1.4f)
                v.x = rx
                v.z = rz
                v.wheelSpin += chaseSpeed * dt * 1.4f
            } else if (v.isTraffic) {
                activeTraffic++
                if (activeTraffic > maxTraffic) continue
                val nx = v.x + v.trafficDirX * v.speed * dt
                val nz = v.z + v.trafficDirZ * v.speed * dt
                if (abs(nx) > 155f || abs(nz) > 155f) {
                    v.trafficDirX = -v.trafficDirX
                    v.trafficDirZ = -v.trafficDirZ
                    v.yaw = atan2(v.trafficDirX, v.trafficDirZ)
                } else {
                    val (rx, rz, hit) = resolveCircleBuildingCollisionLocked(nx, nz, 1.4f)
                    v.x = rx
                    v.z = rz
                    if (hit) {
                        v.trafficDirX = -v.trafficDirX
                        v.trafficDirZ = -v.trafficDirZ
                        v.yaw = atan2(v.trafficDirX, v.trafficDirZ)
                    }
                }
                v.wheelSpin += v.speed * dt * 1.3f
            }
        }
    }

    private fun updatePhysicsPropsLocked(dt: Float) {
        for (p in world.props) {
            if (abs(p.vx) > 0.05f || abs(p.vz) > 0.05f || p.y > p.size * 0.5f || p.vy > 0.05f) {
                p.x += p.vx * dt
                p.z += p.vz * dt
                p.vy -= 18f * dt
                p.y += p.vy * dt
                p.yaw += (p.vx + p.vz) * dt * 0.5f
                if (p.y <= p.size * 0.5f) {
                    p.y = p.size * 0.5f
                    if (p.vy < -2f) p.vy = -p.vy * 0.35f else p.vy = 0f
                    p.vx *= (1f - dt * 4.5f).coerceAtLeast(0f)
                    p.vz *= (1f - dt * 4.5f).coerceAtLeast(0f)
                }
                val (rx, rz, _) = resolveCircleBuildingCollisionLocked(p.x, p.z, p.size * 0.5f)
                p.x = rx
                p.z = rz
            }
        }
    }

    private fun updateNpcsLocked(dt: Float) {
        val iter = world.npcs.iterator()
        while (iter.hasNext()) {
            val npc = iter.next()
            if (npc.attackCooldown > 0f) npc.attackCooldown -= dt
            val dx = playerX - npc.x
            val dz = playerZ - npc.z
            val distSq = dx * dx + dz * dz
            val dist = sqrt(distSq).coerceAtLeast(0.01f)

            when (npc.kind) {
                NpcKind.STORY_NPC -> {
                    if (dist < 14f) {
                        npc.yaw = lerpAngle(npc.yaw, atan2(dx, dz), dt * 4f)
                    }
                }
                NpcKind.PEDESTRIAN -> {
                    npc.patrolTimer -= dt
                    if (npc.patrolTimer <= 0f) {
                        npc.patrolTimer = 2.5f + rng.nextFloat() * 3f
                        npc.yaw += (rng.nextFloat() - 0.5f) * 2.2f
                    }
                    val nx = npc.x + sin(npc.yaw) * 1.8f * dt
                    val nz = npc.z + cos(npc.yaw) * 1.8f * dt
                    val (rx, rz, hit) = resolveCircleBuildingCollisionLocked(nx, nz, 0.5f)
                    if (hit || abs(rx) > 155f || abs(rz) > 155f) {
                        npc.yaw += PI.toFloat() * 0.8f
                    } else {
                        npc.x = rx
                        npc.z = rz
                        npc.walkPhase += dt * 4.5f
                    }
                }
                NpcKind.SYNDICATE_ENEMY, NpcKind.SYNDICATE_BOSS, NpcKind.POLICE_OFFICER -> {
                    if (npc.kind == NpcKind.POLICE_OFFICER && wantedStars == 0) {
                        iter.remove()
                        continue
                    }
                    npc.yaw = lerpAngle(npc.yaw, atan2(dx, dz), dt * 6f)
                    if (dist > 11f && dist < 85f) {
                        val moveSpd = if (npc.kind == NpcKind.SYNDICATE_BOSS) 5.2f else 4.4f
                        val nx = npc.x + (dx / dist) * moveSpd * dt
                        val nz = npc.z + (dz / dist) * moveSpd * dt
                        val (rx, rz, _) = resolveCircleBuildingCollisionLocked(nx, nz, 0.5f)
                        npc.x = rx
                        npc.z = rz
                        npc.walkPhase += dt * 6.5f
                    }
                    if (dist < 34f && npc.attackCooldown <= 0f) {
                        npc.attackCooldown = if (npc.kind == NpcKind.SYNDICATE_BOSS) 0.75f else 1.25f
                        val spread = (rng.nextFloat() - 0.5f) * 0.14f
                        val aimYaw = atan2(dx, dz) + spread
                        projectiles.add(
                            Projectile(
                                x = npc.x + sin(aimYaw) * 0.7f,
                                y = 1.2f,
                                z = npc.z + cos(aimYaw) * 0.7f,
                                vx = sin(aimYaw) * 42f,
                                vy = 0f,
                                vz = cos(aimYaw) * 42f,
                                life = 1.1f,
                                fromPlayer = false,
                                damage = if (npc.kind == NpcKind.SYNDICATE_BOSS) 14 else 9,
                                r = 1.0f, g = 0.28f, b = 0.18f
                            )
                        )
                    }
                }
            }
        }
    }

    private fun fireWeaponLocked() {
        val w = equippedWeapon
        weaponCooldown = w.cooldownSec
        if (w == WeaponType.FISTS) {
            listener.onPlayWeaponSound(w)
            val punchX = playerX + sin(playerYaw) * 1.15f
            val punchZ = playerZ + cos(playerYaw) * 1.15f
            spawnSparksLocked(punchX, 1.25f, punchZ, 4, 0.95f, 0.90f, 0.70f)
            val iter = world.npcs.iterator()
            while (iter.hasNext()) {
                val npc = iter.next()
                if (npc.kind == NpcKind.STORY_NPC) continue
                val dx = npc.x - playerX
                val dz = npc.z - playerZ
                if (dx * dx + dz * dz < w.range * w.range) {
                    npc.hp -= w.damage
                    spawnSparksLocked(npc.x, 1.2f, npc.z, 5, 0.95f, 0.25f, 0.2f)
                    if (npc.hp <= 0) {
                        onNpcEliminatedLocked(npc)
                        iter.remove()
                    } else if (npc.kind == NpcKind.PEDESTRIAN) {
                        escalateWantedLocked(1)
                    }
                    break
                }
            }
            return
        }

        muzzleFlashTimer = 0.08f
        listener.onPlayWeaponSound(w)

        // Auto-aim slight assist toward nearest hostile within 36m & 45 deg cone
        var aimYaw = playerYaw
        var bestDist = w.range
        for (npc in world.npcs) {
            if (npc.kind == NpcKind.SYNDICATE_ENEMY || npc.kind == NpcKind.SYNDICATE_BOSS || npc.kind == NpcKind.POLICE_OFFICER) {
                val dx = npc.x - playerX
                val dz = npc.z - playerZ
                val d = sqrt(dx * dx + dz * dz)
                if (d < bestDist) {
                    val targetAngle = atan2(dx, dz)
                    val diff = abs(normalizeAngle(targetAngle - camYaw))
                    if (diff < 0.75f) {
                        bestDist = d
                        aimYaw = targetAngle
                    }
                }
            }
        }
        playerYaw = aimYaw

        repeat(w.pellets) {
            val spread = (rng.nextFloat() - 0.5f) * w.spreadRad
            val angle = aimYaw + spread
            projectiles.add(
                Projectile(
                    x = playerX + sin(angle) * 0.85f,
                    y = 1.25f,
                    z = playerZ + cos(angle) * 0.85f,
                    vx = sin(angle) * 62f,
                    vy = 0f,
                    vz = cos(angle) * 62f,
                    life = w.range / 62f,
                    fromPlayer = true,
                    damage = w.damage,
                    r = 1.0f, g = 0.88f, b = 0.35f
                )
            )
        }
        spawnSparksLocked(playerX + sin(aimYaw) * 0.9f, 1.25f, playerZ + cos(aimYaw) * 0.9f, 3, 1f, 0.85f, 0.3f)
    }

    private fun updateProjectilesLocked(dt: Float) {
        val pIter = projectiles.iterator()
        while (pIter.hasNext()) {
            val proj = pIter.next()
            proj.x += proj.vx * dt
            proj.y += proj.vy * dt
            proj.z += proj.vz * dt
            proj.life -= dt

            if (proj.life <= 0f) {
                pIter.remove()
                continue
            }

            // Building hit
            val (_, _, hitB) = resolveCircleBuildingCollisionLocked(proj.x, proj.z, 0.25f)
            if (hitB) {
                spawnSparksLocked(proj.x, proj.y, proj.z, 4, 0.95f, 0.75f, 0.3f)
                pIter.remove()
                continue
            }

            // Prop hit
            var hitProp = false
            for (prop in world.props) {
                val dx = prop.x - proj.x
                val dz = prop.z - proj.z
                if (dx * dx + dz * dz < (prop.size * 0.7f) * (prop.size * 0.7f)) {
                    prop.vx += proj.vx * 0.14f
                    prop.vy += 2.2f
                    prop.vz += proj.vz * 0.14f
                    spawnSparksLocked(proj.x, proj.y, proj.z, 4, 0.9f, 0.55f, 0.25f)
                    hitProp = true
                    break
                }
            }
            if (hitProp) {
                pIter.remove()
                continue
            }

            if (proj.fromPlayer) {
                var hitNpc = false
                val nIter = world.npcs.iterator()
                while (nIter.hasNext()) {
                    val npc = nIter.next()
                    if (npc.kind == NpcKind.STORY_NPC) continue
                    val dx = npc.x - proj.x
                    val dz = npc.z - proj.z
                    if (dx * dx + dz * dz < 0.85f * 0.85f) {
                        npc.hp -= proj.damage
                        spawnSparksLocked(proj.x, 1.15f, proj.z, 5, 0.95f, 0.25f, 0.2f)
                        if (npc.hp <= 0) {
                            onNpcEliminatedLocked(npc)
                            nIter.remove()
                        } else if (npc.kind == NpcKind.PEDESTRIAN) {
                            escalateWantedLocked(1)
                        }
                        hitNpc = true
                        break
                    }
                }
                if (hitNpc) pIter.remove()
            } else {
                // Enemy bullet hitting George Nikoladze
                val dx = playerX - proj.x
                val dz = playerZ - proj.z
                val hitRadius = if (drivingVehicle != null) 1.5f else 0.75f
                if (dx * dx + dz * dz < hitRadius * hitRadius) {
                    val rawDmg = if (drivingVehicle != null) (proj.damage / 2).coerceAtLeast(2) else proj.damage
                    applyDamageToPlayerLocked(rawDmg)
                    spawnSparksLocked(proj.x, 1.1f, proj.z, 4, 0.9f, 0.15f, 0.15f)
                    pIter.remove()
                }
            }
        }
    }

    private fun onNpcEliminatedLocked(npc: NpcEntity) {
        if (npc.kind == NpcKind.PEDESTRIAN) {
            escalateWantedLocked(1)
        } else {
            // Drop $25 cash orb
            pickups.add(PickupOrb(npc.x, npc.z, cashAmount = 25, healAmount = if (rng.nextFloat() < 0.35f) 15 else 0))
        }
    }

    private fun applyDamageToPlayerLocked(dmg: Int) {
        if (playerArmor > 0) {
            val absorbed = minOf(playerArmor, dmg)
            playerArmor -= absorbed
            val rem = dmg - absorbed
            playerHealth = (playerHealth - rem).coerceAtLeast(0)
        } else {
            playerHealth = (playerHealth - dmg).coerceAtLeast(0)
        }
        listener.onHealthChanged(playerHealth, playerArmor)
        if (playerHealth <= 0) {
            listener.onPlayerWasted()
        }
    }

    private fun escalateWantedLocked(starsToAdd: Int) {
        wantedStars = (wantedStars + starsToAdd).coerceAtMost(3)
        wantedDecayTimer = 22f
    }

    private fun updateWantedLocked(dt: Float) {
        if (wantedStars <= 0) return
        wantedDecayTimer -= dt
        if (wantedDecayTimer <= 0f) {
            wantedStars = (wantedStars - 1).coerceAtLeast(0)
            wantedDecayTimer = 18f
            return
        }
        policeSpawnTimer -= dt
        val currentPoliceCount = world.npcs.count { it.kind == NpcKind.POLICE_OFFICER }
        val desiredPolice = wantedStars * 2
        if (policeSpawnTimer <= 0f && currentPoliceCount < desiredPolice) {
            policeSpawnTimer = 6f
            val angle = rng.nextFloat() * 6.28f
            val (sx, sz) = resolveCircleWorldCollisionLocked(
                (playerX + sin(angle) * 32f).coerceIn(-160f, 160f),
                (playerZ + cos(angle) * 32f).coerceIn(-160f, 160f),
                0.7f
            )
            world.npcs.add(
                NpcEntity(
                    id = nextEntityId++,
                    kind = NpcKind.POLICE_OFFICER,
                    name = "Patrol Officer",
                    x = sx,
                    z = sz,
                    hp = 85,
                    maxHp = 85,
                    shirtR = 0.14f,
                    shirtG = 0.34f,
                    shirtB = 0.85f
                )
            )
            // Also ensure at least 1 police cruiser exists
            if (world.vehicles.none { it.isPolice }) {
                val (cx, cz) = resolveCircleWorldCollisionLocked((sx + 4f).coerceIn(-160f, 160f), sz, 1.6f)
                world.vehicles.add(
                    VehicleEntity(
                        id = nextEntityId++,
                        model = VehicleModel.SEDAN,
                        x = cx,
                        z = cz,
                        yaw = 0f,
                        r = 0.12f, g = 0.18f, b = 0.35f,
                        isPolice = true
                    )
                )
            }
        }
    }

    private fun setupCurrentMissionStageLocked() {
        val mission = activeMission ?: return
        val stage = mission.stages.getOrNull(activeStageIdx) ?: return
        missionTimerSec = stage.timeLimitSec
        pickupTargetActive = stage.objectiveType == MissionObjectiveType.PICKUP_ITEM
        pickupX = stage.targetX
        pickupZ = stage.targetZ

        if (stage.wantedStars > wantedStars) {
            wantedStars = stage.wantedStars
            wantedDecayTimer = 35f
        }

        if (stage.objectiveType == MissionObjectiveType.ELIMINATE_TARGETS && stage.enemyCount > 0) {
            for (i in 0 until stage.enemyCount) {
                val angle = (2.0 * PI * i / stage.enemyCount).toFloat()
                val (ex, ez) = resolveCircleWorldCollisionLocked(
                    (stage.targetX + sin(angle) * 7f).coerceIn(-165f, 165f),
                    (stage.targetZ + cos(angle) * 7f).coerceIn(-165f, 165f),
                    0.7f
                )
                val isBoss = mission.id == "m6_finale" && i == 0
                world.npcs.add(
                    NpcEntity(
                        id = nextEntityId++,
                        kind = if (isBoss) NpcKind.SYNDICATE_BOSS else NpcKind.SYNDICATE_ENEMY,
                        name = if (isBoss) "Boss Vakhtang" else "Syndicate Gunman",
                        x = ex,
                        z = ez,
                        hp = if (isBoss) 260 else 95,
                        maxHp = if (isBoss) 260 else 95,
                        shirtR = if (isBoss) 0.88f else 0.65f,
                        shirtG = if (isBoss) 0.14f else 0.16f,
                        shirtB = if (isBoss) 0.14f else 0.20f
                    )
                )
            }
        }
    }

    private fun updateMissionStageLocked(dt: Float) {
        val mission = activeMission ?: return
        val stage = mission.stages.getOrNull(activeStageIdx) ?: return

        if (stage.timeLimitSec > 0f) {
            missionTimerSec -= dt
            if (missionTimerSec <= 0f) {
                activeMission = null
                pickupTargetActive = false
                world.npcs.removeAll { it.kind == NpcKind.SYNDICATE_ENEMY || it.kind == NpcKind.SYNDICATE_BOSS }
                listener.onMissionFailed("Time expired before reaching the relay tower!")
                return
            }
        }

        val dx = playerX - stage.targetX
        val dz = playerZ - stage.targetZ
        val dist = sqrt(dx * dx + dz * dz)

        val stageComplete = when (stage.objectiveType) {
            MissionObjectiveType.GO_TO_POINT,
            MissionObjectiveType.TIMED_REACH -> dist < 7.5f
            MissionObjectiveType.PICKUP_ITEM -> dist < 3.8f
            MissionObjectiveType.ELIMINATE_TARGETS -> {
                world.npcs.none { it.kind == NpcKind.SYNDICATE_ENEMY || it.kind == NpcKind.SYNDICATE_BOSS }
            }
            MissionObjectiveType.ESCAPE_WANTED -> wantedStars == 0
        }

        if (stageComplete) {
            activeStageIdx++
            if (activeStageIdx >= mission.stages.size) {
                // Trigger Outro Cutscene!
                isCutsceneActive = true
                cutsceneFocus = CutsceneCameraFocus.TWO_SHOT
                cutsceneNpcX = playerX + 2.5f
                cutsceneNpcZ = playerZ
                listener.onStartCutscene(mission, isOutro = true)
            } else {
                setupCurrentMissionStageLocked()
            }
        }
    }

    private fun updateParticlesLocked(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.z += p.vz * dt
            p.vy -= 9f * dt
            if (p.y < 0.05f) p.y = 0.05f
            p.life -= dt
            if (p.life <= 0f) iter.remove()
        }
    }

    private fun updatePickupsLocked(dt: Float) {
        val iter = pickups.iterator()
        while (iter.hasNext()) {
            val orb = iter.next()
            orb.life -= dt
            val dx = playerX - orb.x
            val dz = playerZ - orb.z
            if (dx * dx + dz * dz < 2.6f * 2.6f) {
                if (orb.cashAmount > 0) {
                    playerCash += orb.cashAmount
                    listener.onCashGained(orb.cashAmount)
                    listener.onPlayCashPickup()
                }
                if (orb.healAmount > 0) {
                    playerHealth = (playerHealth + orb.healAmount).coerceAtMost(100)
                    listener.onHealthChanged(playerHealth, playerArmor)
                }
                iter.remove()
            } else if (orb.life <= 0f) {
                iter.remove()
            }
        }
    }

    private fun spawnSparksLocked(x: Float, y: Float, z: Float, count: Int, r: Float, g: Float, b: Float) {
        if (particles.size > 110) return
        repeat(count) {
            particles.add(
                Particle(
                    x = x, y = y, z = z,
                    vx = (rng.nextFloat() - 0.5f) * 8f,
                    vy = 2f + rng.nextFloat() * 5f,
                    vz = (rng.nextFloat() - 0.5f) * 8f,
                    life = 0.35f, maxLife = 0.35f,
                    size = 0.25f, r = r, g = g, b = b
                )
            )
        }
    }

    private fun updateFollowCameraLocked(dt: Float) {
        val inCar = drivingVehicle != null
        val dist = when {
            inCar && isPortraitViewport -> 14.2f
            inCar -> 11.5f
            isPortraitViewport -> 8.6f
            else -> 6.8f
        }
        val heightOffset = when {
            inCar && isPortraitViewport -> 5.2f
            inCar -> 4.4f
            isPortraitViewport -> 3.3f
            else -> 2.8f
        }

        if (inCar && dynamicCameraTilt) {
            val car = drivingVehicle!!
            if (abs(car.speed) > 4f) {
                camYaw = lerpAngle(camYaw, car.yaw, dt * 1.9f)
            }
        }

        val horiz = cos(camPitch) * dist
        val vert = sin(camPitch) * dist + heightOffset * 0.35f

        val targetCamX = playerX - sin(camYaw) * horiz
        val targetCamY = vert.coerceAtLeast(2.2f)
        val targetCamZ = playerZ - cos(camYaw) * horiz

        // Prevent third-person camera from entering inside buildings
        var safeCamX = targetCamX
        var safeCamZ = targetCamZ
        for (b in world.buildings) {
            if (targetCamY < b.height + 0.8f) {
                val hw = b.width * 0.5f + 0.7f
                val hd = b.depth * 0.5f + 0.7f
                if (safeCamX in (b.x - hw)..(b.x + hw) && safeCamZ in (b.z - hd)..(b.z + hd)) {
                    val leftP = safeCamX - (b.x - hw)
                    val rightP = (b.x + hw) - safeCamX
                    val topP = safeCamZ - (b.z - hd)
                    val botP = (b.z + hd) - safeCamZ
                    val m = minOf(leftP, rightP, topP, botP)
                    when (m) {
                        leftP -> safeCamX = b.x - hw
                        rightP -> safeCamX = b.x + hw
                        topP -> safeCamZ = b.z - hd
                        else -> safeCamZ = b.z + hd
                    }
                }
            }
        }

        val smooth = (dt * 10f).coerceAtMost(1f)
        camPosX += (safeCamX - camPosX) * smooth
        camPosY += (targetCamY - camPosY) * smooth
        camPosZ += (safeCamZ - camPosZ) * smooth

        val targetLookX = playerX + sin(camYaw) * 2.2f
        val targetLookY = if (inCar) 1.3f else 1.65f
        val targetLookZ = playerZ + cos(camYaw) * 2.2f

        camLookX += (targetLookX - camLookX) * smooth
        camLookY += (targetLookY - camLookY) * smooth
        camLookZ += (targetLookZ - camLookZ) * smooth
    }

    private fun updateCutsceneCameraLocked(dt: Float) {
        val orbitAngle = animClock * 0.35f
        val (tx, ty, tz, lx, ly, lz) = when (cutsceneFocus) {
            CutsceneCameraFocus.GEORGE_CLOSEUP -> {
                val a = playerYaw + 0.35f
                listOf(
                    playerX + sin(a) * 3.1f, 2.0f, playerZ + cos(a) * 3.1f,
                    playerX, 1.6f, playerZ
                )
            }
            CutsceneCameraFocus.NPC_CLOSEUP -> {
                val a = playerYaw + PI.toFloat() + 0.35f
                listOf(
                    cutsceneNpcX + sin(a) * 3.2f, 2.0f, cutsceneNpcZ + cos(a) * 3.2f,
                    cutsceneNpcX, 1.6f, cutsceneNpcZ
                )
            }
            CutsceneCameraFocus.TWO_SHOT -> {
                val mx = (playerX + cutsceneNpcX) * 0.5f
                val mz = (playerZ + cutsceneNpcZ) * 0.5f
                listOf(
                    mx + sin(orbitAngle) * 5.8f, 2.6f, mz + cos(orbitAngle) * 5.8f,
                    mx, 1.5f, mz
                )
            }
            CutsceneCameraFocus.WIDE_DISTRICT -> {
                listOf(
                    playerX + sin(orbitAngle) * 18f, 11f, playerZ + cos(orbitAngle) * 18f,
                    playerX, 2.5f, playerZ
                )
            }
        }.let { FloatArray(6) { i -> it[i] } }.let { arr ->
            SixFloats(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5])
        }

        val s = (dt * 4.5f).coerceAtMost(1f)
        camPosX += (tx - camPosX) * s
        camPosY += (ty - camPosY) * s
        camPosZ += (tz - camPosZ) * s
        camLookX += (lx - camLookX) * s
        camLookY += (ly - camLookY) * s
        camLookZ += (lz - camLookZ) * s
    }

    private data class SixFloats(val a: Float, val b: Float, val c: Float, val d: Float, val e: Float, val f: Float)

    private fun resolveCircleWorldCollisionLocked(x: Float, z: Float, radius: Float): Pair<Float, Float> {
        val (rx, rz, _) = resolveCircleBuildingCollisionLocked(x, z, radius)
        return rx to rz
    }

    private fun resolveCircleBuildingCollisionLocked(x: Float, z: Float, radius: Float): Triple<Float, Float, Boolean> {
        var rx = x.coerceIn(-CityWorldBuilder.WORLD_HALF_EXTENT + 3f, CityWorldBuilder.WORLD_HALF_EXTENT - 3f)
        var rz = z.coerceIn(-CityWorldBuilder.WORLD_HALF_EXTENT + 3f, CityWorldBuilder.WORLD_HALF_EXTENT - 3f)
        var collided = rx != x || rz != z

        // Prevent falling into Mtkvari River unless on a bridge (Z near -164, -120, -60, 0, 60, 120, 164)
        val onBridge = abs(rz) < 7.5f ||
            abs(rz - 60f) < 7.5f || abs(rz + 60f) < 7.5f ||
            abs(rz - 120f) < 7.5f || abs(rz + 120f) < 7.5f ||
            abs(rz - 164f) < 9.5f || abs(rz + 164f) < 9.5f
        if (!onBridge && abs(rx) < 10.5f + radius) {
            rx = if (rx >= 0f) 10.5f + radius else -(10.5f + radius)
            collided = true
        }

        // Two solver passes guarantee solid corners and prevent passing through adjacent buildings
        repeat(2) {
            for (b in world.buildings) {
                val halfW = b.width * 0.5f
                val halfD = b.depth * 0.5f
                // Fast bounding rejection
                if (rx + radius <= b.x - halfW || rx - radius >= b.x + halfW ||
                    rz + radius <= b.z - halfD || rz - radius >= b.z + halfD
                ) {
                    continue
                }

                val closestX = rx.coerceIn(b.x - halfW, b.x + halfW)
                val closestZ = rz.coerceIn(b.z - halfD, b.z + halfD)
                val dx = rx - closestX
                val dz = rz - closestZ
                val distSq = dx * dx + dz * dz

                if (distSq <= 0.0001f) {
                    // Center is inside the building AABB: push cleanly out along nearest exterior face
                    collided = true
                    val leftPen = rx - (b.x - halfW)
                    val rightPen = (b.x + halfW) - rx
                    val topPen = rz - (b.z - halfD)
                    val botPen = (b.z + halfD) - rz
                    val minPen = minOf(leftPen, rightPen, topPen, botPen)
                    when (minPen) {
                        leftPen -> rx = b.x - halfW - radius
                        rightPen -> rx = b.x + halfW + radius
                        topPen -> rz = b.z - halfD - radius
                        else -> rz = b.z + halfD + radius
                    }
                } else if (distSq < radius * radius) {
                    collided = true
                    val d = sqrt(distSq)
                    val push = radius - d
                    rx += (dx / d) * push
                    rz += (dz / d) * push
                }
            }
        }
        return Triple(rx, rz, collided)
    }

    private fun findNearestVehicleLocked(maxDist: Float): VehicleEntity? {
        var best: VehicleEntity? = null
        var bestSq = maxDist * maxDist
        for (v in world.vehicles) {
            val dx = v.x - playerX
            val dz = v.z - playerZ
            val d2 = dx * dx + dz * dz
            if (d2 < bestSq) {
                bestSq = d2
                best = v
            }
        }
        return best
    }

    private fun findNearbyInteractableLocked(): NearbyInteractable? {
        if (drivingVehicle != null) return null

        // 1. Mission Start markers (only if no active mission)
        if (activeMission == null) {
            for (m in GameCatalog.missions) {
                val dx = m.startX - playerX
                val dz = m.startZ - playerZ
                if (dx * dx + dz * dz < 6.5f * 6.5f) {
                    val doneLabel = if (completedMissions.contains(m.id)) "Replay Mission ($300)" else "Start Mission ($300)"
                    return NearbyInteractable(InteractableType.MISSION_START, m.id, "M${m.number}: ${m.title}", doneLabel)
                }
            }
        }

        // 2. Weapon Shop
        run {
            val dx = GameCatalog.WEAPON_SHOP_X - playerX
            val dz = GameCatalog.WEAPON_SHOP_Z - playerZ
            if (dx * dx + dz * dz < 6.0f * 6.0f) {
                return NearbyInteractable(InteractableType.WEAPON_SHOP, "armory", "Rustaveli Armory", "Buy Weapons")
            }
        }

        // 3. Car Station
        run {
            val dx = GameCatalog.CAR_STATION_X - playerX
            val dz = GameCatalog.CAR_STATION_Z - playerZ
            if (dx * dx + dz * dz < 6.8f * 6.8f) {
                return NearbyInteractable(InteractableType.CAR_STATION, "car_station", "Tbilisi Motors", "Car Station")
            }
        }

        // 4. Buyable Houses
        for (h in GameCatalog.houses) {
            val dx = h.x - playerX
            val dz = h.z - playerZ
            if (dx * dx + dz * dz < 6.0f * 6.0f) {
                val owned = ownedHouses.contains(h.id)
                return NearbyInteractable(
                    InteractableType.BUYABLE_HOUSE,
                    h.id,
                    h.name,
                    if (owned) "Enter Safehouse" else "Buy $${h.price}"
                )
            }
        }

        // 5. Talkable NPCs
        for (npc in world.npcs) {
            if (npc.kind == NpcKind.STORY_NPC || npc.kind == NpcKind.PEDESTRIAN) {
                val dx = npc.x - playerX
                val dz = npc.z - playerZ
                if (dx * dx + dz * dz < 4.5f * 4.5f) {
                    return NearbyInteractable(InteractableType.TALK_NPC, npc.id.toString(), npc.name, "Talk")
                }
            }
        }

        return null
    }

    fun buildHudSnapshot(): HudSnapshot {
        synchronized(lock) {
            val car = drivingVehicle
            val nearby = findNearbyInteractableLocked()
            val canEnter = car == null && findNearestVehicleLocked(4.8f) != null

            val dots = ArrayList<MinimapDot>(32)
            // Shops & Houses
            dots.add(MinimapDot(GameCatalog.WEAPON_SHOP_X, GameCatalog.WEAPON_SHOP_Z, "WEAPON"))
            dots.add(MinimapDot(GameCatalog.CAR_STATION_X, GameCatalog.CAR_STATION_Z, "CAR"))
            for (h in GameCatalog.houses) {
                dots.add(MinimapDot(h.x, h.z, if (ownedHouses.contains(h.id)) "OWNED_HOUSE" else "HOUSE"))
            }
            val currentMission = activeMission
            val currentStage = currentMission?.stages?.getOrNull(activeStageIdx)
            if (currentStage != null) {
                dots.add(MinimapDot(currentStage.targetX, currentStage.targetZ, "MISSION"))
            } else {
                for (m in GameCatalog.missions) {
                    dots.add(MinimapDot(m.startX, m.startZ, if (completedMissions.contains(m.id)) "MISSION_DONE" else "MISSION"))
                }
            }
            for (npc in world.npcs) {
                when (npc.kind) {
                    NpcKind.SYNDICATE_ENEMY, NpcKind.SYNDICATE_BOSS -> dots.add(MinimapDot(npc.x, npc.z, "ENEMY"))
                    NpcKind.POLICE_OFFICER -> dots.add(MinimapDot(npc.x, npc.z, "POLICE"))
                    NpcKind.STORY_NPC -> dots.add(MinimapDot(npc.x, npc.z, "NPC"))
                    else -> {}
                }
            }

            val district = when {
                abs(playerX) < 28f && abs(playerZ) < 28f -> "Peace Bridge Riverfront"
                playerX < -30f && playerZ < -30f -> "Downtown Financial Core"
                playerX > 30f && playerZ < -30f -> "Mtatsminda TV Tower Heights"
                playerX > 30f && playerZ > 30f -> "Narikala Old Town"
                playerX < -30f && playerZ > 30f -> "Rustavi Shipping Docks"
                else -> "Revenge City Boulevard"
            }

            val stageText = when (currentStage?.objectiveType) {
                MissionObjectiveType.ELIMINATE_TARGETS -> {
                    val rem = world.npcs.count { it.kind == NpcKind.SYNDICATE_ENEMY || it.kind == NpcKind.SYNDICATE_BOSS }
                    "${currentStage.instruction} ($rem remaining)"
                }
                else -> currentStage?.instruction
            }

            val gpsX = customGpsX ?: currentStage?.targetX
            val gpsZ = customGpsZ ?: currentStage?.targetZ
            val gpsName = if (customGpsX != null) (customGpsLabel ?: "GPS Pin") else currentStage?.title
            val gpsDist = if (gpsX != null && gpsZ != null) {
                val gdx = gpsX - playerX
                val gdz = gpsZ - playerZ
                val d = sqrt(gdx * gdx + gdz * gdz).toInt()
                if (customGpsX != null && d < 6) {
                    customGpsX = null
                    customGpsZ = null
                    customGpsLabel = null
                }
                d
            } else null

            return HudSnapshot(
                cash = playerCash,
                health = playerHealth,
                armor = playerArmor,
                wantedStars = wantedStars,
                timeOfDayHours = timeOfDayHours,
                districtName = district,
                speedKmh = car?.let { (abs(it.speed) * 4.5f).toInt() } ?: 0,
                inVehicle = car != null,
                currentVehicleName = car?.model?.displayName,
                canEnterVehicle = canEnter,
                equippedWeapon = equippedWeapon,
                nearbyInteractable = nearby,
                activeMissionTitle = currentMission?.let { "M${it.number}: ${it.title}" },
                activeStageTitle = currentStage?.title,
                activeObjectiveText = stageText,
                missionTimerSec = if (currentStage?.timeLimitSec ?: 0f > 0f) missionTimerSec.coerceAtLeast(0f) else null,
                playerX = playerX,
                playerZ = playerZ,
                playerYaw = playerYaw,
                minimapDots = dots,
                gpsTargetX = gpsX,
                gpsTargetZ = gpsZ,
                gpsLabel = gpsName,
                gpsDistanceMeters = gpsDist,
                fps = currentFps
            )
        }
    }

    fun getMissionObjectivePos(): Pair<Float, Float>? {
        synchronized(lock) {
            val st = activeMission?.stages?.getOrNull(activeStageIdx) ?: return null
            return st.targetX to st.targetZ
        }
    }

    fun isPickupTargetActive(): Pair<Float, Float>? {
        synchronized(lock) {
            return if (pickupTargetActive) pickupX to pickupZ else null
        }
    }

    private fun lerpAngle(a: Float, b: Float, t: Float): Float {
        val diff = normalizeAngle(b - a)
        return a + diff * t.coerceIn(0f, 1f)
    }

    private fun normalizeAngle(angle: Float): Float {
        var res = angle
        while (res > PI) res -= (2.0 * PI).toFloat()
        while (res < -PI) res += (2.0 * PI).toFloat()
        return res
    }
}
