package com.example.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.data.VoiceProfile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class VoiceActingEngine(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isReady = false

    private var maleVoices: List<Voice> = emptyList()
    private var femaleVoices: List<Voice> = emptyList()

    private val pendingUtterances = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    @Volatile
    var enabled: Boolean = true

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            val res = engine.setLanguage(Locale.US)
            isReady = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    utteranceId?.let { pendingUtterances.remove(it)?.complete(Unit) }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    utteranceId?.let { pendingUtterances.remove(it)?.complete(Unit) }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    utteranceId?.let { pendingUtterances.remove(it)?.complete(Unit) }
                }
            })

            if (isReady) {
                val allEnglish = runCatching {
                    engine.voices
                        ?.filter { it.locale.language == "en" && !it.isNetworkConnectionRequired }
                        ?.sortedBy { it.name }
                        .orEmpty()
                }.getOrDefault(emptyList())

                val maleHints = listOf("male", "man", "iom", "iol", "tpf", "gbb", "gbd", "rjs", "en-us-x-iol", "en-us-x-iom")
                val femaleHints = listOf("female", "woman", "girl", "sfg", "tpc", "tpd", "gba", "gbc", "en-us-x-sfg", "en-us-x-tpc")

                val males = allEnglish.filter { v ->
                    val n = v.name.lowercase(Locale.ROOT)
                    maleHints.any { n.contains(it) } && !n.contains("female")
                }
                val females = allEnglish.filter { v ->
                    val n = v.name.lowercase(Locale.ROOT)
                    femaleHints.any { n.contains(it) }
                }

                maleVoices = males.ifEmpty { allEnglish }
                femaleVoices = females.ifEmpty { allEnglish }
            }
        }
    }

    /**
     * Speaks the line and suspends until the TTS engine finishes (never overlapping voices).
     * Falls back to a natural reading duration if TTS is disabled or unavailable.
     */
    suspend fun speakAndAwait(text: String, profile: VoiceProfile, characterSeed: Int = 0) {
        val fallbackMs = (text.length * 55L).coerceIn(1400L, 5200L)
        if (!enabled || !isReady) {
            _isSpeaking.value = true
            kotlinx.coroutines.delay(fallbackMs)
            _isSpeaking.value = false
            return
        }

        val engine = tts ?: run {
            _isSpeaking.value = true
            kotlinx.coroutines.delay(fallbackMs)
            _isSpeaking.value = false
            return
        }

        val uid = "rc_${System.nanoTime()}"
        val deferred = CompletableDeferred<Unit>()
        pendingUtterances[uid] = deferred

        configureVoice(engine, profile, characterSeed)
        _isSpeaking.value = true
        val params = Bundle()
        val queued = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, uid)
        if (queued != TextToSpeech.SUCCESS) {
            pendingUtterances.remove(uid)
            kotlinx.coroutines.delay(fallbackMs)
            _isSpeaking.value = false
            return
        }

        // Wait until TTS finishes or safety timeout elapses
        withTimeoutOrNull((fallbackMs + 4500L).coerceAtLeast(6000L)) {
            deferred.await()
        }
        pendingUtterances.remove(uid)
        _isSpeaking.value = false
    }

    private fun configureVoice(engine: TextToSpeech, profile: VoiceProfile, characterSeed: Int) {
        val pool = if (profile.isFemale) femaleVoices else maleVoices
        if (pool.isNotEmpty()) {
            val slot = (profile.voiceSlot + characterSeed).mod(pool.size)
            val selected = pool[slot]
            runCatching { engine.voice = selected }
        }

        val pitchOffset = if (characterSeed == 0) 0f else ((characterSeed % 5) - 2) * 0.03f
        val rateOffset = if (characterSeed == 0) 0f else ((characterSeed % 3) - 1) * 0.03f
        val finalPitch = if (profile.isFemale) {
            (profile.pitch + pitchOffset).coerceIn(1.26f, 1.72f)
        } else {
            (profile.pitch + pitchOffset).coerceIn(0.42f, 0.90f)
        }
        val finalRate = (profile.rate + rateOffset).coerceIn(0.80f, 1.15f)

        engine.setPitch(finalPitch)
        engine.setSpeechRate(finalRate)
    }

    fun stop() {
        _isSpeaking.value = false
        pendingUtterances.values.forEach { it.complete(Unit) }
        pendingUtterances.clear()
        tts?.stop()
    }

    fun release() {
        stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
