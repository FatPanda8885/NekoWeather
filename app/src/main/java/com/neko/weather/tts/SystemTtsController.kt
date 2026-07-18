package com.neko.weather.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.media.AudioAttributes
import android.os.Bundle
import com.neko.weather.live2d.CubismSignals
import com.neko.weather.model.VoiceSettings
import java.util.Locale

class SystemTtsController(context: Context) {
    private var ready = false
    private var pending: Pair<String, VoiceSettings>? = null
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                tts?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                val languageResult = tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = CubismSignals.setSpeaking(true)
                    override fun onDone(utteranceId: String?) = CubismSignals.setSpeaking(false)
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) = CubismSignals.setSpeaking(false)
                    override fun onError(utteranceId: String?, errorCode: Int) = CubismSignals.setSpeaking(false)
                    override fun onStop(utteranceId: String?, interrupted: Boolean) = CubismSignals.setSpeaking(false)
                })
                pending?.let { (text, settings) -> speak(text, settings) }
                pending = null
            } else {
                pending = null
            }
        }
    }

    fun speak(text: String, settings: VoiceSettings): Boolean {
        if (!ready) {
            pending = text to settings
            return true
        }
        val engine = tts ?: return false
        if (engine.setSpeechRate(settings.rate) == TextToSpeech.ERROR) return false
        if (engine.setPitch(settings.pitch) == TextToSpeech.ERROR) return false
        val parameters = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f)
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_MUSIC)
        }
        return engine.speak(text, TextToSpeech.QUEUE_FLUSH, parameters, "weather-${System.currentTimeMillis()}") == TextToSpeech.SUCCESS
    }

    fun stop() {
        CubismSignals.setSpeaking(false)
        tts?.stop()
    }

    fun shutdown() {
        CubismSignals.setSpeaking(false)
        tts?.shutdown()
        tts = null
    }
}
