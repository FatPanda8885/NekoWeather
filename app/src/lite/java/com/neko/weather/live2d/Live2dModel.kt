package com.neko.weather.live2d

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

object CubismRuntime {
    const val isAvailable: Boolean = false
    const val displayName: String = "未安装"
}

/** Lite 变体没有 Cubism 渲染器，保留同名信号量以免主源码 TTS 桥接需要分支。 */
object CubismSignals {
    private val SPEAKING = AtomicBoolean(false)

    fun isSpeaking(): Boolean = SPEAKING.get()

    fun setSpeaking(speaking: Boolean) {
        SPEAKING.set(speaking)
    }
}

@Composable
fun Live2dModel(
    modelPath: String,
    previewPath: String,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") interactionEnabled: Boolean = true,
    @Suppress("UNUSED_PARAMETER") forceStatic: Boolean = false,
) {
    val bitmap = remember(previewPath) {
        previewPath.takeIf { it.isNotBlank() && File(it).isFile }
            ?.let(BitmapFactory::decodeFile)
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Live2D 静态预览",
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    }
}
