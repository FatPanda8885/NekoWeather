package com.neko.weather.tts

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.os.Bundle
import android.util.Log
import com.neko.weather.live2d.CubismSignals
import com.neko.weather.model.VoiceSettings
import java.util.Locale

class SystemTtsController(context: Context) {
    private val appContext = context.applicationContext
    private var ready = false

    /** 所有绑定尝试（含默认引擎的 null）都失败时为 true。 */
    private var engineMissing = false

    /** 引擎初始化成功但缺少中文语音数据时为 true，此时播报会静默失败。 */
    private var chineseVoiceMissing = false
    private var pending: Pair<String, VoiceSettings>? = null
    private var tts: TextToSpeech? = null
    private val attemptedEngines = mutableSetOf<String?>()

    /** 引擎侧播报出错时的回调；参数为系统错误码（可空）。可能在 binder 线程调用。 */
    var speakErrorHandler: ((Int?) -> Unit)? = null

    init {
        createEngine(preferredEnginePackage())
    }

    /**
     * 部分系统（如 ColorOS 16 / Android 16）上两参构造器对「默认引擎」的解析会直接
     * 初始化失败，即使系统设置里 TTS 完全可用；显式指定引擎包名绑定更可靠。
     */
    private fun preferredEnginePackage(): String? {
        val raw = Settings.Secure.getString(appContext.contentResolver, "tts_default_synth")
        val fromSettings = raw?.substringBefore('/')?.takeIf(String::isNotBlank)
        if (fromSettings != null) return fromSettings
        return firstInstalledEngine()
    }

    private fun firstInstalledEngine(exclude: String? = null): String? =
        appContext.packageManager.queryIntentServices(Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0)
            ?.asSequence()
            ?.mapNotNull { it.serviceInfo?.packageName }
            ?.filter { it != exclude }
            ?.firstOrNull()

    private fun createEngine(enginePackage: String?) {
        attemptedEngines += enginePackage
        val listener = TextToSpeech.OnInitListener { status -> handleInitResult(status, enginePackage) }
        tts = if (enginePackage != null) {
            TextToSpeech(appContext, listener, enginePackage)
        } else {
            TextToSpeech(appContext, listener)
        }
    }

    private fun handleInitResult(status: Int, enginePackage: String?) {
        if (status == TextToSpeech.SUCCESS) {
            ready = true
            val engine = tts ?: return
            val languageResult = engine.setLanguage(Locale.SIMPLIFIED_CHINESE)
            chineseVoiceMissing = languageResult == TextToSpeech.LANG_MISSING_DATA ||
                languageResult == TextToSpeech.LANG_NOT_SUPPORTED
            if (chineseVoiceMissing) {
                engine.language = Locale.getDefault()
            }
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
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
            return
        }

        // 绑定失败：销毁本次实例，换下一个候选引擎重试（默认解析 → 设置里的默认引擎 →
        // 已安装的其他 TTS 服务），全部失败才判定为「未检测到引擎」
        tts?.shutdown()
        tts = null
        val fallback = sequenceOf(
            preferredEnginePackage(),
            firstInstalledEngine(exclude = enginePackage),
        ).firstOrNull { it !in attemptedEngines }
        if (fallback != null) {
            Log.w("NekoWeather", "TTS init failed for ${enginePackage ?: "default"}; retrying with $fallback")
            createEngine(fallback)
        } else {
            Log.w("NekoWeather", "TTS init failed for ${enginePackage ?: "default"}; no usable engine left")
            engineMissing = true
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
