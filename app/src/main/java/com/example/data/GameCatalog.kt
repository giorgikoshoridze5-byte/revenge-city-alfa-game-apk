package com.example.data

import androidx.annotation.DrawableRes
import com.example.R

enum class WeaponType(
    val id: String,
    val displayName: String,
    val price: Int,
    val damage: Int,
    val range: Float,
    val cooldownSec: Float,
    val pellets: Int,
    val spreadRad: Float,
    val description: String
) {
    FISTS(
        id = "FISTS",
        displayName = "Unarmed / Fists",
        price = 0,
        damage = 22,
        range = 2.6f,
        cooldownSec = 0.42f,
        pellets = 1,
        spreadRad = 0.0f,
        description = "Holster firearms and fight close-range with heavy strikes."
    ),
    PISTOL(
        id = "PISTOL",
        displayName = "9mm Silenced Pistol",
        price = 0,
        damage = 34,
        range = 32f,
        cooldownSec = 0.34f,
        pellets = 1,
        spreadRad = 0.02f,
        description = "George's signature suppressed sidearm. Accurate and reliable."
    ),
    SHOTGUN(
        id = "SHOTGUN",
        displayName = "Sawed-Off Shotgun",
        price = 350,
        damage = 28,
        range = 18f,
        cooldownSec = 0.85f,
        pellets = 5,
        spreadRad = 0.16f,
        description = "Devastating close-quarters spread for dockyard ambushes."
    ),
    SMG(
        id = "SMG",
        displayName = "Tactical SMG",
        price = 450,
        damage = 20,
        range = 28f,
        cooldownSec = 0.12f,
        pellets = 1,
        spreadRad = 0.06f,
        description = "Rapid-fire compact submachine gun built for street chases."
    ),
    AK47(
        id = "AK47",
        displayName = "Kalashnikov AK-47",
        price = 600,
        damage = 42,
        range = 42f,
        cooldownSec = 0.17f,
        pellets = 1,
        spreadRad = 0.03f,
        description = "Heavy-caliber rifle capable of stopping armored syndicate squads."
    )
}

enum class VehicleModel(
    val id: String,
    val displayName: String,
    val price: Int,
    val maxSpeed: Float,
    val acceleration: Float,
    val handling: Float,
    val r: Float,
    val g: Float,
    val b: Float,
    val length: Float,
    val width: Float,
    val height: Float,
    val description: String
) {
    TAXI(
        id = "TAXI",
        displayName = "Revenge Yellow Cab",
        price = 150,
        maxSpeed = 23f,
        acceleration = 16.5f,
        handling = 2.65f,
        r = 0.96f, g = 0.74f, b = 0.12f,
        length = 4.25f, width = 1.92f, height = 1.38f,
        description = "Classic Tbilisi airport yellow taxi that brought George back downtown."
    ),
    SEDAN(
        id = "SEDAN",
        displayName = "Tbilisi Street Sedan",
        price = 0,
        maxSpeed = 24f,
        acceleration = 17f,
        handling = 2.6f,
        r = 0.24f, g = 0.32f, b = 0.44f,
        length = 4.2f, width = 1.9f, height = 1.35f,
        description = "Dependable 4-door city cruiser with balanced handling."
    ),
    COUPE(
        id = "COUPE",
        displayName = "Shadow GT Coupe",
        price = 300,
        maxSpeed = 32f,
        acceleration = 23f,
        handling = 3.1f,
        r = 0.78f, g = 0.14f, b = 0.18f,
        length = 4.1f, width = 1.95f, height = 1.18f,
        description = "Twin-turbo sports coupe built for high-speed drift escapes."
    ),
    SUV(
        id = "SUV",
        displayName = "Kavkaz 4x4 Armored",
        price = 550,
        maxSpeed = 27f,
        acceleration = 19f,
        handling = 2.35f,
        r = 0.18f, g = 0.22f, b = 0.17f,
        length = 4.7f, width = 2.15f, height = 1.75f,
        description = "Reinforced mountain SUV that smashes through roadblocks."
    ),
    SUPERCAR(
        id = "SUPERCAR",
        displayName = "Apex V8 Supercar",
        price = 900,
        maxSpeed = 40f,
        acceleration = 30f,
        handling = 3.45f,
        r = 0.96f, g = 0.72f, b = 0.15f,
        length = 4.3f, width = 2.05f, height = 1.08f,
        description = "Exotic mid-engine hypercar dominating Revenge City avenues."
    )
}

