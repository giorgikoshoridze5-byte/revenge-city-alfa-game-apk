package com.example.engine

import com.example.data.GameCatalog
import com.example.data.VehicleModel
import com.example.data.VoiceProfile
import kotlin.math.abs
import kotlin.random.Random

class CityWorldData(
    val buildings: List<BuildingBox>,
    val roads: List<RoadStrip>,
    val roadDashes: List<RoadDash>,
    val parks: List<GreenParkPad>,
    val lamps: List<StreetLamp>,
    val trees: List<TreeProp>,
    val props: MutableList<PhysicsProp>,
    val vehicles: MutableList<VehicleEntity>,
    val npcs: MutableList<NpcEntity>
)

object CityWorldBuilder {

    const val WORLD_HALF_EXTENT = 195f

    fun build(): CityWorldData {
        val buildings = ArrayList<BuildingBox>()
        val roads = ArrayList<RoadStrip>()
        val roadDashes = ArrayList<RoadDash>()
        val parks = ArrayList<GreenParkPad>()
        val lamps = ArrayList<StreetLamp>()
        val trees = ArrayList<TreeProp>()
        val props = ArrayList<PhysicsProp>()
        val vehicles = ArrayList<VehicleEntity>()
        val npcs = ArrayList<NpcEntity>()

        // 1. Highways & Avenues Network
        // North-South Avenues + Outer Ring Highway
        for (ax in listOf(-164f, -120f, -60f, 60f, 120f, 164f)) {
            val isHwy = abs(ax) > 150f
            roads.add(RoadStrip(ax, 0f, if (isHwy) 18f else 14f, 368f, isHighway = isHwy))
            for (dz in -164..164 step 14) {
                roadDashes.add(RoadDash(ax, dz.toFloat(), 0.38f, 6.0f))
            }
        }
        // West-East Highways & Bridges over Mtkvari River (at Z = -164, -120, -60, 0, 60, 120, 164)
        for (az in listOf(-164f, -120f, -60f, 0f, 60f, 120f, 164f)) {
            val isHwy = abs(az) > 150f || az == 0f
            roads.add(RoadStrip(0f, az, 368f, if (isHwy) 18f else 14f, isBridge = true, isHighway = isHwy))
            for (dx in -164..164 step 14) {
                if (abs(dx) < 8) continue
                roadDashes.add(RoadDash(dx.toFloat(), az, 6.0f, 0.38f))
            }
        }
        // Riverfront Express Boulevards
        roads.add(RoadStrip(-26f, 0f, 13f, 360f))
        roads.add(RoadStrip(26f, 0f, 13f, 360f))

        // 2. Green Park Lawns & Riverfront Promenades
        parks.add(GreenParkPad(-16f, -45f, 9f, 52f))
        parks.add(GreenParkPad(-16f, 45f, 9f, 52f))
        parks.add(GreenParkPad(16f, -45f, 9f, 52f))
        parks.add(GreenParkPad(16f, 45f, 9f, 52f))
        parks.add(GreenParkPad(92f, -32f, 26f, 22f))
        parks.add(GreenParkPad(-92f, 32f, 26f, 22f))
        parks.add(GreenParkPad(44f, 92f, 18f, 22f))
        parks.add(GreenParkPad(138f, -46f, 22f, 24f))

        // 3. Dense Streetlights lining Highways, Avenues, & Bridges
        for (z in -160..160 step 22) {
            lamps.add(StreetLamp(-34f, z.toFloat()))
            lamps.add(StreetLamp(34f, z.toFloat()))
            lamps.add(StreetLamp(-68f, z.toFloat()))
            lamps.add(StreetLamp(68f, z.toFloat()))
            lamps.add(StreetLamp(-128f, z.toFloat()))
            lamps.add(StreetLamp(128f, z.toFloat()))
            lamps.add(StreetLamp(-174f, z.toFloat()))
            lamps.add(StreetLamp(174f, z.toFloat()))
        }
        for (x in -152..152 step 24) {
            if (abs(x) < 12) continue
            lamps.add(StreetLamp(x.toFloat(), -174f))
            lamps.add(StreetLamp(x.toFloat(), 174f))
            lamps.add(StreetLamp(x.toFloat(), -10f))
            lamps.add(StreetLamp(x.toFloat(), 10f))
        }

        val rng = Random(2026)

        // 4. Tall Buildings & Skyscrapers Everywhere across all 4 Quadrants!
        fun addSkyscraper(x: Float, z: Float, w: Float, d: Float, h: Float, style: BuildingStyle = BuildingStyle.SKYSCRAPER_GLASS) {
            val r = when (style) {
                BuildingStyle.OLD_TOWN_STONE -> 0.54f + rng.nextFloat() * 0.12f
                BuildingStyle.URBAN_BLOCK -> 0.28f + rng.nextFloat() * 0.10f
                else -> 0.17f + rng.nextFloat() * 0.14f
            }
            val g = when (style) {
                BuildingStyle.OLD_TOWN_STONE -> 0.40f + rng.nextFloat() * 0.10f
                BuildingStyle.URBAN_BLOCK -> 0.30f + rng.nextFloat() * 0.10f
                else -> 0.23f + rng.nextFloat() * 0.14f
            }
            val b = when (style) {
                BuildingStyle.OLD_TOWN_STONE -> 0.28f + rng.nextFloat() * 0.08f
                BuildingStyle.URBAN_BLOCK -> 0.36f + rng.nextFloat() * 0.10f
                else -> 0.33f + rng.nextFloat() * 0.15f
            }
            buildings.add(
                BuildingBox(
                    x = x, z = z, width = w, depth = d, height = h,
                    r = r, g = g, b = b,
                    style = style,
                    hasRoofAccent = h > 22f,
                    hasDome = style == BuildingStyle.OLD_TOWN_STONE && rng.nextFloat() < 0.35f
                )
            )
        }

        // Northwest Downtown Financial Towers
        val nwTowers = listOf(
            Triple(-90f, -90f, 44f), Triple(-90f, -30f, 36f), Triple(-44f, -90f, 42f),
            Triple(-142f, -90f, 52f), Triple(-142f, -30f, 38f), Triple(-44f, -30f, 30f),
            Triple(-142f, -142f, 58f), Triple(-90f, -142f, 48f), Triple(-44f, -142f, 38f)
        )
        for ((bx, bz, bh) in nwTowers) {
            if (abs(bx - (-122f)) < 16f && abs(bz - (-126f)) < 16f) continue
            addSkyscraper(bx, bz, 17f + rng.nextFloat() * 6f, 17f + rng.nextFloat() * 6f, bh, BuildingStyle.SKYSCRAPER_GLASS)
        }

        // Northeast Mtatsminda High-Rises & Towers
        val neTowers = listOf(
            Triple(90f, -90f, 34f), Triple(142f, -90f, 28f), Triple(90f, -142f, 40f),
            Triple(44f, -90f, 32f), Triple(44f, -142f, 36f), Triple(44f, -30f, 26f)
        )
        for ((bx, bz, bh) in neTowers) {
            addSkyscraper(bx, bz, 16f + rng.nextFloat() * 5f, 16f + rng.nextFloat() * 5f, bh, BuildingStyle.SKYSCRAPER_GLASS)
        }
        // TV Tower base pedestal at (142, -114)
        buildings.add(BuildingBox(142f, -114f, 9f, 9f, 6f, 0.25f, 0.27f, 0.32f, BuildingStyle.URBAN_BLOCK))

        // Southeast Narikala Old Town & Skyline Towers
        val seBuildings = listOf(
            Triple(92f, 32f, 16f), Triple(144f, 32f, 24f), Triple(144f, 90f, 26f),
            Triple(92f, 144f, 30f), Triple(144f, 144f, 34f), Triple(44f, 32f, 24f),
            Triple(44f, 144f, 28f)
        )
        for ((bx, bz, bh) in seBuildings) {
            val st = if (bh < 25f) BuildingStyle.OLD_TOWN_STONE else BuildingStyle.URBAN_BLOCK
            addSkyscraper(bx, bz, 16f + rng.nextFloat() * 5f, 15f + rng.nextFloat() * 5f, bh, st)
        }
        buildings.add(BuildingBox(142f, 128f, 26f, 5f, 9f, 0.48f, 0.40f, 0.30f, BuildingStyle.OLD_TOWN_STONE))

        // Southwest Harbor Towers & Rustavi Shipping Docks
        val swTowers = listOf(
            Triple(-44f, 92f, 26f), Triple(-44f, 144f, 36f), Triple(-92f, 144f, 40f),
            Triple(-142f, 32f, 32f), Triple(-44f, 32f, 28f), Triple(-142f, 144f, 30f)
        )
        for ((bx, bz, bh) in swTowers) {
            addSkyscraper(bx, bz, 17f, 16f, bh, BuildingStyle.URBAN_BLOCK)
        }

        val containerColors = listOf(
            Triple(0.70f, 0.20f, 0.16f),
            Triple(0.16f, 0.36f, 0.58f),
            Triple(0.18f, 0.48f, 0.32f),
            Triple(0.75f, 0.52f, 0.14f)
        )
        val containerSpots = listOf(
            -92f to 92f, -92f to 104f, -146f to 90f, -146f to 102f
        )
        containerSpots.forEachIndexed { idx, (cx, cz) ->
            val col = containerColors[idx % containerColors.size]
            buildings.add(
                BuildingBox(
                    x = cx, z = cz, width = 11f, depth = 4.2f, height = if (idx % 2 == 0) 7.2f else 3.8f,
                    r = col.first, g = col.second, b = col.third,
                    style = BuildingStyle.SHIPPING_CONTAINER
                )
            )
        }

        // 5. Special Interactive Buildings: Weapon Shop, Car Station, & 4 Buyable Houses
        buildings.add(
            BuildingBox(
                x = GameCatalog.WEAPON_SHOP_X - 9f, z = GameCatalog.WEAPON_SHOP_Z,
                width = 12f, depth = 11f, height = 7f,
                r = 0.22f, g = 0.20f, b = 0.18f,
                style = BuildingStyle.WEAPON_SHOP
            )
        )
        buildings.add(
            BuildingBox(
                x = GameCatalog.CAR_STATION_X + 10f, z = GameCatalog.CAR_STATION_Z,
                width = 14f, depth = 12f, height = 6.5f,
                r = 0.16f, g = 0.26f, b = 0.38f,
                style = BuildingStyle.CAR_STATION
            )
        )
        for (house in GameCatalog.houses) {
            buildings.add(
                BuildingBox(
                    x = house.x, z = house.z - 8f,
                    width = 12f, depth = 10f, height = 7.5f,
                    r = 0.72f, g = 0.64f, b = 0.52f,
                    style = BuildingStyle.HOUSE_PROPERTY,
                    hasRoofAccent = true
                )
            )
        }

        // 6. Rich Vegetation: 120+ Trees across Parks, Riverbanks, Highways, & Hillsides
        for (tz in -152..152 step 13) {
            if (abs(tz) < 12 || abs(tz - 60) < 12 || abs(tz + 60) < 12 || abs(tz - 120) < 12 || abs(tz + 120) < 12) continue
            trees.add(TreeProp(-16f, tz.toFloat(), 5.4f + rng.nextFloat() * 2.2f, 2.3f))
            trees.add(TreeProp(16f, tz.toFloat(), 5.6f + rng.nextFloat() * 2.2f, 2.4f))
        }
        // Highway & Boulevard Sidewalk Trees
        for (tx in listOf(-37f, 37f, -72f, 72f, -132f, 132f, -180f, 180f)) {
            for (tz in -150..150 step 30) {
                if (abs(tz) < 14 || abs(tz - 60) < 14 || abs(tz + 60) < 14 || abs(tz - 120) < 14 || abs(tz + 120) < 14) continue
                trees.add(TreeProp(tx, tz.toFloat(), 5.2f + rng.nextFloat() * 2.4f, 2.1f))
            }
        }
        // Park Grove Clusters
        for (park in parks) {
            if (abs(park.x) < 20f) continue
            for (k in 0 until 7) {
                val px = park.x + (rng.nextFloat() - 0.5f) * (park.width - 4f)
                val pz = park.z + (rng.nextFloat() - 0.5f) * (park.depth - 4f)
                trees.add(TreeProp(px, pz, 5.8f + rng.nextFloat() * 2.5f, 2.5f))
            }
        }

        // 7. Knockable Physical Props (crates, hazard barrels)
        for (i in 0 until 28) {
            val px = if (i < 12) -105f - rng.nextFloat() * 35f else -10f + rng.nextFloat() * 110f
            val pz = if (i < 12) 78f + rng.nextFloat() * 45f else -110f + rng.nextFloat() * 220f
            val isBarrel = i % 3 == 0
            props.add(
                PhysicsProp(
                    x = px, y = 0.55f, z = pz,
                    size = if (isBarrel) 0.9f else 1.15f,
                    r = if (isBarrel) 0.82f else 0.64f,
                    g = if (isBarrel) 0.22f else 0.45f,
                    b = if (isBarrel) 0.14f else 0.25f,
                    isBarrel = isBarrel
                )
            )
        }

        // 8. Parked & Moving Traffic Vehicles (including George's Arrival Taxi & Highway Cruisers)
        var vid = 1
        vehicles.add(VehicleEntity(vid++, VehicleModel.TAXI, -22f, 12f, 0.1f, isPlayerOwned = true))
        vehicles.add(VehicleEntity(vid++, VehicleModel.SEDAN, -28f, -4f, 0f, isPlayerOwned = true))
        vehicles.add(VehicleEntity(vid++, VehicleModel.COUPE, -42f, -6f, 1.57f))
        vehicles.add(VehicleEntity(vid++, VehicleModel.SUV, -84f, 68f, 0.2f))
        vehicles.add(VehicleEntity(vid++, VehicleModel.SUPERCAR, 52f, -44f, 3.14f))
        vehicles.add(VehicleEntity(vid++, VehicleModel.COUPE, 72f, 62f, -1.57f))

        // Airport Terminal & Runway Pad (South-East Express Edge)
        buildings.add(BuildingBox(152f, 178f, 36f, 14f, 12f, 0.28f, 0.34f, 0.44f, BuildingStyle.SKYSCRAPER_GLASS, hasRoofAccent = true))
        roads.add(RoadStrip(112f, 182f, 96f, 16f, isHighway = true))

        val trafficSpawns = listOf(
            Triple(-60f, -100f, 1f), Triple(-60f, 80f, -1f),
            Triple(60f, -80f, 1f), Triple(60f, 100f, -1f),
            Triple(-120f, 30f, 1f), Triple(120f, -30f, -1f),
            Triple(-164f, -40f, 1f), Triple(164f, 40f, -1f)
        )
        for ((tx, tz, dir) in trafficSpawns) {
            val model = VehicleModel.entries[vid % VehicleModel.entries.size]
            vehicles.add(
                VehicleEntity(
                    id = vid++,
                    model = model,
                    x = tx + (if (dir > 0) 3.6f else -3.6f),
                    z = tz,
                    yaw = if (dir > 0) 0f else 3.14159f,
                    speed = if (abs(tx) > 150f) 14f else 9.5f,
                    isTraffic = true,
                    trafficDirX = 0f,
                    trafficDirZ = dir
                )
            )
        }

        // 9. Named Story NPCs & 32 Varied Boys & Girls Pedestrians
        var nid = 1
        for (entry in GameCatalog.storyNpcs) {
            val isGirl = entry.voiceProfile.isFemale
            npcs.add(
                NpcEntity(
                    id = nid++,
                    kind = NpcKind.STORY_NPC,
                    name = entry.name,
                    x = entry.x,
                    z = entry.z,
                    yaw = 0.5f,
                    shirtR = if (isGirl) 0.90f else 0.92f,
                    shirtG = if (isGirl) 0.34f else 0.68f,
                    shirtB = if (isGirl) 0.58f else 0.16f,
                    voiceProfile = entry.voiceProfile,
                    dialogue = entry
                )
            )
        }
        val boyProfiles = listOf(
            "Giorgi" to VoiceProfile.BOY_TOUGH,
            "Luka" to VoiceProfile.BOY_CHILL,
            "Sandro" to VoiceProfile.BOY_HYPE,
            "Tornike" to VoiceProfile.COP,
            "Davit" to VoiceProfile.VENDOR,
            "Irakli" to VoiceProfile.BOY_TOUGH,
            "Nikolozi" to VoiceProfile.BOY_CHILL,
            "Bacho" to VoiceProfile.BOY_HYPE,
            "Saba" to VoiceProfile.BOY_TOUGH,
            "Lasha" to VoiceProfile.BOY_CHILL
        )
        val girlProfiles = listOf(
            "Salome" to VoiceProfile.GIRL_SWEET,
            "Elene" to VoiceProfile.GIRL_BRIGHT,
            "Ana" to VoiceProfile.GIRL_WARM,
            "Ketevan" to VoiceProfile.NINO,
            "Sofo" to VoiceProfile.GIRL_SWEET,
            "Nia" to VoiceProfile.GIRL_BRIGHT,
            "Maiko" to VoiceProfile.GIRL_WARM,
            "Lika" to VoiceProfile.GIRL_SWEET,
            "Tekla" to VoiceProfile.GIRL_BRIGHT,
            "Khatia" to VoiceProfile.GIRL_WARM
        )
        for (i in 0 until 32) {
            val px = if (i < 10) (-36f + (i - 5) * 14f) else (-140f + rng.nextFloat() * 280f)
            val pz = if (i < 10) (-28f + rng.nextFloat() * 56f) else (-140f + rng.nextFloat() * 280f)
            var safeX = if (abs(px) < 14f) 20f else px
            var safeZ = pz
            for (b in buildings) {
                val hw = b.width * 0.5f + 0.8f
                val hd = b.depth * 0.5f + 0.8f
                if (safeX in (b.x - hw)..(b.x + hw) && safeZ in (b.z - hd)..(b.z + hd)) {
                    safeX = if (safeX >= b.x) b.x + hw + 0.6f else b.x - hw - 0.6f
                }
            }
            val isGirl = i % 2 == 1
            val (pedName, pedVoice) = if (isGirl) {
                girlProfiles[(i / 2) % girlProfiles.size]
            } else {
                boyProfiles[(i / 2) % boyProfiles.size]
            }
            npcs.add(
                NpcEntity(
                    id = nid++,
                    kind = NpcKind.PEDESTRIAN,
                    name = pedName,
                    x = safeX,
                    z = safeZ,
                    yaw = rng.nextFloat() * 6.28f,
                    hp = 60,
                    maxHp = 60,
                    shirtR = if (isGirl) 0.74f + rng.nextFloat() * 0.24f else 0.18f + rng.nextFloat() * 0.30f,
                    shirtG = if (isGirl) 0.26f + rng.nextFloat() * 0.36f else 0.22f + rng.nextFloat() * 0.35f,
                    shirtB = if (isGirl) 0.56f + rng.nextFloat() * 0.36f else 0.35f + rng.nextFloat() * 0.45f,
                    voiceProfile = pedVoice
                )
            )
        }

        return CityWorldData(buildings, roads, roadDashes, parks, lamps, trees, props, vehicles, npcs)
    }
}
