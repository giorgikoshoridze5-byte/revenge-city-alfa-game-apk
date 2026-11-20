package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import com.example.R
import com.example.data.WeaponType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Audio engine for Revenge City:
 * - Plays the Iron Tide soundtrack (R.raw.iron_tide_theme) ONLY during Loading Screen & Main Menu
 * - Real-time procedural SFX for weapons, car horn, collisions, cash pickup, and mission passed
 */
class RevengeAudioEngine(private val context: Context) {

    enum class MusicMode {
        NONE,
        LOADING_MENU
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var mediaPlayer: MediaPlayer? = null

    @Volatile
    var musicVolume: Float = 0.75f
        set(value) {
            field = value.coerceIn(0f, 1f)
            runCatching { mediaPlayer?.setVolume(field, field) }
        }

    @Volatile
    var sfxVolume: Float = 0.85f

    @Volatile
    private var currentMode: MusicMode = MusicMode.NONE

    private val sampleRate = 22050

    @Synchronized
    fun setMusicMode(mode: MusicMode) {
        if (currentMode == mode && (mode == MusicMode.NONE || mediaPlayer?.isPlaying == true)) return
        currentMode = mode
        if (mode == MusicMode.NONE) {
            stopMenuMusic()
            return
        }
        startMenuMusic()
    }

    private fun startMenuMusic() {
        stopMenuMusic()
        runCatching {
            val mp = MediaPlayer.create(context.applicationContext, R.raw.iron_tide_theme)
            mp?.apply {
                isLooping = true
                setVolume(musicVolume, musicVolume)
                start()
            }
            mediaPlayer = mp
        }
    }

    private fun stopMenuMusic() {
        runCatching {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        }
        mediaPlayer = null
    }

    fun playWeaponFire(weapon: WeaponType) {
        if (sfxVolume <= 0.02f) return
        val durationMs = when (weapon) {
            WeaponType.FISTS -> 75
            WeaponType.PISTOL -> 90
            WeaponType.SHOTGUN -> 170
            WeaponType.SMG -> 65
            WeaponType.AK47 -> 110
        }
        val basePitch = when (weapon) {
            WeaponType.FISTS -> 95.0
            WeaponType.PISTOL -> 260.0
            WeaponType.SHOTGUN -> 115.0
            WeaponType.SMG -> 320.0
            WeaponType.AK47 -> 175.0
        }
        playOneShot(durationMs) { t ->
            val env = exp(-t * (1000.0 / durationMs) * 4.5)
            val noiseScale = if (weapon == WeaponType.FISTS) 0.25 else 0.65
            val noise = (Random.nextDouble() * 2.0 - 1.0) * noiseScale
            val punch = sin(2.0 * PI * (basePitch * exp(-t * 18.0)) * t) * 0.55
            (noise + punch) * env * 0.75
        }
    }

    fun playCarHorn() {
        if (sfxVolume <= 0.02f) return
        playOneShot(240) { t ->
            val env = if (t < 0.02) t / 0.02 else if (t > 0.20) (0.24 - t) / 0.04 else 1.0
            (sin(2.0 * PI * 349.23 * t) * 0.45 + sin(2.0 * PI * 440.0 * t) * 0.45) * env * 0.45
        }
    }

    fun playCollision() {
        if (sfxVolume <= 0.02f) return
        playOneShot(130) { t ->
            val env = exp(-t * 28.0)
            val thud = sin(2.0 * PI * 75.0 * t) * 0.7
            val crunch = (Random.nextDouble() * 2.0 - 1.0) * 0.35
            (thud + crunch) * env * 0.65
        }
    }

    fun playCashPickup() {
        if (sfxVolume <= 0.02f) return
        playOneShot(280) { t ->
            val f = if (t < 0.11) 880.0 else 1318.5
            val env = exp(-(if (t < 0.11) t else t - 0.11) * 14.0)
            sin(2.0 * PI * f * t) * env * 0.48
        }
    }

    fun playMissionPassed() {
        if (sfxVolume <= 0.02f) return
        playOneShot(900) { t ->
            val chord = when {
                t < 0.22 -> 440.0
                t < 0.44 -> 554.37
                else -> 659.25
            }
            val env = exp(-(t % 0.3) * 4.5)
            (sin(2.0 * PI * chord * t) + 0.4 * sin(2.0 * PI * (chord * 1.5) * t)) * env * 0.45
        }
    }

    private fun playOneShot(durationMs: Int, generator: (Double) -> Double) {
        val vol = sfxVolume.coerceIn(0f, 1f)
        scope.launch {
            val count = (sampleRate * durationMs) / 1000
            if (count <= 0) return@launch
            val buf = ShortArray(count)
            for (i in 0 until count) {
                val t = i.toDouble() / sampleRate.toDouble()
                val s = (generator(t) * vol).coerceIn(-0.95, 0.95)
                buf[i] = (s * 32767.0).toInt().toShort()
            }
            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(count * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            }.getOrNull() ?: return@launch

            runCatching {
                track.write(buf, 0, count)
                track.play()
                kotlinx.coroutines.delay(durationMs.toLong() + 60L)
                track.stop()
                track.release()
            }
        }
    }

    @Synchronized
    fun release() {
        currentMode = MusicMode.NONE
        stopMenuMusic()
    }
}