data class BuyableHouse(
    val id: String,
    val name: String,
    val district: String,
    val price: Int,
    val x: Float,
    val z: Float,
    val spawnX: Float,
    val spawnZ: Float,
    val description: String
)

enum class VoiceProfile(
    val pitch: Float,
    val rate: Float,
    val isFemale: Boolean,
    val voiceSlot: Int
) {
    // Tough Men & Boys (deep, manly, distinct from one another)
    GEORGE(0.56f, 0.90f, isFemale = false, voiceSlot = 0),
    VILLAIN(0.44f, 0.82f, isFemale = false, voiceSlot = 1),
    COP(0.66f, 0.96f, isFemale = false, voiceSlot = 2),
    VENDOR(0.72f, 0.92f, isFemale = false, voiceSlot = 3),
    LEVAN(0.84f, 1.07f, isFemale = false, voiceSlot = 4),
    BOY_TOUGH(0.60f, 0.89f, isFemale = false, voiceSlot = 1),
    BOY_CHILL(0.76f, 0.97f, isFemale = false, voiceSlot = 2),
    BOY_HYPE(0.88f, 1.08f, isFemale = false, voiceSlot = 3),

    // Girls (bright, girly, expressive, each distinct)
    NINO(1.40f, 0.96f, isFemale = true, voiceSlot = 0),
    GIRL_BRIGHT(1.56f, 1.05f, isFemale = true, voiceSlot = 1),
    GIRL_WARM(1.30f, 0.90f, isFemale = true, voiceSlot = 2),
    GIRL_SWEET(1.66f, 1.09f, isFemale = true, voiceSlot = 3)
}

enum class DialogueEmotion {
    NEUTRAL,
    HAPPY,
    SERIOUS,
    ANGRY,
    SURPRISED,
    PHONE_CALL
}

data class DialogueLine(
    val speaker: String,
    val text: String,
    val voiceProfile: VoiceProfile,
    val cameraFocus: CutsceneCameraFocus = CutsceneCameraFocus.TWO_SHOT,
    val emotion: DialogueEmotion = DialogueEmotion.NEUTRAL
)

enum class CutsceneCameraFocus {
    GEORGE_CLOSEUP,
    NPC_CLOSEUP,
    TWO_SHOT,
    WIDE_DISTRICT
}

enum class MissionObjectiveType {
    GO_TO_POINT,
    ELIMINATE_TARGETS,
    PICKUP_ITEM,
    ESCAPE_WANTED,
    TIMED_REACH
}

data class MissionStage(
    val title: String,
    val instruction: String,
    val objectiveType: MissionObjectiveType,
    val targetX: Float,
    val targetZ: Float,
    val enemyCount: Int = 0,
    val wantedStars: Int = 0,
    val timeLimitSec: Float = 0f
)

data class MissionDefinition(
    val id: String,
    val number: Int,
    val title: String,
    val district: String,
    val startX: Float,
    val startZ: Float,
    val npcName: String,
    val npcRole: String,
    val rewardCash: Int = 300,
    val introCutscene: List<DialogueLine>,
    val outroCutscene: List<DialogueLine>,
    val stages: List<MissionStage>
)

data class LoadingSlide(
    @DrawableRes val imageRes: Int,
    val title: String,
    val subtitle: String
)

data class NpcDialogueEntry(
    val id: String,
    val name: String,
    val role: String,
    val x: Float,
    val z: Float,
    val voiceProfile: VoiceProfile,
    val lines: List<String>
)

object GameCatalog {

    val loadingSlides = listOf(
        LoadingSlide(
            imageRes = R.drawable.loading_art_tbilisi_bridge,
            title = "GEORGE NIKOLADZE",
            subtitle = "Returning to Revenge City to settle an old blood debt across the riverfront."
        ),
        LoadingSlide(
            imageRes = R.drawable.loading_art_crew_lineup,
            title = "THE SYNDICATE & OLD ALLIES",
            subtitle = "Every block is carved up by rival crews, crooked cops, and broken promises."
        ),
        LoadingSlide(
            imageRes = R.drawable.loading_art_docks_boss,
            title = "RUSTAVI DOCKS",
            subtitle = "Smuggled crates, armored SUVs, and heavy firepower wait behind the steel gates."
        ),
        LoadingSlide(
            imageRes = R.drawable.loading_art_mountain_tower,
            title = "ROOTS IN THE MOUNTAINS",
            subtitle = "Before the neon towers and silenced pistols, George learned survival in the highlands."
        ),
        LoadingSlide(
            imageRes = R.drawable.loading_art_orchard_flashback,
            title = "NEVER FORGET",
            subtitle = "Every mission pays $300. Earn respect, buy safehouses, and take back the city."
        )
    )

