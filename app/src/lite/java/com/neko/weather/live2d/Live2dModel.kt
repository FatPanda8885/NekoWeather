package com.neko.weather.live2d

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File

object CubismRuntime {
    const val isAvailable: Boolean = false
    const val displayName: String = "未安装"
}

@Composable
fun Live2dModel(
    modelPath: String,
    previewPath: String,
    modifier: Modifier = Modifier,
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
