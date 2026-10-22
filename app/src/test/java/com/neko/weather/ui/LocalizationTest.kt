package com.neko.weather.ui

import com.neko.weather.model.AppLanguage
import com.neko.weather.data.LauncherLabelManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LocalizationTest {
    @Test
    fun primaryScreensHaveEnglishTranslations() {
        val samples = listOf(
            "设置", "应用与界面", "数据与网络", "提醒", "其他",
            "外观与主题", "显示与更新", "语音与音色", "天气数据供应商", "AI 天气建议",
            "通知提醒", "备份与还原", "关于", "逐小时预报", "未来 15 日", "今日详情",
            "空气质量", "气象灾害预警", "生活建议", "降雨趋势与云图", "和风天气组合设置",
            "实验性功能，可能会造成卡顿", "拖动图片调整裁切区域，双指可缩放和旋转。",
        )
        samples.forEach { source ->
            val translated = localizedText(source, AppLanguage.ENGLISH)
            assertFalse("English translation still contains Han characters: $translated", translated.any { it.code in 0x4E00..0x9FFF })
        }
    }

    @Test
    fun traditionalChineseUsesTraditionalTerms() {
        val translated = localizedText("设置天气与降雨云图", AppLanguage.TRADITIONAL_CHINESE)
        assertTrue(translated.contains("設定"))
        assertTrue(translated.contains("天氣"))
        assertTrue(translated.contains("雲圖"))
    }

    @Test
    fun dynamicEnglishPhrasesDoNotMixLanguages() {
        assertEquals("Air Excellent 35", localizedText("空气 优 35", AppLanguage.ENGLISH))
        assertEquals("Light rain", localizedText("小雨", AppLanguage.ENGLISH))
        assertEquals("Rain gear", localizedText("雨具", AppLanguage.ENGLISH))
        assertEquals("Show next 9 days", localizedText("展开后续 9 天", AppLanguage.ENGLISH))
        listOf(
            "选择语言", "单位与天气更新", "内容供应商配置", "选择城市", "配置通知",
            "特别功能", "协议与隐私", "配置完毕", "欢迎使用猫猫风雨",
            "第三方内容使用说明", "请滚动至文末后继续", "同意并继续", "请完整阅读",
        ).forEach { source ->
            val translated = localizedText(source, AppLanguage.ENGLISH)
            assertFalse("Onboarding translation still contains Han characters: $translated", translated.any { it.code in 0x4E00..0x9FFF })
        }
    }

    @Test
    fun japaneseCoversPrimaryNavigation() {
        assertEquals("設定", localizedText("设置", AppLanguage.JAPANESE))
        assertEquals("1時間ごとの予報", localizedText("逐小时预报", AppLanguage.JAPANESE))
        assertEquals("大気質", localizedText("空气质量", AppLanguage.JAPANESE))
    }

    @Test
    fun markdownLinksKeepBrowserTargets() {
        val markdownLink = inlineMarkdown("[NekoWeather](https://nekoweather.12am.moe)")
        assertEquals(
            "https://nekoweather.12am.moe",
            markdownLink.getStringAnnotations("URL", 0, markdownLink.length).single().item,
        )
        val automaticLink = inlineMarkdown("<https://www.gnu.org/licenses/>")
        assertEquals(
            "https://www.gnu.org/licenses/",
            automaticLink.getStringAnnotations("URL", 0, automaticLink.length).single().item,
        )
    }

    @Test
    fun launcherLabelFollowsAppLanguage() {
        assertEquals("猫猫风雨", LauncherLabelManager.labelFor(AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("猫猫风雨", LauncherLabelManager.labelFor(AppLanguage.TRADITIONAL_CHINESE))
        assertEquals("NekoWeather", LauncherLabelManager.labelFor(AppLanguage.ENGLISH))
        assertEquals("NekoWeather", LauncherLabelManager.labelFor(AppLanguage.JAPANESE))
    }

    @Test
    fun onboardingLanguageFollowsSupportedSystemLocale() {
        assertEquals(AppLanguage.SIMPLIFIED_CHINESE, systemAppLanguage(Locale.forLanguageTag("zh-CN")))
        assertEquals(AppLanguage.TRADITIONAL_CHINESE, systemAppLanguage(Locale.forLanguageTag("zh-Hant-TW")))
        assertEquals(AppLanguage.JAPANESE, systemAppLanguage(Locale.JAPANESE))
        assertEquals(AppLanguage.ENGLISH, systemAppLanguage(Locale.ENGLISH))
        assertEquals(AppLanguage.ENGLISH, systemAppLanguage(Locale.FRENCH))
    }

    @Test
    fun legalDocumentAssetsMatchLanguage() {
        assertEquals("terms_of_use.md", legalDocumentAsset("terms_of_use.md", AppLanguage.SIMPLIFIED_CHINESE))
        assertEquals("terms_of_use_zh-TW.md", legalDocumentAsset("terms_of_use.md", AppLanguage.TRADITIONAL_CHINESE))
        assertEquals("terms_of_use_en.md", legalDocumentAsset("terms_of_use.md", AppLanguage.ENGLISH))
        assertEquals("terms_of_use_ja.md", legalDocumentAsset("terms_of_use.md", AppLanguage.JAPANESE))
        assertEquals("gpl_v3.txt", legalDocumentAsset("gpl_v3.txt", AppLanguage.JAPANESE))
    }
}
