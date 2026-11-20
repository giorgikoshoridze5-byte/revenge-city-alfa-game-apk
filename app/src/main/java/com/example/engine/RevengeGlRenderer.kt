package com.example.engine

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.data.GameCatalog
import com.example.data.VehicleModel
import com.example.data.WeaponType
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class RevengeGlRenderer(
    private val sim: WorldSimulation
) : GLSurfaceView.Renderer {

    private lateinit var shader: GlShaderProgram
    private lateinit var meshes: MeshPrimitives

    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var lastTimeNs = System.nanoTime()
    private var fpsFrames = 0
    private var fpsAccum = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glFrontFace(GLES20.GL_CCW)
        shader = GlShaderProgram()
        meshes = MeshPrimitives()
        lastTimeNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.coerceAtLeast(1).toFloat()
        sim.isPortraitViewport = aspect < 1.0f
        // On vertical portrait screens, widen vertical FOV so horizontal road & sidewalk visibility stays comfortable
        val fovy = if (aspect < 1.0f) {
            (56f + (1.0f - aspect) * 42f).coerceIn(60f, 80f)
        } else {
            56f
        }
        Matrix.perspectiveM(projMatrix, 0, fovy, aspect, 0.5f, 340f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastTimeNs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        lastTimeNs = now

        fpsFrames++
        fpsAccum += dt
        if (fpsAccum >= 0.5f) {
            sim.currentFps = (fpsFrames / fpsAccum).toInt().coerceIn(1, 120)
            fpsFrames = 0
            fpsAccum = 0f
        }

        sim.step(dt)

        // Compute dynamic Day-Night lighting parameters
        val hour = sim.timeOfDayHours
        val sunAngle = ((hour - 6f) / 12f) * PI.toFloat()
        val sunHeight = sin(sunAngle)
        val nightFactor = ((-sunHeight + 0.18f) / 0.55f).coerceIn(0f, 1f)
        val sunsetFactor = (1f - abs(sunHeight) * 2.8f).coerceIn(0f, 1f)

        val skyR = lerp(lerp(0.32f, 0.90f, sunsetFactor), 0.04f, nightFactor)
        val skyG = lerp(lerp(0.58f, 0.44f, sunsetFactor), 0.06f, nightFactor)
        val skyB = lerp(lerp(0.90f, 0.36f, sunsetFactor), 0.14f, nightFactor)

        GLES20.glClearColor(skyR, skyG, skyB, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        GLES20.glUseProgram(shader.programId)

        val camX = sim.camPosX
        val camY = sim.camPosY
        val camZ = sim.camPosZ
        Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, sim.camLookX, sim.camLookY, sim.camLookZ, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        val sunDirX = cos(sunAngle) * 0.7f
        val sunDirY = if (sunHeight > 0.05f) sunHeight else 0.35f
        val sunDirZ = 0.55f

        val sunR = lerp(lerp(1.0f, 1.0f, sunsetFactor), 0.25f, nightFactor)
        val sunG = lerp(lerp(0.95f, 0.60f, sunsetFactor), 0.32f, nightFactor)
        val sunB = lerp(lerp(0.86f, 0.38f, sunsetFactor), 0.55f, nightFactor)

        val ambSkyR = lerp(0.42f, 0.10f, nightFactor)
        val ambSkyG = lerp(0.50f, 0.14f, nightFactor)
        val ambSkyB = lerp(0.65f, 0.24f, nightFactor)

        val drawDist = sim.graphicsQuality.drawDistance
        val drawDistSq = drawDist * drawDist

        GLES20.glUniform3f(shader.uSunDir, sunDirX, sunDirY, sunDirZ)
        GLES20.glUniform3f(shader.uSunColor, sunR, sunG, sunB)
        GLES20.glUniform3f(shader.uAmbientSky, ambSkyR, ambSkyG, ambSkyB)
        GLES20.glUniform3f(shader.uAmbientGround, 0.14f, 0.14f, 0.16f)
        GLES20.glUniform3f(shader.uCameraPos, camX, camY, camZ)
        GLES20.glUniform3f(shader.uFogColor, skyR, skyG, skyB)
        GLES20.glUniform1f(shader.uFogStart, drawDist * 0.45f)
        GLES20.glUniform1f(shader.uFogEnd, drawDist)
        GLES20.glUniform1f(shader.uNightFactor, nightFactor)

        synchronized(sim.lock) {
            // 1. Ground Terrain & River & Roads
            meshes.cube.bind(shader.aPosition, shader.aNormal)
            // City Earth Base
            drawBox(0f, -0.5f, 0f, 410f, 1.0f, 410f, 0f, 0.16f, 0.18f, 0.17f, 0f, 8f, 0.02f, 0f)
            // Mtkvari River Channel
            drawBox(0f, -0.35f, 0f, 20f, 0.85f, 400f, 0f, 0.06f, 0.24f, 0.36f, 0.12f * nightFactor, 48f, 0.55f, 0f)
            // Green Park Lawns & Riverfront Promenades
            for (park in sim.world.parks) {
                val dx = park.x - camX
                val dz = park.z - camZ
                if (dx * dx + dz * dz > drawDistSq * 0.9f) continue
                drawBox(park.x, 0.02f, park.z, park.width, 0.06f, park.depth, 0f, 0.14f, 0.42f, 0.19f, 0f, 6f, 0.02f, 0f)
            }
            // Roads, Highways & Bridges
            for (road in sim.world.roads) {
                val y = if (road.isBridge) 0.06f else 0.04f
                val shade = if (road.isHighway) 0.10f else 0.13f
                drawBox(road.x, y, road.z, road.width, 0.12f, road.depth, 0f, shade, shade + 0.01f, shade + 0.03f, 0f, 12f, 0.06f, 0f)
            }
            // Dashed Highway & Boulevard Lane Markings
            for (dash in sim.world.roadDashes) {
                val dx = dash.x - camX
                val dz = dash.z - camZ
                if (dx * dx + dz * dz > drawDistSq * 0.65f) continue
                drawBox(dash.x, 0.11f, dash.z, dash.width, 0.02f, dash.depth, 0f, 0.90f, 0.90f, 0.92f, 0.06f * nightFactor, 8f, 0.05f, 0f)
            }
            // Iconic Peace Bridge Arches over Mtkvari River (X=0, Z=0)
            drawPeaceBridge(nightFactor)

            // 2. Buildings (distance culled)
            for (b in sim.world.buildings) {
                val dx = b.x - camX
                val dz = b.z - camZ
                if (dx * dx + dz * dz > drawDistSq) continue
                val winMode = if (b.style == BuildingStyle.SKYSCRAPER_GLASS || b.style == BuildingStyle.URBAN_BLOCK || b.style == BuildingStyle.HOUSE_PROPERTY) 1f else 0f
                val spec = if (b.style == BuildingStyle.SKYSCRAPER_GLASS) 0.55f else 0.12f
                drawBox(b.x, b.height * 0.5f, b.z, b.width, b.height, b.depth, 0f, b.r, b.g, b.b, 0f, 28f, spec, winMode)
                if (b.hasRoofAccent) {
                    drawBox(b.x, b.height + 0.4f, b.z, b.width * 0.65f, 0.8f, b.depth * 0.65f, 0f, 0.20f, 0.22f, 0.28f, 0f, 16f, 0.1f, 0f)
                }
                if (b.style == BuildingStyle.WEAPON_SHOP) {
                    drawBox(b.x, b.height + 0.7f, b.z, 6.5f, 1.1f, 1.2f, 0f, 0.96f, 0.68f, 0.12f, 0.85f, 16f, 0.2f, 0f)
                } else if (b.style == BuildingStyle.CAR_STATION) {
                    drawBox(b.x, b.height + 0.7f, b.z, 7.5f, 1.1f, 1.2f, 0f, 0.22f, 0.74f, 0.98f, 0.85f, 16f, 0.2f, 0f)
                }
            }

            // 3. Mtatsminda TV Tower (142, -114)
            drawTvTower(nightFactor)

            // 4. Streetlamps & Physics Props
            if (sim.streetLightsEnabled) {
                for (lamp in sim.world.lamps) {
                    val dx = lamp.x - camX
                    val dz = lamp.z - camZ
                    if (dx * dx + dz * dz > drawDistSq * 0.7f) continue
                    drawBox(lamp.x, 2.8f, lamp.z, 0.22f, 5.6f, 0.22f, 0f, 0.25f, 0.28f, 0.32f, 0f, 16f, 0.2f, 0f)
                    drawBox(lamp.x, 5.65f, lamp.z, 0.7f, 0.24f, 0.7f, 0f, 1.0f, 0.86f, 0.52f, nightFactor * 0.95f, 12f, 0.1f, 0f)
                }
            } else {
                // Street lights saving mode: render unlit poles without glowing bulbs
                for (lamp in sim.world.lamps) {
                    val dx = lamp.x - camX
                    val dz = lamp.z - camZ
                    if (dx * dx + dz * dz > drawDistSq * 0.55f) continue
                    drawBox(lamp.x, 2.8f, lamp.z, 0.22f, 5.6f, 0.22f, 0f, 0.20f, 0.22f, 0.26f, 0f, 8f, 0.05f, 0f)
                }
            }

            for (p in sim.world.props) {
                val dx = p.x - camX
                val dz = p.z - camZ
                if (dx * dx + dz * dz > drawDistSq * 0.5f) continue
                if (p.isBarrel) {
                    meshes.cylinder.bind(shader.aPosition, shader.aNormal)
                    drawCylinder(p.x, p.y, p.z, p.size * 0.7f, p.size * 1.15f, p.size * 0.7f, p.yaw, p.r, p.g, p.b, 0f)
                    meshes.cube.bind(shader.aPosition, shader.aNormal)
                } else {
                    drawBox(p.x, p.y, p.z, p.size, p.size, p.size, p.yaw, p.r, p.g, p.b, 0f, 8f, 0.08f, 0f)
                }
            }

            // 5. Trees & Old Town Domes
            meshes.cylinder.bind(shader.aPosition, shader.aNormal)
            for (t in sim.world.trees) {
                val dx = t.x - camX
                val dz = t.z - camZ
                if (dx * dx + dz * dz > drawDistSq * 0.7f) continue
                drawCylinder(t.x, t.height * 0.25f, t.z, 0.55f, t.height * 0.5f, 0.55f, 0f, 0.36f, 0.24f, 0.15f, 0f)
            }
            meshes.dome.bind(shader.aPosition, shader.aNormal)
            for (t in sim.world.trees) {
                val dx = t.x - camX
                val dz = t.z - camZ
                if (dx * dx + dz * dz > drawDistSq * 0.7f) continue
                drawDome(t.x, t.height * 0.65f, t.z, t.radius * 1.9f, t.height * 0.65f, t.radius * 1.9f, 0.14f, 0.44f, 0.20f, 0f)
            }
            for (b in sim.world.buildings) {
                if (b.hasDome) {
                    drawDome(b.x, b.height + 2.2f, b.z, 6.5f, 5.2f, 6.5f, 0.85f, 0.68f, 0.24f, 0.12f)
                }
            }

            // 6. Interactive Beacons (Missions, Shops, Buyable Houses, Pickups)
            drawWorldBeacons(nightFactor)

            // 7. Vehicles
            for (v in sim.world.vehicles) {
                val dx = v.x - camX
                val dz = v.z - camZ
                if (dx * dx + dz * dz > drawDistSq) continue
                drawVehicle(v, nightFactor)
            }

            // 8. NPCs & George Nikoladze (when on foot)
            val activeSpeaker = sim.talkingSpeakerName
            val speakingNow = sim.isTalkingNow
            for (npc in sim.world.npcs) {
                val dx = npc.x - camX
                val dz = npc.z - camZ
                if (dx * dx + dz * dz > drawDistSq * 0.75f) continue
                val npcTalking = speakingNow && activeSpeaker != null && npc.name.equals(activeSpeaker, ignoreCase = true)
                drawHumanoid(
                    x = npc.x, z = npc.z, yaw = npc.yaw, walkPhase = npc.walkPhase,
                    jacketR = npc.shirtR, jacketG = npc.shirtG, jacketB = npc.shirtB,
                    hasWeapon = npc.kind == NpcKind.SYNDICATE_ENEMY || npc.kind == NpcKind.SYNDICATE_BOSS || npc.kind == NpcKind.POLICE_OFFICER,
                    isBoss = npc.kind == NpcKind.SYNDICATE_BOSS,
                    isFemale = npc.voiceProfile.isFemale,
                    isTalking = npcTalking,
                    seed = npc.id
                )
            }

            if (sim.drivingVehicle == null) {
                // George Nikoladze (brown leather jacket, olive-dark shirt, blue jeans)
                val georgeTalking = speakingNow && (activeSpeaker?.contains("George", ignoreCase = true) == true)
                drawHumanoid(
                    x = sim.playerX, z = sim.playerZ, yaw = sim.playerYaw, walkPhase = sim.playerWalkPhase,
                    jacketR = 0.42f, jacketG = 0.26f, jacketB = 0.15f,
                    hasWeapon = sim.equippedWeapon != WeaponType.FISTS,
                    isBoss = false,
                    isFemale = false,
                    isTalking = georgeTalking,
                    seed = 777
                )
                if (sim.muzzleFlashTimer > 0f) {
                    meshes.dome.bind(shader.aPosition, shader.aNormal)
                    val fx = sim.playerX + sin(sim.playerYaw) * 0.95f
                    val fz = sim.playerZ + cos(sim.playerYaw) * 0.95f
                    drawDome(fx, 1.25f, fz, 0.55f, 0.55f, 0.55f, 1.0f, 0.9f, 0.35f, 1.0f)
                }
            }

            // 9. Projectiles & Particles
            meshes.dome.bind(shader.aPosition, shader.aNormal)
            for (p in sim.projectiles) {
                drawDome(p.x, p.y, p.z, 0.26f, 0.26f, 0.26f, p.r, p.g, p.b, 1.0f)
            }
            for (pt in sim.particles) {
                val alphaScale = (pt.life / pt.maxLife).coerceIn(0.1f, 1f)
                drawDome(pt.x, pt.y, pt.z, pt.size * alphaScale, pt.size * alphaScale, pt.size * alphaScale, pt.r, pt.g, pt.b, 0.7f)
            }
        }
    }

    private fun drawPeaceBridge(nightFactor: Float) {
        val pulse = 0.65f + 0.35f * sin(sim.animClock * 2.5f)
        for (i in -3..3) {
            val bx = i * 3.8f
            val h = 4.6f - (abs(i) * 0.7f)
            drawBox(bx, h, -6.5f, 3.6f, 0.45f, 0.6f, 0f, 0.28f, 0.85f, 0.98f, 0.35f + 0.55f * nightFactor * pulse, 48f, 0.6f, 0f)
            drawBox(bx, h, 6.5f, 3.6f, 0.45f, 0.6f, 0f, 0.28f, 0.85f, 0.98f, 0.35f + 0.55f * nightFactor * pulse, 48f, 0.6f, 0f)
            drawBox(bx, h * 0.5f, -6.5f, 0.28f, h, 0.28f, 0f, 0.65f, 0.88f, 0.96f, 0.3f * nightFactor, 32f, 0.4f, 0f)
            drawBox(bx, h * 0.5f, 6.5f, 0.28f, h, 0.28f, 0f, 0.65f, 0.88f, 0.96f, 0.3f * nightFactor, 32f, 0.4f, 0f)
        }
    }

    private fun drawTvTower(nightFactor: Float) {
        val tx = 142f
        val tz = -114f
        val glow = 0.5f + 0.5f * sin(sim.animClock * 3.2f)
        meshes.cube.bind(shader.aPosition, shader.aNormal)
        drawBox(tx, 18f, tz, 3.2f, 36f, 3.2f, 0f, 0.72f, 0.74f, 0.80f, 0.15f * nightFactor, 24f, 0.3f, 0f)
        drawBox(tx, 34f, tz, 7.0f, 2.2f, 7.0f, 0f, 0.86f, 0.26f, 0.92f, 0.45f + 0.55f * nightFactor * glow, 32f, 0.5f, 0f)
        drawBox(tx, 48f, tz, 1.3f, 28f, 1.3f, 0f, 0.92f, 0.35f, 0.75f, 0.35f + 0.55f * nightFactor, 32f, 0.4f, 0f)
        meshes.dome.bind(shader.aPosition, shader.aNormal)
        drawDome(tx, 63f, tz, 1.9f, 1.9f, 1.9f, 1.0f, 0.22f, 0.25f, 0.6f + 0.4f * glow)
    }

    private fun drawWorldBeacons(nightFactor: Float) {
        val bob = sin(sim.animClock * 3.5f) * 0.35f
        meshes.cylinder.bind(shader.aPosition, shader.aNormal)

        // Weapon Shop Amber Beacon
        drawCylinder(GameCatalog.WEAPON_SHOP_X, 0.12f, GameCatalog.WEAPON_SHOP_Z, 3.4f, 0.14f, 3.4f, 0f, 0.96f, 0.70f, 0.14f, 0.85f)
        // Car Station Cyan Beacon
        drawCylinder(GameCatalog.CAR_STATION_X, 0.12f, GameCatalog.CAR_STATION_Z, 3.8f, 0.14f, 3.8f, 0f, 0.20f, 0.75f, 0.98f, 0.85f)
        // Buyable Houses
        for (h in GameCatalog.houses) {
            val owned = sim.ownedHouses.contains(h.id)
            if (owned) {
                drawCylinder(h.x, 0.12f, h.z, 3.2f, 0.14f, 3.2f, 0f, 0.26f, 0.78f, 0.42f, 0.8f)
            } else {
                drawCylinder(h.x, 0.12f, h.z, 3.2f, 0.14f, 3.2f, 0f, 0.96f, 0.72f, 0.18f, 0.8f)
            }
        }

        // Mission Start or Objective Beacon
        val activeMissionPos = sim.getMissionObjectivePos()
        if (activeMissionPos != null) {
            drawCylinder(activeMissionPos.first, 4.5f, activeMissionPos.second, 1.2f, 9f, 1.2f, 0f, 0.98f, 0.78f, 0.18f, 0.9f)
        } else {
            for (m in GameCatalog.missions) {
                val done = sim.completedMissions.contains(m.id)
                val r = if (done) 0.35f else 0.98f
                val g = if (done) 0.80f else 0.74f
                val b = if (done) 0.48f else 0.14f
                drawCylinder(m.startX, 0.14f, m.startZ, 3.0f, 0.16f, 3.0f, 0f, r, g, b, 0.85f)
                drawCylinder(m.startX, 2.2f + bob, m.startZ, 0.9f, 1.4f, 0.9f, sim.animClock * 1.5f, r, g, b, 0.95f)
            }
        }

        // Custom GPS Waypoint Sky Beam
        val gps = sim.getGpsTarget()
        if (gps != null) {
            drawCylinder(gps.first, 7.5f, gps.second, 0.95f, 15f, 0.95f, 0f, 0.18f, 0.92f, 1.0f, 0.90f)
            drawCylinder(gps.first, 0.16f, gps.second, 3.2f, 0.16f, 3.2f, 0f, 0.18f, 0.92f, 1.0f, 0.95f)
        }

        // Pickup Orbs & Mission Briefcase
        meshes.dome.bind(shader.aPosition, shader.aNormal)
        for (orb in sim.pickups) {
            drawDome(orb.x, 0.85f + bob * 0.5f, orb.z, 0.85f, 0.85f, 0.85f, 0.25f, 0.88f, 0.42f, 0.95f)
        }
        val pickupPos = sim.isPickupTargetActive()
        if (pickupPos != null) {
            meshes.cube.bind(shader.aPosition, shader.aNormal)
            drawBox(pickupPos.first, 1.1f + bob * 0.5f, pickupPos.second, 1.1f, 0.85f, 0.65f, sim.animClock * 2f, 0.98f, 0.82f, 0.22f, 0.95f, 24f, 0.4f, 0f)
        }
    }

    private fun drawVehicle(v: VehicleEntity, nightFactor: Float) {
        val m = v.model
        meshes.cube.bind(shader.aPosition, shader.aNormal)

        // Ground shadow pad
        drawBox(v.x, 0.09f, v.z, m.width * 1.08f, 0.04f, m.length * 1.05f, v.yaw, 0.04f, 0.04f, 0.06f, 0f, 4f, 0f, 0f)

        // Lower chassis with suspension pitch/roll
        drawCarPart(v, 0f, 0.48f, 0f, m.width, m.height * 0.52f, m.length, v.r, v.g, v.b, 0f, 72f, 0.90f)
        // Upper cabin
        drawCarPart(v, 0f, 0.48f + m.height * 0.42f, -m.length * 0.06f, m.width * 0.84f, m.height * 0.44f, m.length * 0.52f, 0.14f, 0.18f, 0.25f, 0f, 96f, 0.95f)
        // Front & Rear Bumpers + Grille + Side Mirrors
        drawCarPart(v, 0f, 0.30f, m.length * 0.505f, m.width * 0.96f, 0.18f, 0.14f, 0.12f, 0.13f, 0.16f, 0f, 48f, 0.4f)
        drawCarPart(v, 0f, 0.30f, -m.length * 0.505f, m.width * 0.96f, 0.18f, 0.14f, 0.12f, 0.13f, 0.16f, 0f, 48f, 0.4f)
        drawCarPart(v, 0f, 0.46f, m.length * 0.505f, m.width * 0.48f, 0.20f, 0.08f, 0.08f, 0.09f, 0.11f, 0f, 64f, 0.6f)
        drawCarPart(v, -m.width * 0.48f, 0.72f, m.length * 0.12f, 0.18f, 0.10f, 0.14f, v.r, v.g, v.b, 0f, 48f, 0.6f)
        drawCarPart(v, m.width * 0.48f, 0.72f, m.length * 0.12f, 0.18f, 0.10f, 0.14f, v.r, v.g, v.b, 0f, 48f, 0.6f)

        // Glowing TAXI Roof Sign
        if (v.model == VehicleModel.TAXI) {
            drawCarPart(v, 0f, 0.48f + m.height * 0.68f, -m.length * 0.05f, 0.68f, 0.19f, 0.28f, 1.0f, 0.86f, 0.20f, 0.95f, 24f, 0.3f)
        }

        // Police Lightbar
        if (v.isPolice) {
            val flashRed = sin(v.sirenTimer) > 0f
            drawCarPart(v, -0.35f, 0.48f + m.height * 0.7f, -m.length * 0.05f, 0.55f, 0.20f, 0.35f, 0.95f, 0.12f, 0.12f, if (flashRed) 1f else 0.15f, 16f, 0.3f)
            drawCarPart(v, 0.35f, 0.48f + m.height * 0.7f, -m.length * 0.05f, 0.55f, 0.20f, 0.35f, 0.15f, 0.45f, 1.0f, if (!flashRed) 1f else 0.15f, 16f, 0.3f)
        }

        // Headlights & Taillights
        val headGlow = if (nightFactor > 0.25f || v.id == sim.drivingVehicle?.id) 0.95f else 0.15f
        drawCarPart(v, -m.width * 0.34f, 0.48f, m.length * 0.5f, 0.34f, 0.18f, 0.08f, 1.0f, 0.96f, 0.80f, headGlow, 16f, 0.2f)
        drawCarPart(v, m.width * 0.34f, 0.48f, m.length * 0.5f, 0.34f, 0.18f, 0.08f, 1.0f, 0.96f, 0.80f, headGlow, 16f, 0.2f)
        drawCarPart(v, -m.width * 0.34f, 0.48f, -m.length * 0.5f, 0.36f, 0.18f, 0.08f, 0.95f, 0.14f, 0.14f, 0.85f, 16f, 0.2f)
        drawCarPart(v, m.width * 0.34f, 0.48f, -m.length * 0.5f, 0.36f, 0.18f, 0.08f, 0.95f, 0.14f, 0.14f, 0.85f, 16f, 0.2f)

        // 4 Rotating Wheels
        meshes.cylinder.bind(shader.aPosition, shader.aNormal)
        val wx = m.width * 0.48f
        val wz = m.length * 0.32f
        drawWheel(v, -wx, 0.34f, wz, true)
        drawWheel(v, wx, 0.34f, wz, true)
        drawWheel(v, -wx, 0.34f, -wz, false)
        drawWheel(v, wx, 0.34f, -wz, false)
    }

    private fun drawCarPart(
        v: VehicleEntity,
        lx: Float, ly: Float, lz: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float,
        emissive: Float, shininess: Float, spec: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, v.x, 0f, v.z)
        Matrix.rotateM(modelMatrix, 0, Math.toDegrees(v.yaw.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.rotateM(modelMatrix, 0, Math.toDegrees(v.pitch.toDouble()).toFloat(), 1f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, Math.toDegrees(v.roll.toDouble()).toFloat(), 0f, 0f, 1f)
        Matrix.translateM(modelMatrix, 0, lx, ly, lz)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES20.glUniformMatrix4fv(shader.uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrix, 1, false, modelMatrix, 0)
        GLES20.glUniform3f(shader.uColor, r, g, b)
        GLES20.glUniform1f(shader.uEmissive, emissive)
        GLES20.glUniform1f(shader.uShininess, shininess)
        GLES20.glUniform1f(shader.uSpecStrength, spec)
        GLES20.glUniform1f(shader.uWindowMode, 0f)
        meshes.cube.draw()
    }

    private fun drawWheel(v: VehicleEntity, lx: Float, ly: Float, lz: Float, front: Boolean) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, v.x, 0f, v.z)
        Matrix.rotateM(modelMatrix, 0, Math.toDegrees(v.yaw.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.translateM(modelMatrix, 0, lx, ly, lz)
        if (front) {
            Matrix.rotateM(modelMatrix, 0, Math.toDegrees(v.steerAngle.toDouble()).toFloat(), 0f, 1f, 0f)
        }
        Matrix.rotateM(modelMatrix, 0, 90f, 0f, 0f, 1f)
        Matrix.rotateM(modelMatrix, 0, Math.toDegrees(v.wheelSpin.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, 0.68f, 0.28f, 0.68f)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES20.glUniformMatrix4fv(shader.uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrix, 1, false, modelMatrix, 0)
        GLES20.glUniform3f(shader.uColor, 0.08f, 0.09f, 0.11f)
        GLES20.glUniform1f(shader.uEmissive, 0f)
        GLES20.glUniform1f(shader.uShininess, 18f)
        GLES20.glUniform1f(shader.uSpecStrength, 0.25f)
        GLES20.glUniform1f(shader.uWindowMode, 0f)
        meshes.cylinder.draw()
    }

    private fun drawHumanoid(
        x: Float,
        z: Float,
        yaw: Float,
        walkPhase: Float,
        jacketR: Float,
        jacketG: Float,
        jacketB: Float,
        hasWeapon: Boolean,
        isBoss: Boolean,
        isFemale: Boolean = false,
        isTalking: Boolean = false,
        seed: Int = 0
    ) {
        val scale = if (isBoss) 1.18f else if (isFemale) 0.95f else 1.0f
        val swing = sin(walkPhase) * 24f

        val hairColor = when (seed % 4) {
            1 -> Triple(0.28f, 0.16f, 0.10f)
            2 -> Triple(0.45f, 0.30f, 0.15f)
            3 -> Triple(0.16f, 0.12f, 0.11f)
            else -> Triple(0.09f, 0.08f, 0.08f)
        }
        val skinR = if (isFemale) 0.88f else 0.84f
        val skinG = if (isFemale) 0.72f else 0.67f
        val skinB = if (isFemale) 0.62f else 0.55f

        val armX = if (isFemale) 0.31f * scale else 0.36f * scale

        // Ground shadow disc
        meshes.cylinder.bind(shader.aPosition, shader.aNormal)
        drawHumanLimb(x, z, yaw, 0f, 0.09f, 0f, 0f, 0.76f * scale, 0.025f, 0.76f * scale, 0.04f, 0.04f, 0.06f, meshes.cylinder)

        // Rounded Legs (Thighs + Shins)
        drawHumanLimb(x, z, yaw, -0.135f * scale, 0.58f * scale, 0f, swing * 0.95f, 0.19f * scale, 0.38f * scale, 0.20f * scale, 0.14f, 0.18f, 0.28f, meshes.cylinder)
        drawHumanLimb(x, z, yaw, 0.135f * scale, 0.58f * scale, 0f, -swing * 0.95f, 0.19f * scale, 0.38f * scale, 0.20f * scale, 0.14f, 0.18f, 0.28f, meshes.cylinder)
        drawHumanLimb(x, z, yaw, -0.135f * scale, 0.28f * scale, 0f, swing * 0.7f, 0.17f * scale, 0.34f * scale, 0.18f * scale, 0.12f, 0.15f, 0.24f, meshes.cylinder)
        drawHumanLimb(x, z, yaw, 0.135f * scale, 0.28f * scale, 0f, -swing * 0.7f, 0.17f * scale, 0.34f * scale, 0.18f * scale, 0.12f, 0.15f, 0.24f, meshes.cylinder)

        // Rounded Torso & Neck
        val torsoW = if (isFemale) 0.48f * scale else 0.56f * scale
        drawHumanLimb(x, z, yaw, 0f, 1.13f * scale, 0f, 0f, torsoW, 0.62f * scale, 0.31f * scale, jacketR, jacketG, jacketB, meshes.cylinder)
        drawHumanLimb(x, z, yaw, 0f, 1.48f * scale, 0f, 0f, 0.14f * scale, 0.14f * scale, 0.14f * scale, skinR * 0.92f, skinG * 0.92f, skinB * 0.92f, meshes.cylinder)

        // Cylindrical Upper & Lower Arms
        drawHumanLimb(x, z, yaw, -armX, 1.24f * scale, 0f, -swing * 0.7f, 0.15f * scale, 0.28f * scale, 0.15f * scale, jacketR * 0.9f, jacketG * 0.9f, jacketB * 0.9f, meshes.cylinder)
        drawHumanLimb(x, z, yaw, -armX, 0.98f * scale, 0f, -swing * 0.7f, 0.13f * scale, 0.26f * scale, 0.13f * scale, jacketR * 0.82f, jacketG * 0.82f, jacketB * 0.82f, meshes.cylinder)
        drawHumanLimb(x, z, yaw, armX, 1.24f * scale, if (hasWeapon) 0.10f else 0f, if (hasWeapon) -72f else swing * 0.7f, 0.15f * scale, 0.28f * scale, 0.15f * scale, jacketR * 0.9f, jacketG * 0.9f, jacketB * 0.9f, meshes.cylinder)
        drawHumanLimb(x, z, yaw, armX, if (hasWeapon) 1.21f * scale else 0.98f * scale, if (hasWeapon) 0.22f * scale else 0f, if (hasWeapon) -72f else swing * 0.7f, 0.13f * scale, 0.26f * scale, 0.13f * scale, jacketR * 0.82f, jacketG * 0.82f, jacketB * 0.82f, meshes.cylinder)

        // Spherical Joints, Hands, Head, Ears, Eyes (Sclera + Iris + Pupil Dots!)
        meshes.dome.bind(shader.aPosition, shader.aNormal)
        // Knee caps & Shoulders
        drawHumanLimb(x, z, yaw, -0.135f * scale, 0.42f * scale, 0.02f * scale, 0f, 0.18f * scale, 0.15f * scale, 0.18f * scale, 0.13f, 0.17f, 0.26f, meshes.dome)
        drawHumanLimb(x, z, yaw, 0.135f * scale, 0.42f * scale, 0.02f * scale, 0f, 0.18f * scale, 0.15f * scale, 0.18f * scale, 0.13f, 0.17f, 0.26f, meshes.dome)
        drawHumanLimb(x, z, yaw, -armX, 1.37f * scale, 0f, 0f, 0.17f * scale, 0.16f * scale, 0.17f * scale, jacketR, jacketG, jacketB, meshes.dome)
        drawHumanLimb(x, z, yaw, armX, 1.37f * scale, 0f, 0f, 0.17f * scale, 0.16f * scale, 0.17f * scale, jacketR, jacketG, jacketB, meshes.dome)
        // Hands
        drawHumanLimb(x, z, yaw, -armX, 0.82f * scale, 0f, 0f, 0.13f * scale, 0.13f * scale, 0.14f * scale, skinR, skinG, skinB, meshes.dome)
        drawHumanLimb(x, z, yaw, armX, if (hasWeapon) 1.21f * scale else 0.82f * scale, if (hasWeapon) 0.34f * scale else 0f, 0f, 0.13f * scale, 0.13f * scale, 0.14f * scale, skinR, skinG, skinB, meshes.dome)

        // Smooth Skull & Cheekbones & Ears
        drawHumanLimb(x, z, yaw, 0f, 1.67f * scale, 0f, 0f, 0.35f * scale, 0.39f * scale, 0.35f * scale, skinR, skinG, skinB, meshes.dome)
        drawHumanLimb(x, z, yaw, -0.17f * scale, 1.66f * scale, -0.01f * scale, 0f, 0.06f * scale, 0.09f * scale, 0.06f * scale, skinR * 0.90f, skinG * 0.88f, skinB * 0.86f, meshes.dome)
        drawHumanLimb(x, z, yaw, 0.17f * scale, 1.66f * scale, -0.01f * scale, 0f, 0.06f * scale, 0.09f * scale, 0.06f * scale, skinR * 0.90f, skinG * 0.88f, skinB * 0.86f, meshes.dome)

        // Detailed Eyes: White Sclera + Colored Iris + Black Pupil Dot positioned cleanly on front skull surface
        drawHumanLimb(x, z, yaw, -0.068f * scale, 1.70f * scale, 0.162f * scale, 0f, 0.064f * scale, 0.044f * scale, 0.036f * scale, 0.96f, 0.97f, 0.98f, meshes.dome)
        drawHumanLimb(x, z, yaw, 0.068f * scale, 1.70f * scale, 0.162f * scale, 0f, 0.064f * scale, 0.044f * scale, 0.036f * scale, 0.96f, 0.97f, 0.98f, meshes.dome)
        val irisR = if (seed == 777) 0.18f else 0.25f
        val irisG = if (seed == 777) 0.44f else if (isFemale) 0.48f else 0.20f
        val irisB = if (seed == 777) 0.72f else 0.18f
        drawHumanLimb(x, z, yaw, -0.068f * scale, 1.70f * scale, 0.173f * scale, 0f, 0.036f * scale, 0.034f * scale, 0.024f * scale, irisR, irisG, irisB, meshes.dome)
        drawHumanLimb(x, z, yaw, 0.068f * scale, 1.70f * scale, 0.173f * scale, 0f, 0.036f * scale, 0.034f * scale, 0.024f * scale, irisR, irisG, irisB, meshes.dome)
        drawHumanLimb(x, z, yaw, -0.068f * scale, 1.70f * scale, 0.179f * scale, 0f, 0.018f * scale, 0.018f * scale, 0.018f * scale, 0.04f, 0.04f, 0.05f, meshes.dome)
        drawHumanLimb(x, z, yaw, 0.068f * scale, 1.70f * scale, 0.179f * scale, 0f, 0.018f * scale, 0.018f * scale, 0.018f * scale, 0.04f, 0.04f, 0.05f, meshes.dome)

        // Nose Tip
        drawHumanLimb(x, z, yaw, 0f, 1.64f * scale, 0.184f * scale, 0f, 0.046f * scale, 0.055f * scale, 0.052f * scale, skinR * 0.90f, skinG * 0.87f, skinB * 0.84f, meshes.dome)

        // Detailed Box Accents (Boots, Belt, Silver Buckle Dot, Coat Buttons, Watch, Brows, Animated Mouth, Hair, Weapon)
        meshes.cube.bind(shader.aPosition, shader.aNormal)
        // Boots & Soles
        drawHumanLimb(x, z, yaw, -0.135f * scale, 0.10f * scale, 0.03f * scale, swing * 0.4f, 0.21f * scale, 0.13f * scale, 0.28f * scale, 0.09f, 0.08f, 0.08f, meshes.cube)
        drawHumanLimb(x, z, yaw, 0.135f * scale, 0.10f * scale, 0.03f * scale, -swing * 0.4f, 0.21f * scale, 0.13f * scale, 0.28f * scale, 0.09f, 0.08f, 0.08f, meshes.cube)
        drawHumanLimb(x, z, yaw, -0.135f * scale, 0.04f * scale, 0.03f * scale, swing * 0.4f, 0.22f * scale, 0.04f * scale, 0.29f * scale, 0.04f, 0.04f, 0.05f, meshes.cube)
        drawHumanLimb(x, z, yaw, 0.135f * scale, 0.04f * scale, 0.03f * scale, -swing * 0.4f, 0.22f * scale, 0.04f * scale, 0.29f * scale, 0.04f, 0.04f, 0.05f, meshes.cube)

        // Waist Belt + Metallic Buckle Dot
        drawHumanLimb(x, z, yaw, 0f, 0.80f * scale, 0f, 0f, torsoW * 1.02f, 0.075f * scale, 0.31f * scale, 0.08f, 0.07f, 0.07f, meshes.cube)
        drawHumanLimb(x, z, yaw, 0f, 0.80f * scale, 0.155f * scale, 0f, 0.09f * scale, 0.065f * scale, 0.03f * scale, 0.85f, 0.78f, 0.42f, meshes.cube)

        // Inner Collar Shirt & Front Jacket Button Dots
        drawHumanLimb(x, z, yaw, 0f, 1.25f * scale, 0.05f * scale, 0f, 0.18f * scale, 0.36f * scale, 0.24f * scale, 0.18f, 0.24f, 0.20f, meshes.cube)
        for (btnIdx in 0..2) {
            drawHumanLimb(x, z, yaw, 0f, (1.02f + btnIdx * 0.11f) * scale, 0.165f * scale, 0f, 0.032f * scale, 0.032f * scale, 0.022f * scale, 0.82f, 0.76f, 0.62f, meshes.cube)
        }
        // Wrist Watch Accent
        drawHumanLimb(x, z, yaw, -armX, 0.89f * scale, 0f, 0f, 0.142f * scale, 0.04f * scale, 0.142f * scale, 0.78f, 0.80f, 0.84f, meshes.cube)

        // Eyebrows
        drawHumanLimb(x, z, yaw, -0.068f * scale, 1.745f * scale, 0.168f * scale, 0f, 0.072f * scale, 0.018f * scale, 0.025f * scale, hairColor.first, hairColor.second, hairColor.third, meshes.cube)
        drawHumanLimb(x, z, yaw, 0.068f * scale, 1.745f * scale, 0.168f * scale, 0f, 0.072f * scale, 0.018f * scale, 0.025f * scale, hairColor.first, hairColor.second, hairColor.third, meshes.cube)

        // Animated 3D Mouth & Teeth when talking
        val talkPulse = if (isTalking) (0.5f + 0.5f * sin(sim.animClock * 18f)) else 0f
        val mouthH = (0.016f + talkPulse * 0.048f) * scale
        val mouthW = (0.095f + talkPulse * 0.025f) * scale
        val mouthR = if (isFemale) 0.76f else if (isTalking) 0.22f else 0.52f
        val mouthG = if (isFemale) 0.24f else if (isTalking) 0.06f else 0.28f
        val mouthB = if (isFemale) 0.32f else if (isTalking) 0.07f else 0.26f
        drawHumanLimb(x, z, yaw, 0f, 1.565f * scale, 0.173f * scale, 0f, mouthW, mouthH, 0.028f * scale, mouthR, mouthG, mouthB, meshes.cube)
        if (isTalking && talkPulse > 0.35f) {
            drawHumanLimb(x, z, yaw, 0f, 1.578f * scale, 0.182f * scale, 0f, mouthW * 0.65f, 0.014f * scale, 0.018f * scale, 0.96f, 0.96f, 0.96f, meshes.cube)
        }

        // Hair & Beard Styling
        drawHumanLimb(x, z, yaw, 0f, 1.855f * scale, -0.01f * scale, 0f, 0.34f * scale, 0.10f * scale, 0.34f * scale, hairColor.first, hairColor.second, hairColor.third, meshes.cube)
        if (isFemale) {
            drawHumanLimb(x, z, yaw, 0f, 1.63f * scale, -0.15f * scale, 0f, 0.36f * scale, 0.44f * scale, 0.12f * scale, hairColor.first, hairColor.second, hairColor.third, meshes.cube)
            drawHumanLimb(x, z, yaw, -0.17f * scale, 1.63f * scale, 0f, 0f, 0.055f * scale, 0.34f * scale, 0.22f * scale, hairColor.first, hairColor.second, hairColor.third, meshes.cube)
            drawHumanLimb(x, z, yaw, 0.17f * scale, 1.63f * scale, 0f, 0f, 0.055f * scale, 0.34f * scale, 0.22f * scale, hairColor.first, hairColor.second, hairColor.third, meshes.cube)
        } else if (seed == 777 || isBoss || seed % 2 == 0) {
            drawHumanLimb(x, z, yaw, 0f, 1.515f * scale, 0.145f * scale, 0f, 0.14f * scale, 0.058f * scale, 0.05f * scale, hairColor.first, hairColor.second, hairColor.third, meshes.cube)
        }

        if (hasWeapon) {
            drawHumanLimb(x, z, yaw, armX, 1.24f * scale, 0.46f * scale, 0f, 0.09f, 0.12f, 0.46f, 0.14f, 0.15f, 0.18f, meshes.cube)
        }
    }

    private fun drawHumanLimb(
        px: Float, pz: Float, yaw: Float,
        lx: Float, ly: Float, lz: Float, pitchDeg: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float,
        mesh: VboMesh = meshes.cube
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, px, 0f, pz)
        Matrix.rotateM(modelMatrix, 0, Math.toDegrees(yaw.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.translateM(modelMatrix, 0, lx, ly, lz)
        if (pitchDeg != 0f) Matrix.rotateM(modelMatrix, 0, pitchDeg, 1f, 0f, 0f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES20.glUniformMatrix4fv(shader.uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrix, 1, false, modelMatrix, 0)
        GLES20.glUniform3f(shader.uColor, r, g, b)
        GLES20.glUniform1f(shader.uEmissive, 0f)
        GLES20.glUniform1f(shader.uShininess, 18f)
        GLES20.glUniform1f(shader.uSpecStrength, 0.18f)
        GLES20.glUniform1f(shader.uWindowMode, 0f)
        mesh.draw()
    }

    private fun drawBox(
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        yawRad: Float,
        r: Float, g: Float, b: Float,
        emissive: Float, shininess: Float, spec: Float, windowMode: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        if (yawRad != 0f) Matrix.rotateM(modelMatrix, 0, Math.toDegrees(yawRad.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES20.glUniformMatrix4fv(shader.uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrix, 1, false, modelMatrix, 0)
        GLES20.glUniform3f(shader.uColor, r, g, b)
        GLES20.glUniform1f(shader.uEmissive, emissive)
        GLES20.glUniform1f(shader.uShininess, shininess)
        GLES20.glUniform1f(shader.uSpecStrength, spec)
        GLES20.glUniform1f(shader.uWindowMode, windowMode)
        meshes.cube.draw()
    }

    private fun drawCylinder(
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        yawRad: Float,
        r: Float, g: Float, b: Float,
        emissive: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        if (yawRad != 0f) Matrix.rotateM(modelMatrix, 0, Math.toDegrees(yawRad.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES20.glUniformMatrix4fv(shader.uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrix, 1, false, modelMatrix, 0)
        GLES20.glUniform3f(shader.uColor, r, g, b)
        GLES20.glUniform1f(shader.uEmissive, emissive)
        GLES20.glUniform1f(shader.uShininess, 20f)
        GLES20.glUniform1f(shader.uSpecStrength, 0.25f)
        GLES20.glUniform1f(shader.uWindowMode, 0f)
        meshes.cylinder.draw()
    }

    private fun drawDome(
        x: Float, y: Float, z: Float,
        sx: Float, sy: Float, sz: Float,
        r: Float, g: Float, b: Float,
        emissive: Float
    ) {
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, x, y, z)
        Matrix.scaleM(modelMatrix, 0, sx, sy, sz)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        GLES20.glUniformMatrix4fv(shader.uMvpMatrix, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(shader.uModelMatrix, 1, false, modelMatrix, 0)
        GLES20.glUniform3f(shader.uColor, r, g, b)
        GLES20.glUniform1f(shader.uEmissive, emissive)
        GLES20.glUniform1f(shader.uShininess, 24f)
        GLES20.glUniform1f(shader.uSpecStrength, 0.3f)
        GLES20.glUniform1f(shader.uWindowMode, 0f)
        meshes.dome.draw()
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
}