    val houses = listOf(
        BuyableHouse(
            id = "docks_safehouse",
            name = "Harbor Loft Safehouse",
            district = "Industrial Docks",
            price = 450,
            x = -125f,
            z = 115f,
            spawnX = -114f,
            spawnZ = 115f,
            description = "Reinforced brick hideout overlooking the shipping cranes. Restores full health & saves progress."
        ),
        BuyableHouse(
            id = "old_town_flat",
            name = "Old Town Balcony Flat",
            district = "Narikala Ridge",
            price = 600,
            x = 112f,
            z = 104f,
            spawnX = 98f,
            spawnZ = 104f,
            description = "Historic carved wooden balcony apartment steps away from the fortress walls."
        ),
        BuyableHouse(
            id = "riverside_loft",
            name = "Peace Bridge Glass Loft",
            district = "Riverfront Quay",
            price = 900,
            x = -46f,
            z = -24f,
            spawnX = -34f,
            spawnZ = -24f,
            description = "Modern steel-and-glass residence right beside the illuminated Peace Bridge."
        ),
        BuyableHouse(
            id = "mtatsminda_villa",
            name = "Mtatsminda Crest Villa",
            district = "TV Tower Heights",
            price = 1500,
            x = 132f,
            z = -128f,
            spawnX = 118f,
            spawnZ = -128f,
            description = "Luxury hilltop mansion beneath the neon TV Tower with panoramic skyline views."
        )
    )

    const val WEAPON_SHOP_X = -56f
    const val WEAPON_SHOP_Z = 56f

    const val CAR_STATION_X = 56f
    const val CAR_STATION_Z = -56f

    val storyNpcs = listOf(
        NpcDialogueEntry(
            id = "npc_levan",
            name = "Levan \"The Fox\"",
            role = "Street Informant",
            x = -12f,
            z = 18f,
            voiceProfile = VoiceProfile.LEVAN,
            lines = listOf(
                "George! Keep your head down. Beka's crew has eyes on the Peace Bridge.",
                "If you need heavy iron, hit Rustaveli Armory a block southwest of here.",
                "Every job you pull in this town pays three hundred crisp dollars. Spend it smart."
            )
        ),
        NpcDialogueEntry(
            id = "npc_nino",
            name = "Nino",
            role = "Old Town Fixer",
            x = 84f,
            z = 72f,
            voiceProfile = VoiceProfile.NINO,
            lines = listOf(
                "Welcome back to Narikala, George Nikoladze. The streets remember your name.",
                "Buy a safehouse in the districts so you always have a clean garage and medkit.",
                "Watch the night shift—police patrols double after sunset."
            )
        ),
        NpcDialogueEntry(
            id = "npc_armorer",
            name = "Master Tamaz",
            role = "Rustaveli Armory Owner",
            x = WEAPON_SHOP_X + 5f,
            z = WEAPON_SHOP_Z,
            voiceProfile = VoiceProfile.VENDOR,
            lines = listOf(
                "Need an AK-47 or Kevlar vest, George? Step onto the amber marker anytime.",
                "Keep your suppressor clean and your trigger finger steady."
            )
        ),
        NpcDialogueEntry(
            id = "npc_mechanic",
            name = "Giga Turbo",
            role = "Tbilisi Motors Chief",
            x = CAR_STATION_X - 5f,
            z = CAR_STATION_Z,
            voiceProfile = VoiceProfile.VENDOR,
            lines = listOf(
                "We tune the fastest rides in Revenge City. Bring cash, leave with horsepower.",
                "Need a quick chassis repair? Pull right into the blue bay."
            )
        ),
        NpcDialogueEntry(
            id = "npc_mariam",
            name = "Mariam",
            role = "Riverfront Street Artist",
            x = -24f,
            z = 22f,
            voiceProfile = VoiceProfile.GIRL_BRIGHT,
            lines = listOf(
                "Hey George! Love the leather jacket. The Peace Bridge glows so pretty after sunset!",
                "If you're looking for a fast ride, check out Tbilisi Motors across the river."
            )
        ),
        NpcDialogueEntry(
            id = "npc_tamuna",
            name = "Tamuna",
            role = "Downtown Lounge Singer",
            x = -46f,
            z = -38f,
            voiceProfile = VoiceProfile.GIRL_WARM,
            lines = listOf(
                "Careful in Downtown Plaza, George. Vakhtang's tough guys watch the penthouse doors.",
                "Earn $300 on a mission and treat yourself to the Riverside Glass Loft!"
            )
        )
    )

