package com.neko.weather.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.neko.weather.model.AppLanguage
import com.neko.weather.ui.LegalLanguageDropdown
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 法律文档页的语言下拉（LegalLanguageDropdown）。
 *
 * 选择依据：参数只有 selected: AppLanguage 与 onSelected 回调，是纯状态输入；
 * 文档正文由 assets 里的 markdown 渲染，属于另一个测试目标，这里只锁标签与按钮形态。
 */
@RunWith(AndroidJUnit4::class)
class LegalLanguageScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun simplifiedChinese() = capture("LegalLanguageDropdown_zh", AppLanguage.SIMPLIFIED_CHINESE)

    @Test
    fun english() = capture("LegalLanguageDropdown_en", AppLanguage.ENGLISH)

    private fun capture(name: String, selected: AppLanguage) {
        composeRule.setContent {
            ScreenshotTheme(dark = false) {
                Box(modifier = Modifier.padding(16.dp)) {
                    LegalLanguageDropdown(selected = selected, onSelected = {})
                }
            }
        }
        composeRule.onRoot().captureRoboImage(ScreenshotBaseline.path(name))
    }
}
