package com.neko.weather.live2d

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File

object CubismRuntime {
    const val isAvailable: Boolean = true
    const val displayName: String = "Cubism SDK for Java 5-r.5"
}

@Composable
fun Live2dModel(
    modelPath: String,
    previewPath: String,
    modifier: Modifier = Modifier,
    interactionEnabled: Boolean = true,
    forceStatic: Boolean = false,
) {
    val validModel = remember(modelPath) {
        modelPath.takeIf { it.isNotBlank() && File(it).isFile }
    }
    if (validModel != null && !forceStatic) {
        key(validModel, interactionEnabled) {
            AndroidView(
                factory = { context -> CubismSurfaceView(context, validModel, interactionEnabled) {} },
                modifier = modifier,
                onRelease = { it.release() },
            )
        }
    } else {
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
}