    val missions = listOf(
        MissionDefinition(
            id = "m1_bridge",
            number = 1,
            title = "Echoes on the Bridge",
            district = "Peace Bridge Riverfront",
            startX = -16f,
            startZ = 10f,
            npcName = "Levan \"The Fox\"",
            npcRole = "Informant",
            rewardCash = 300,
            introCutscene = listOf(
                DialogueLine(
                    "George Nikoladze",
                    "Just landed at Tbilisi Airport after five long years. Let me grab that yellow taxi to the city.",
                    VoiceProfile.GEORGE,
                    CutsceneCameraFocus.WIDE_DISTRICT,
                    DialogueEmotion.SERIOUS
                ),
                DialogueLine(
                    "Taxi Driver",
                    "Welcome back to Revenge City, boss! Buckle up, taking you straight down the highway.",
                    VoiceProfile.VENDOR,
                    CutsceneCameraFocus.NPC_CLOSEUP,
                    DialogueEmotion.HAPPY
                ),
                DialogueLine(
                    "Levan \"The Fox\"",
                    "George! Incoming call from Peace Bridge! How are you, man?",
                    VoiceProfile.LEVAN,
                    CutsceneCameraFocus.NPC_CLOSEUP,
                    DialogueEmotion.PHONE_CALL
                ),
                DialogueLine(
                    "George Nikoladze",
                    "I am good, man! Just got off the plane and my taxi is pulling up to Peace Bridge right now.",
                    VoiceProfile.GEORGE,
                    CutsceneCameraFocus.GEORGE_CLOSEUP,
                    DialogueEmotion.HAPPY
                ),
                DialogueLine(
                    "Levan \"The Fox\"",
                    "Good to see you face to face, George! A syndicate courier just crossed the Peace Bridge—take him out and grab the ledger briefcase!",
                    VoiceProfile.LEVAN,
                    CutsceneCameraFocus.TWO_SHOT,
                    DialogueEmotion.SERIOUS
                )
            ),
            outroCutscene = listOf(
                DialogueLine("George Nikoladze", "Got the briefcase. Only half the pages are here, which means Beka has the rest at the docks.", VoiceProfile.GEORGE, CutsceneCameraFocus.GEORGE_CLOSEUP, DialogueEmotion.SERIOUS),
                DialogueLine("Levan \"The Fox\"", "Here's your $300 cut, George. Upgrade your gear before heading to Rustavi Docks.", VoiceProfile.LEVAN, CutsceneCameraFocus.TWO_SHOT, DialogueEmotion.HAPPY)
            ),
            stages = listOf(
                MissionStage(
                    title = "Cross the Peace Bridge",
                    instruction = "Cross the glass Peace Bridge to intercept the courier.",
                    objectiveType = MissionObjectiveType.GO_TO_POINT,
                    targetX = 28f,
                    targetZ = 0f
                ),
                MissionStage(
                    title = "Eliminate the Escorts",
                    instruction = "Defeat the 3 syndicate escorts guarding the drop.",
                    objectiveType = MissionObjectiveType.ELIMINATE_TARGETS,
                    targetX = 48f,
                    targetZ = 16f,
                    enemyCount = 3
                ),
                MissionStage(
                    title = "Secure the Briefcase",
                    instruction = "Pick up the ledger briefcase on the east bank.",
                    objectiveType = MissionObjectiveType.PICKUP_ITEM,
                    targetX = 48f,
                    targetZ = 16f
                )
            )
        ),
        MissionDefinition(
            id = "m2_docks",
            number = 2,
            title = "Dockyard Betrayal",
            district = "Rustavi Shipping Docks",
            startX = -96f,
            startZ = 88f,
            npcName = "Beka \"Dock Boss\"",
            npcRole = "Syndicate Enforcer",
            rewardCash = 300,
            introCutscene = listOf(
                DialogueLine("George Nikoladze", "Beka! End of the line. Hand over the remaining shipping manifests.", VoiceProfile.GEORGE, CutsceneCameraFocus.GEORGE_CLOSEUP),
                DialogueLine("Beka \"Dock Boss\"", "You walked into a steel cage, Nikoladze. Boys—bury him between the containers!", VoiceProfile.VILLAIN, CutsceneCameraFocus.NPC_CLOSEUP)
            ),
            outroCutscene = listOf(
                DialogueLine("George Nikoladze", "Dockyard cleared. The manifests point straight to Downtown Financial Plaza.", VoiceProfile.GEORGE, CutsceneCameraFocus.TWO_SHOT)
            ),
            stages = listOf(
                MissionStage(
                    title = "Breach the Container Yard",
                    instruction = "Eliminate Beka's 4 armed dock guards.",
                    objectiveType = MissionObjectiveType.ELIMINATE_TARGETS,
                    targetX = -126f,
                    targetZ = 104f,
                    enemyCount = 4
                ),
                MissionStage(
                    title = "Recover the Manifest",
                    instruction = "Collect the encrypted shipping manifest crate.",
                    objectiveType = MissionObjectiveType.PICKUP_ITEM,
                    targetX = -132f,
                    targetZ = 112f
                )
            )
        ),
        MissionDefinition(
            id = "m3_downtown",
            number = 3,
            title = "Midnight Pickup",
            district = "Downtown Financial Plaza",
            startX = -58f,
            startZ = -58f,
            npcName = "Levan \"The Fox\"",
            npcRole = "Informant",
            rewardCash = 300,
            introCutscene = listOf(
                DialogueLine("Levan \"The Fox\"", "The syndicate parked an armored courier car beneath the glass skyscrapers.", VoiceProfile.LEVAN, CutsceneCameraFocus.NPC_CLOSEUP),
                DialogueLine("George Nikoladze", "I'll boost it myself, clear the plaza, and lose the cops.", VoiceProfile.GEORGE, CutsceneCameraFocus.GEORGE_CLOSEUP)
            ),
            outroCutscene = listOf(
                DialogueLine("George Nikoladze", "Clean getaway. Three hundred more in the pocket and the syndicate is sweating.", VoiceProfile.GEORGE, CutsceneCameraFocus.TWO_SHOT)
            ),
            stages = listOf(
                MissionStage(
                    title = "Clear Plaza Security",
                    instruction = "Take down the 3 guards watching the armored drop.",
                    objectiveType = MissionObjectiveType.ELIMINATE_TARGETS,
                    targetX = -82f,
                    targetZ = -92f,
                    enemyCount = 3,
                    wantedStars = 1
                ),
                MissionStage(
                    title = "Grab the Vault Codes",
                    instruction = "Collect the keycard from the financial plaza.",
                    objectiveType = MissionObjectiveType.PICKUP_ITEM,
                    targetX = -82f,
                    targetZ = -92f,
                    wantedStars = 2
                ),
                MissionStage(
                    title = "Deliver to River Quay",
                    instruction = "Drive fast and deliver the codes to the River Quay drop point.",
                    objectiveType = MissionObjectiveType.GO_TO_POINT,
                    targetX = -26f,
                    targetZ = -34f,
                    wantedStars = 2
                )
            )
        ),
        MissionDefinition(
            id = "m4_fortress",
            number = 4,
            title = "Fortress Deal Gone Wrong",
            district = "Narikala Old Town",
            startX = 82f,
            startZ = 76f,
            npcName = "Nino",
            npcRole = "Old Town Fixer",
            rewardCash = 300,
            introCutscene = listOf(
                DialogueLine("Nino", "George, thank god you're here! Vakhtang's hitmen surrounded the Old Town gate.", VoiceProfile.NINO, CutsceneCameraFocus.NPC_CLOSEUP),
                DialogueLine("George Nikoladze", "Stay behind me, Nino. Nobody touches Narikala while I'm standing.", VoiceProfile.GEORGE, CutsceneCameraFocus.GEORGE_CLOSEUP)
            ),
            outroCutscene = listOf(
                DialogueLine("Nino", "You saved my life, George. Take this $300 bounty—next stop is the TV Tower.", VoiceProfile.NINO, CutsceneCameraFocus.TWO_SHOT)
            ),
            stages = listOf(
                MissionStage(
                    title = "Defend Old Town",
                    instruction = "Eliminate the 5 syndicate hitmen near the fortress ridge.",
                    objectiveType = MissionObjectiveType.ELIMINATE_TARGETS,
                    targetX = 114f,
                    targetZ = 88f,
                    enemyCount = 5
                ),
                MissionStage(
                    title = "Reach Extraction Point",
                    instruction = "Escort Nino safely back down to the riverside boulevard.",
                    objectiveType = MissionObjectiveType.GO_TO_POINT,
                    targetX = 52f,
                    targetZ = 48f
                )
            )
        ),
        MissionDefinition(
            id = "m5_tower",
            number = 5,
            title = "Tower Signal",
            district = "Mtatsminda TV Tower",
            startX = 68f,
            startZ = -72f,
            npcName = "Officer Koba",
            npcRole = "Rogue Detective",
            rewardCash = 300,
            introCutscene = listOf(
                DialogueLine("Officer Koba", "Nikoladze, the syndicate is scrambling police frequencies from the Mtatsminda TV Tower!", VoiceProfile.COP, CutsceneCameraFocus.NPC_CLOSEUP),
                DialogueLine("George Nikoladze", "Give me 65 seconds. I'll climb that ridge and shut their relay down.", VoiceProfile.GEORGE, CutsceneCameraFocus.GEORGE_CLOSEUP)
            ),
            outroCutscene = listOf(
                DialogueLine("Officer Koba", "Signal restored! Here is your $300 tactical fee. Vakhtang is hiding at the Penthouse Plaza.", VoiceProfile.COP, CutsceneCameraFocus.TWO_SHOT)
            ),
            stages = listOf(
                MissionStage(
                    title = "Race to Mtatsminda Tower",
                    instruction = "Reach the TV Tower base before the scramble locks!",
                    objectiveType = MissionObjectiveType.TIMED_REACH,
                    targetX = 126f,
                    targetZ = -114f,
                    timeLimitSec = 65f
                ),
                MissionStage(
                    title = "Neutralize Tower Guard",
                    instruction = "Eliminate the 4 elite syndicate guards at the tower.",
                    objectiveType = MissionObjectiveType.ELIMINATE_TARGETS,
                    targetX = 126f,
                    targetZ = -114f,
                    enemyCount = 4
                ),
                MissionStage(
                    title = "Shut Down Jammer",
                    instruction = "Destroy and collect the signal jammer core.",
                    objectiveType = MissionObjectiveType.PICKUP_ITEM,
                    targetX = 132f,
                    targetZ = -120f
                )
            )
        ),
        MissionDefinition(
            id = "m6_finale",
            number = 6,
            title = "Revenge City Finale",
            district = "Syndicate Penthouse Plaza",
            startX = -108f,
            startZ = -108f,
            npcName = "Boss Vakhtang",
            npcRole = "Syndicate Kingpin",
            rewardCash = 300,
            introCutscene = listOf(
                DialogueLine("Boss Vakhtang", "Look at you, George Nikoladze. All this bloodshed over pride and a ruined name.", VoiceProfile.VILLAIN, CutsceneCameraFocus.NPC_CLOSEUP),
                DialogueLine("George Nikoladze", "You took everything from my family, Vakhtang. Tonight Revenge City belongs to us again.", VoiceProfile.GEORGE, CutsceneCameraFocus.GEORGE_CLOSEUP),
                DialogueLine("Boss Vakhtang", "Send every last guard! Don't let him leave this plaza alive!", VoiceProfile.VILLAIN, CutsceneCameraFocus.WIDE_DISTRICT)
            ),
            outroCutscene = listOf(
                DialogueLine("George Nikoladze", "It's done. Vakhtang's empire is finished. Welcome to a new dawn in Revenge City.", VoiceProfile.GEORGE, CutsceneCameraFocus.GEORGE_CLOSEUP)
            ),
            stages = listOf(
                MissionStage(
                    title = "Syndicate Showdown",
                    instruction = "Defeat Boss Vakhtang and his 6 heavy bodyguards!",
                    objectiveType = MissionObjectiveType.ELIMINATE_TARGETS,
                    targetX = -122f,
                    targetZ = -126f,
                    enemyCount = 6,
                    wantedStars = 2
                ),
                MissionStage(
                    title = "Claim the Kingpin Vault",
                    instruction = "Claim the Syndicate Master Key at the center of the plaza.",
                    objectiveType = MissionObjectiveType.PICKUP_ITEM,
                    targetX = -122f,
                    targetZ = -126f
                )
            )
        )
    )
}
