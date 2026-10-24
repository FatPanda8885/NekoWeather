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
    /** 引擎初始化成功但缺少中文语音数据时为 true，此时 speak 直接失败并提示用户。 */
    private var chineseVoiceMissing = false
    private var pending: Pair<String, VoiceSettings>? = null
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                // USAGE_MEDIA 而非 ASSISTANT：部分 OEM（如 Oplus）把 Assistant 流路由到
                // 默认静音的通道，导致「播放成功」却听不到声音。
                tts?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                val languageResult = tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                chineseVoiceMissing = languageResult == TextToSpeech.LANG_MISSING_DATA ||
                    languageResult == TextToSpeech.LANG_NOT_SUPPORTED
                if (chineseVoiceMissing) {
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
        // 不再附加 KEY_PARAM_STREAM：设置了 AudioAttributes 后流参数会被忽略，且部分
        // OEM 实现里两者并存会走异常分支。
        val parameters = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f)
        }
        return engine.speak(text, TextToSpeech.QUEUE_FLUSH, parameters, "weather-${System.currentTimeMillis()}") == TextToSpeech.SUCCESS
    }

    fun isChineseVoiceMissing(): Boolean = chineseVoiceMissing

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
