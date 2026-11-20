package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.RevengeAudioEngine
import com.example.audio.VoiceActingEngine
import com.example.data.BuyableHouse
import com.example.data.DialogueEmotion
import com.example.data.DialogueLine
import com.example.data.GameCatalog
import com.example.data.GameRepository
import com.example.data.GameSettings
import com.example.data.GraphicsQuality
import com.example.data.MissionDefinition
import com.example.data.NpcDialogueEntry
import com.example.data.PlayerProgressEntity
import com.example.data.TimeCycleSpeed
import com.example.data.VehicleModel
import com.example.data.WeaponType
import com.example.engine.HudSnapshot
import com.example.engine.InteractableType
import com.example.engine.SimulationListener
import com.example.engine.WorldSimulation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppScreen {
    LOADING,
    MAIN_MENU,
    SETTINGS,
    PLAYING
}

enum class ActiveModal {
    NONE,
    WEAPON_SHOP,
    CAR_STATION,
    BUY_HOUSE,
    NPC_DIALOGUE,
    FULL_MAP,
    PAUSE_MENU,
    MISSION_PASSED,
    MISSION_FAILED,
    WASTED
}

data class CutsceneState(
    val mission: MissionDefinition,
    val isOutro: Boolean,
    val lines: List<DialogueLine>,
    val currentLineIndex: Int
)

data class NpcChatState(
    val npc: NpcDialogueEntry,
    val lineIndex: Int
)

data class GameUiState(
    val screen: AppScreen = AppScreen.LOADING,
    val loadingProgress: Float = 0f,
    val loadingComplete: Boolean = false,
    val progress: PlayerProgressEntity = PlayerProgressEntity(),
    val settings: GameSettings = GameSettings(),
    val activeModal: ActiveModal = ActiveModal.NONE,
    val selectedHouse: BuyableHouse? = null,
    val cutscene: CutsceneState? = null,
    val npcChat: NpcChatState? = null,
    val lastMissionTitle: String = "",
    val lastMissionFailReason: String = "",
    val toastMessage: String? = null,
    val isPaused: Boolean = false,
    val isSpeaking: Boolean = false
)

class GameViewModel(application: Application) : AndroidViewModel(application), SimulationListener {

    private val repository = GameRepository(application)
    val audio = RevengeAudioEngine(application)
    val voice = VoiceActingEngine(application)
    val simulation = WorldSimulation(this)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _hudState = MutableStateFlow(simulation.buildHudSnapshot())
    val hudState: StateFlow<HudSnapshot> = _hudState.asStateFlow()

