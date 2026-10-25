package com.neko.weather.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.neko.weather.ui.settings.ActionEmphasis
import com.neko.weather.ui.settings.ChoiceOption
import com.neko.weather.ui.settings.SettingsPanel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 设置页的控件行（SliderRow / ChoiceRow / TextFieldRow / ActionRow）。
 *
 * 选择依据：参数同样全是纯数据（当前值、选项列表、回调），没有 ViewModel 也没有副作用；
 * 这四类行承担了设置页主要的视觉回归风险（滑杆刻度、按钮组换行、输入框错误态）。
 */
@RunWith(AndroidJUnit4::class)
class SettingsControlsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun default() = capture("SettingsControls_default")

    private fun capture(name: String) {
        composeRule.setContent {
            ScreenshotTheme(dark = false) {
                SettingsPanel {
                    Group(title = "动画与刷新") {
                        SliderRow(
                            title = "动画速度",
                            valueText = "1.0x",
                            value = 0.5f,
                            onValueChange = {},
                            valueRange = 0.2f..2.0f,
                        )
                        SeparatorRow()
                        ChoiceRow(
                            label = "布局",
                            options = listOf(ChoiceOption("grid", "网格"), ChoiceOption("list", "列表")),
                            selected = "grid",
                            onSelect = {},
                        )
                        SeparatorRow()
                        TextFieldRow(
                            label = "API Host",
                            value = "https://api.example.com",
                            onValueChange = {},
                            supportingText = "示例状态，不会写入配置",
                        )
                        SeparatorRow()
                        TextFieldRow(
                            label = "API Key",
                            value = "",
                            onValueChange = {},
                            isError = true,
                            supportingText = "必填",
                            password = true,
                        )
                        SeparatorRow()
                        ActionRow(label = "保存", onClick = {}, emphasis = ActionEmphasis.PRIMARY)
                        SeparatorRow()
                        ActionRow(label = "恢复默认", onClick = {}, enabled = false)
                    }
                }
            }
        }
        composeRule.onRoot().captureRoboImage(ScreenshotBaseline.path(name))
    }
}
