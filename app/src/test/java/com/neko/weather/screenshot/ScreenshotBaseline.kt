package com.neko.weather.screenshot

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 截图基线路径。
 *
 * 只有 recordRoborazzi* / verifyRoborazzi* 任务（会把 roborazzi.test.* 转发进测试 JVM）才读写
 * 入库基线 app/src/test/screenshots/；普通的 testDebugUnitTest 只写到 build/ 下，
 * 否则一次普通单测运行就会就地生成"基线"，让随后的回归比较失去意义。
 */
internal object ScreenshotBaseline {
    private const val BASELINE_DIR = "src/test/screenshots"
    private const val SCRATCH_DIR = "build/outputs/roborazzi-scratch"

    private fun flag(name: String): Boolean = System.getProperty(name) == "true"

    fun path(name: String): String {
        val dir = if (flag("roborazzi.test.record") || flag("roborazzi.test.verify")) {
            BASELINE_DIR
        } else {
            SCRATCH_DIR
        }
        return "$dir/$name.png"
    }
}

/**
 * 固定使用 Material3 默认配色，不读 ConfigStore / ViewModel / 网络，
 * 保证同一份代码在任意机器上渲染出的像素一致。
 */
@Composable
internal fun ScreenshotTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            content()
        }
    }
}
