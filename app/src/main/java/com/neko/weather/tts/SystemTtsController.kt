package com.neko.weather.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.os.Bundle
import com.neko.weather.live2d.CubismSignals
import com.neko.weather.model.VoiceSettings
import java.util.Locale

class SystemTtsController(context: Context) {
    private var ready = false

    /** 设备上没有任何可用 TTS 引擎（init 回调失败）时为 true。 */
    private var engineMissing = false

    /** 引擎初始化成功但缺少中文语音数据时为 true，此时播报会静默失败。 */
    private var chineseVoiceMissing = false
    private var pending: Pair<String, VoiceSettings>? = null
    private var tts: TextToSpeech? = null

    /** 引擎侧播报出错时的回调；参数为系统错误码（可空）。可能在 binder 线程调用。 */
    var speakErrorHandler: ((Int?) -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status != TextToSpeech.SUCCESS) {
                engineMissing = true
                pending = null
                return@TextToSpeech
            }
            ready = true
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
                override fun onError(utteranceId: String?) {
                    CubismSignals.setSpeaking(false)
                    speakErrorHandler?.invoke(null)
                }
                override fun onError(utteranceId: String?, errorCode: Int) {
                    CubismSignals.setSpeaking(false)
                    speakErrorHandler?.invoke(errorCode)
                }
                override fun onStop(utteranceId: String?, interrupted: Boolean) = CubismSignals.setSpeaking(false)
            })
            pending?.let { (text, settings) -> speak(text, settings) }
            pending = null
        }
    }

    fun speak(text: String, settings: VoiceSettings): Boolean {
        if (engineMissing) return false
        if (!ready) {
            pending = text to settings
            return true
        }
        if (chineseVoiceMissing) return false
        val engine = tts ?: return false
        if (engine.setSpeechRate(settings.rate) == TextToSpeech.ERROR) return false
        if (engine.setPitch(settings.pitch) == TextToSpeech.ERROR) return false
        // 传统流参数配置：不设置 AudioAttributes（个别 OEM 引擎对属性路由的实现有缺陷，
        // 会把「播放成功」的语音路由到静音通道），STREAM_MUSIC 是兼容面最广的组合。
        val parameters = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f)
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_MUSIC)
        }
        return engine.speak(text, TextToSpeech.QUEUE_FLUSH, parameters, "weather-${System.currentTimeMillis()}") == TextToSpeech.SUCCESS
    }

    fun isEngineMissing(): Boolean = engineMissing

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
