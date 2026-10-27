package com.neko.weather.screenshot

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.neko.weather.ui.settings.SettingsPanel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 设置页的可复用行（SettingsScope 的 Group / Entry / SwitchRow / NoteRow / SeparatorRow）。
 *
 * 选择依据：这些 composable 的参数全是纯数据（标题字符串、布尔值、回调），
 * 不接收 MainUiState、不接收 MainViewModel、不触发任何网络或存储读取。
 */
@RunWith(AndroidJUnit4::class)
class SettingsRowsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun light() = capture("SettingsRows_light", dark = false)

    @Test
    fun dark() = capture("SettingsRows_dark", dark = true)

    private fun capture(name: String, dark: Boolean) {
        composeRule.setContent {
            ScreenshotTheme(dark) {
                SettingsPanel {
                    Group(title = "显示", description = "示例数据，不读取真实配置") {
                        Entry(
                            icon = Icons.Outlined.Palette,
                            title = "外观与主题",
                            description = "深浅模式、主题色与外观预设",
                            showChevron = true,
                            onClick = {},
                        )
                        SeparatorRow()
                        SwitchRow(
                            title = "跟随系统深浅",
                            description = "按系统设置切换配色",
                            checked = true,
                            onCheckedChange = {},
                        )
                        SeparatorRow()
                        SwitchRow(
                            title = "动态取色",
                            description = "需要 Android 12 及以上",
                            checked = false,
                            onCheckedChange = {},
                            enabled = false,
                        )
                        NoteRow("主页天气界面保持沉浸式深色风格，主题主要作用于设置等界面。")
                    }
                    Group(title = "关于") {
                        Entry(title = "版本", trailingText = "1.1.0")
                        SeparatorRow()
                        Entry(title = "隐私政策", showChevron = true, onClick = {})
                        NoteRow("读取失败：网络不可用（示例错误态）", isError = true)
                    }
                }
            }
        }
        composeRule.onRoot().captureRoboImage(ScreenshotBaseline.path(name))
    }
}
