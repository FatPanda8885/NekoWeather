package com.neko.weather.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.neko.weather.model.AppLanguage
import java.util.Locale

internal fun systemAppLanguage(locale: Locale = Locale.getDefault()): AppLanguage = when (locale.language.lowercase()) {
    "zh" -> {
        val traditional = locale.script.equals("Hant", ignoreCase = true) ||
            locale.country.uppercase() in setOf("TW", "HK", "MO")
        if (traditional) AppLanguage.TRADITIONAL_CHINESE else AppLanguage.SIMPLIFIED_CHINESE
    }
    "ja" -> AppLanguage.JAPANESE
    "en" -> AppLanguage.ENGLISH
    else -> AppLanguage.ENGLISH
}

internal fun legalDocumentAsset(baseAsset: String, language: AppLanguage): String {
    if (!baseAsset.endsWith(".md")) return baseAsset
    val stem = baseAsset.removeSuffix(".md")
    return when (language) {
        AppLanguage.SIMPLIFIED_CHINESE -> baseAsset
        AppLanguage.TRADITIONAL_CHINESE -> "${stem}_zh-TW.md"
        AppLanguage.ENGLISH -> "${stem}_en.md"
        AppLanguage.JAPANESE -> "${stem}_ja.md"
    }
}

internal fun legalLanguageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SIMPLIFIED_CHINESE -> "简体中文"
    AppLanguage.TRADITIONAL_CHINESE -> "繁體中文"
    AppLanguage.ENGLISH -> "English"
    AppLanguage.JAPANESE -> "日本語"
}

@Composable
internal fun LegalLanguageDropdown(selected: AppLanguage, onSelected: (AppLanguage) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(legalLanguageLabel(selected)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AppLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text(legalLanguageLabel(language)) },
                    onClick = { onSelected(language); expanded = false },
                )
            }
        }
    }
}