    private var lastToastTimeMs = 0L
    private var cutscenePlaybackJob: Job? = null
    private var npcDialogueJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }

        // Observe active speech state for mouth animation
        viewModelScope.launch {
            voice.isSpeaking.collect { speaking ->
                simulation.isTalkingNow = speaking
                _uiState.update { it.copy(isSpeaking = speaking) }
            }
        }

        // Observe Room progress
        viewModelScope.launch {
            repository.progressFlow.collect { prog ->
                _uiState.update { it.copy(progress = prog) }
                simulation.syncPersistentState(
                    cash = prog.cash,
                    health = prog.health,
                    armor = prog.armor,
                    weapon = WeaponType.entries.firstOrNull { w -> w.id == prog.equippedWeapon } ?: WeaponType.PISTOL,
                    ownedWeps = prog.ownedWeaponsCsv.split(",").mapNotNull { id -> WeaponType.entries.firstOrNull { it.id == id } }.toSet().ifEmpty { setOf(WeaponType.PISTOL) },
                    ownedHouseIds = prog.ownedHousesCsv.split(",").filter { it.isNotBlank() }.toSet(),
                    completedMissionIds = prog.completedMissionsCsv.split(",").filter { it.isNotBlank() }.toSet()
                )
            }
        }

        // Observe DataStore settings
        viewModelScope.launch {
            repository.settingsFlow.collect { s ->
                _uiState.update { it.copy(settings = s) }
                audio.musicVolume = s.musicVolume
                audio.sfxVolume = s.sfxVolume
                voice.enabled = s.voiceActingEnabled
                simulation.graphicsQuality = s.graphicsQuality
                simulation.timeCycleSpeed = s.timeCycleSpeed
                simulation.touchSensitivity = s.touchSensitivity
                simulation.dynamicCameraTilt = s.dynamicCameraTilt
                simulation.streetLightsEnabled = s.streetLightsEnabled
            }
        }

        // Start Loading screen music ("Iron Tide") and slower cinematic progress timer (~15.5s across all 5 photos)
        audio.setMusicMode(RevengeAudioEngine.MusicMode.LOADING_MENU)
        viewModelScope.launch {
            val steps = 100
            for (i in 1..steps) {
                delay(155L)
                _uiState.update {
                    it.copy(
                        loadingProgress = i / steps.toFloat(),
                        loadingComplete = i == steps
                    )
                }
            }
        }

        // Periodic HUD snapshot sampler (~20Hz for crisp minimap & gauges without Compose over-recomposition)
        viewModelScope.launch {
            while (isActive) {
                if (_uiState.value.screen == AppScreen.PLAYING) {
                    _hudState.value = simulation.buildHudSnapshot()
                }
                delay(50L)
            }
        }
    }

    fun enterMainMenuFromLoading() {
        _uiState.update { it.copy(screen = AppScreen.MAIN_MENU) }
        audio.setMusicMode(RevengeAudioEngine.MusicMode.LOADING_MENU)
    }

    fun openSettings() {
        _uiState.update { it.copy(screen = AppScreen.SETTINGS) }
    }

    fun closeSettingsToMenu() {
        val backScreen = if (_uiState.value.activeModal == ActiveModal.PAUSE_MENU) AppScreen.PLAYING else AppScreen.MAIN_MENU
        _uiState.update { it.copy(screen = backScreen) }
    }

    fun enterOpenWorld(startMission: MissionDefinition? = null) {
        _uiState.update {
            it.copy(
                screen = AppScreen.PLAYING,
                activeModal = ActiveModal.NONE,
                isPaused = false
            )
        }
        // Stop menu/loading music once entering gameplay
        audio.setMusicMode(RevengeAudioEngine.MusicMode.NONE)
        if (startMission != null) {
            simulation.startMissionDirectly(startMission)
        } else {
            val done = _uiState.value.progress.completedMissionsCsv.split(",").filter { it.isNotBlank() }.toSet()
            if (!done.contains("m1_bridge")) {
                GameCatalog.missions.firstOrNull()?.let { firstMission ->
                    simulation.startMissionDirectly(firstMission)
                }
            }
        }
    }

    fun returnToMainMenu() {
        cutscenePlaybackJob?.cancel()
        cutscenePlaybackJob = null
        npcDialogueJob?.cancel()
        npcDialogueJob = null
        simulation.talkingSpeakerName = null
        voice.stop()
        _uiState.update {
            it.copy(
                screen = AppScreen.MAIN_MENU,
                activeModal = ActiveModal.NONE,
                cutscene = null,
                npcChat = null,
                isPaused = false
            )
        }
        audio.setMusicMode(RevengeAudioEngine.MusicMode.LOADING_MENU)
    }

    fun togglePauseMenu() {
        val current = _uiState.value.activeModal
        if (current == ActiveModal.PAUSE_MENU) {
            _uiState.update { it.copy(activeModal = ActiveModal.NONE, isPaused = false) }
        } else if (current == ActiveModal.NONE && _uiState.value.cutscene == null) {
            _uiState.update { it.copy(activeModal = ActiveModal.PAUSE_MENU, isPaused = true) }
        }
    }

    fun closeActiveModal() {
        npcDialogueJob?.cancel()
        npcDialogueJob = null
        simulation.talkingSpeakerName = null
        voice.stop()
        _uiState.update {
            it.copy(
                activeModal = ActiveModal.NONE,
                selectedHouse = null,
                npcChat = null,
                isPaused = false
            )
        }
    }

    fun triggerNearbyInteraction() {
        val nearby = _hudState.value.nearbyInteractable ?: return
        when (nearby.type) {
            InteractableType.MISSION_START -> {
                val mission = GameCatalog.missions.firstOrNull { it.id == nearby.id } ?: return
                simulation.startMissionDirectly(mission)
            }
            InteractableType.WEAPON_SHOP -> {
                _uiState.update { it.copy(activeModal = ActiveModal.WEAPON_SHOP, isPaused = true) }
            }
            InteractableType.CAR_STATION -> {
                _uiState.update { it.copy(activeModal = ActiveModal.CAR_STATION, isPaused = true) }
            }
            InteractableType.BUYABLE_HOUSE -> {
                val house = GameCatalog.houses.firstOrNull { it.id == nearby.id } ?: return
                _uiState.update { it.copy(activeModal = ActiveModal.BUY_HOUSE, selectedHouse = house, isPaused = true) }
            }
            InteractableType.TALK_NPC -> {
                val npcId = nearby.id.toIntOrNull()
                val worldNpc = synchronized(simulation.lock) {
                    simulation.world.npcs.firstOrNull { it.id == npcId }
                }
                val voiceProfile = worldNpc?.voiceProfile ?: com.example.data.VoiceProfile.BOY_CHILL
                val defaultLines = if (voiceProfile.isFemale) {
                    listOf(
                        "Hey George! How are you, man? Stay safe out there tonight!",
                        "I am good, thank you! Check the GPS map if you want to cruise across the bridge."
                    )
                } else {
                    listOf(
                        "Hey George! Welcome back from the airport! How are you, man?",
                        "I am good, brother! Hit Rustaveli Armory if you want to upgrade your weapons."
                    )
                }
                val entry = worldNpc?.dialogue ?: NpcDialogueEntry(
                    id = "ped_$npcId",
                    name = nearby.label,
                    role = if (voiceProfile.isFemale) "Revenge City Girl" else "Revenge City Guy",
                    x = 0f,
                    z = 0f,
                    voiceProfile = voiceProfile,
                    lines = defaultLines
                )
                npcDialogueJob?.cancel()
                npcDialogueJob = viewModelScope.launch {
                    for (idx in entry.lines.indices) {
                        simulation.talkingSpeakerName = entry.name
                        _uiState.update {
                            it.copy(
                                activeModal = ActiveModal.NPC_DIALOGUE,
                                npcChat = NpcChatState(entry, idx),
                                isPaused = true
                            )
                        }
                        voice.speakAndAwait(entry.lines[idx], entry.voiceProfile, entry.name.hashCode())
                        delay(2000L)
                    }
                    closeActiveModal()
                }
            }
        }
    }

    fun openFullMap() {
        _uiState.update { it.copy(activeModal = ActiveModal.FULL_MAP, isPaused = true) }
    }

    fun setGpsWaypoint(x: Float, z: Float, label: String) {
        simulation.setGpsWaypoint(x, z, label)
        showToast("GPS Locked: $label")
    }

    fun clearGpsWaypoint() {
        simulation.clearGpsWaypoint()
        showToast("GPS Waypoint Cleared")
    }

    fun skipCutscene() {
        cutscenePlaybackJob?.cancel()
        cutscenePlaybackJob = null
        val cs = _uiState.value.cutscene ?: return
        finishCutsceneInternal(cs)
    }

    private fun finishCutsceneInternal(cs: CutsceneState) {
        simulation.talkingSpeakerName = null
        voice.stop()
        _uiState.update { it.copy(cutscene = null) }
        if (cs.isOutro) {
            simulation.finishOutroCutscene()
        } else {
            simulation.beginMissionGameplayAfterIntro()
            showToast("Mission Started: ${cs.mission.title}")
        }
    }

    private var lastFirearmId: String = WeaponType.PISTOL.id

    fun cycleEquippedWeapon() {
        val prog = _uiState.value.progress
        val owned = (listOf(WeaponType.FISTS) + prog.ownedWeaponsCsv.split(",")
            .mapNotNull { id -> WeaponType.entries.firstOrNull { it.id == id && it != WeaponType.FISTS } })
            .distinct()
        val idx = owned.indexOfFirst { it.id == prog.equippedWeapon }.coerceAtLeast(0)
        val nextWeapon = owned[(idx + 1) % owned.size]
        if (nextWeapon != WeaponType.FISTS) lastFirearmId = nextWeapon.id
        viewModelScope.launch {
            repository.saveProgress(prog.copy(equippedWeapon = nextWeapon.id))
        }
        showToast("Weapon: ${nextWeapon.displayName}")
    }

    fun toggleWeaponHolster() {
        val prog = _uiState.value.progress
        val ownedGuns = prog.ownedWeaponsCsv.split(",")
            .mapNotNull { id -> WeaponType.entries.firstOrNull { it.id == id && it != WeaponType.FISTS } }
            .ifEmpty { listOf(WeaponType.PISTOL) }
        val nextWeapon = if (prog.equippedWeapon == WeaponType.FISTS.id) {
            ownedGuns.firstOrNull { it.id == lastFirearmId } ?: ownedGuns.last()
        } else {
            lastFirearmId = prog.equippedWeapon
            WeaponType.FISTS
        }
        viewModelScope.launch {
            repository.saveProgress(prog.copy(equippedWeapon = nextWeapon.id))
        }
        showToast(if (nextWeapon == WeaponType.FISTS) "Weapon Holstered (Fists)" else "Drawn: ${nextWeapon.displayName}")
    }

    fun buyOrEquipWeapon(weapon: WeaponType) {
        val prog = _uiState.value.progress
        val owned = prog.ownedWeaponsCsv.split(",").filter { it.isNotBlank() }.toMutableSet()
        if (owned.contains(weapon.id)) {
            viewModelScope.launch { repository.saveProgress(prog.copy(equippedWeapon = weapon.id)) }
            showToast("Equipped ${weapon.displayName}")
            return
        }
        if (prog.cash < weapon.price) {
            showToast("Need $${weapon.price} cash!")
            return
        }
        owned.add(weapon.id)
        audio.playCashPickup()
        viewModelScope.launch {
            repository.saveProgress(
                prog.copy(
                    cash = prog.cash - weapon.price,
                    ownedWeaponsCsv = owned.joinToString(","),
                    equippedWeapon = weapon.id
                )
            )
        }
        showToast("Purchased ${weapon.displayName}!")
    }

    fun buyArmorVest() {
        val prog = _uiState.value.progress
        if (prog.cash < 200) {
            showToast("Need $200 for Kevlar Armor!")
            return
        }
        audio.playCashPickup()
        viewModelScope.launch {
            repository.saveProgress(prog.copy(cash = prog.cash - 200, armor = 100))
        }
        showToast("Kevlar Armor equipped (100%)")
    }

    fun buyOrSpawnVehicle(model: VehicleModel) {
        val prog = _uiState.value.progress
        val owned = prog.ownedVehiclesCsv.split(",").filter { it.isNotBlank() }.toMutableSet()
        if (owned.contains(model.id)) {
            simulation.spawnOwnedVehicleNearPlayer(model)
            viewModelScope.launch { repository.saveProgress(prog.copy(activeVehicle = model.id)) }
            closeActiveModal()
            showToast("Delivered ${model.displayName} outside!")
            return
        }
        if (prog.cash < model.price) {
            showToast("Need $${model.price} cash!")
            return
        }
        owned.add(model.id)
        audio.playCashPickup()
        simulation.spawnOwnedVehicleNearPlayer(model)
        viewModelScope.launch {
            repository.saveProgress(
                prog.copy(
                    cash = prog.cash - model.price,
                    ownedVehiclesCsv = owned.joinToString(","),
                    activeVehicle = model.id
                )
            )
        }
        closeActiveModal()
        showToast("Bought & delivered ${model.displayName}!")
    }

    fun repairVehicleAtStation() {
        val prog = _uiState.value.progress
        if (prog.cash < 50) {
            showToast("Need $50 for chassis repair!")
            return
        }
        audio.playCashPickup()
        simulation.repairActiveOrNearestVehicle()
        viewModelScope.launch { repository.saveProgress(prog.copy(cash = prog.cash - 50)) }
        showToast("Vehicle repaired & tuned!")
    }

    fun buyOrRestAtHouse(house: BuyableHouse) {
        val prog = _uiState.value.progress
        val owned = prog.ownedHousesCsv.split(",").filter { it.isNotBlank() }.toMutableSet()
        if (owned.contains(house.id)) {
            simulation.healAtHouse(house)
            val activeModel = VehicleModel.entries.firstOrNull { it.id == prog.activeVehicle } ?: VehicleModel.SEDAN
            simulation.spawnOwnedVehicleNearPlayer(activeModel, house.spawnX + 4f, house.spawnZ + 3f)
            viewModelScope.launch { repository.saveProgress(prog.copy(health = 100, armor = maxOf(prog.armor, 50))) }
            closeActiveModal()
            showToast("Rested at ${house.name}. Health restored!")
            return
        }
        if (prog.cash < house.price) {
            showToast("Need $${house.price} to buy ${house.name}!")
            return
        }
        owned.add(house.id)
        audio.playMissionPassed()
        simulation.healAtHouse(house)
        viewModelScope.launch {
            repository.saveProgress(
                prog.copy(
                    cash = prog.cash - house.price,
                    ownedHousesCsv = owned.joinToString(","),
                    health = 100,
                    armor = maxOf(prog.armor, 50)
                )
            )
        }
        closeActiveModal()
        showToast("Purchased ${house.name}!")
    }

    fun startMissionFromMenu(mission: MissionDefinition) {
        enterOpenWorld(startMission = mission)
    }

    fun handleWastedRespawn() {
        simulation.respawnAfterWasted()
        viewModelScope.launch {
            val prog = _uiState.value.progress
            repository.saveProgress(prog.copy(health = 100, armor = 25))
        }
        _uiState.update { it.copy(activeModal = ActiveModal.NONE, isPaused = false) }
    }

    // Settings updates
    fun setGraphicsQuality(quality: GraphicsQuality) {
        viewModelScope.launch { repository.updateSettings { it.copy(graphicsQuality = quality) } }
    }

    fun setMusicVolume(v: Float) {
        viewModelScope.launch { repository.updateSettings { it.copy(musicVolume = v) } }
    }

    fun setSfxVolume(v: Float) {
        viewModelScope.launch { repository.updateSettings { it.copy(sfxVolume = v) } }
    }

    fun setVoiceEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateSettings { it.copy(voiceActingEnabled = enabled) } }
    }

    fun setTouchSensitivity(sens: Float) {
        viewModelScope.launch { repository.updateSettings { it.copy(touchSensitivity = sens) } }
    }

    fun setTimeSpeed(speed: TimeCycleSpeed) {
        viewModelScope.launch { repository.updateSettings { it.copy(timeCycleSpeed = speed) } }
    }

    fun setShowFps(show: Boolean) {
        viewModelScope.launch { repository.updateSettings { it.copy(showFpsCounter = show) } }
    }

    fun setDynamicCam(enabled: Boolean) {
        viewModelScope.launch { repository.updateSettings { it.copy(dynamicCameraTilt = enabled) } }
    }

    fun setStreetLightsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateSettings { it.copy(streetLightsEnabled = enabled) } }
        showToast(if (enabled) "Street Lights ON (Saved)" else "Street Lights Saving Mode ON (Saved)")
    }

    fun resetProgress() {
        viewModelScope.launch {
            repository.resetStoryProgress()
            showToast("Story & assets reset to fresh start")
        }
    }

    fun showToast(msg: String) {
        lastToastTimeMs = System.currentTimeMillis()
        _uiState.update { it.copy(toastMessage = msg) }
        viewModelScope.launch {
            delay(2600L)
            if (System.currentTimeMillis() - lastToastTimeMs >= 2500L) {
                _uiState.update { it.copy(toastMessage = null) }
            }
        }
    }

    // SimulationListener callbacks
    override fun onPlayWeaponSound(weapon: WeaponType) = audio.playWeaponFire(weapon)
    override fun onPlayHorn() = audio.playCarHorn()
    override fun onPlayCollision() = audio.playCollision()
    override fun onPlayCashPickup() = audio.playCashPickup()

    override fun onStartCutscene(mission: MissionDefinition, isOutro: Boolean) {
        val lines = if (isOutro) mission.outroCutscene else mission.introCutscene
        if (lines.isEmpty()) return
        cutscenePlaybackJob?.cancel()
        cutscenePlaybackJob = viewModelScope.launch {
            for (idx in lines.indices) {
                val line = lines[idx]
                simulation.cutsceneFocus = line.cameraFocus
                simulation.talkingSpeakerName = line.speaker
                _uiState.update {
                    it.copy(
                        activeModal = ActiveModal.NONE,
                        cutscene = CutsceneState(mission, isOutro, lines, idx),
                        isPaused = false
                    )
                }
                voice.speakAndAwait(line.text, line.voiceProfile, line.speaker.hashCode())
                delay(2000L)
            }
            val activeCs = _uiState.value.cutscene ?: CutsceneState(mission, isOutro, lines, lines.lastIndex)
            finishCutsceneInternal(activeCs)
        }
    }

    override fun onMissionCompleted(mission: MissionDefinition) {
        audio.playMissionPassed()
        viewModelScope.launch {
            val prog = _uiState.value.progress
            val done = prog.completedMissionsCsv.split(",").filter { it.isNotBlank() }.toMutableSet()
            done.add(mission.id)
            repository.saveProgress(
                prog.copy(
                    cash = prog.cash + mission.rewardCash, // Always +$300!
                    completedMissionsCsv = done.joinToString(","),
                    health = 100
                )
            )
        }
        _uiState.update {
            it.copy(
                activeModal = ActiveModal.MISSION_PASSED,
                lastMissionTitle = mission.title
            )
        }
    }

    override fun onMissionFailed(reason: String) {
        _uiState.update {
            it.copy(
                activeModal = ActiveModal.MISSION_FAILED,
                lastMissionFailReason = reason
            )
        }
    }

    override fun onPlayerWasted() {
        cutscenePlaybackJob?.cancel()
        cutscenePlaybackJob = null
        npcDialogueJob?.cancel()
        npcDialogueJob = null
        simulation.talkingSpeakerName = null
        voice.stop()
        _uiState.update {
            it.copy(
                activeModal = ActiveModal.WASTED,
                cutscene = null,
                npcChat = null,
                isPaused = true
            )
        }
    }

    override fun onCashGained(amount: Int) {
        viewModelScope.launch {
            val prog = _uiState.value.progress
            repository.saveProgress(prog.copy(cash = prog.cash + amount))
        }
        showToast("+$${amount} Cash Collected")
    }

    override fun onHealthChanged(health: Int, armor: Int) {
        viewModelScope.launch {
            val prog = _uiState.value.progress
            repository.saveProgress(prog.copy(health = health, armor = armor))
        }
    }

    override fun onCleared() {
        super.onCleared()
        audio.release()
        voice.release()
    }
}
