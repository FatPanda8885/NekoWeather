package com.neko.weather.ui.settings

import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toHct
import com.materialkolor.rememberDynamicColorScheme
import com.neko.weather.model.ThemeMode
import com.neko.weather.model.ThemeSettings
import com.neko.weather.model.themeColorOptions
import com.neko.weather.ui.MainUiState
import com.neko.weather.ui.MainViewModel
import com.neko.weather.ui.Text

/**
 * 主题色彩二级页：深浅模式预览卡 + 动态取色 + 主题色色板（参照 animeko 主题与色彩），
 * 并承载「外观预设」入口。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ThemeColorPage(
    state: MainUiState,
    viewModel: MainViewModel,
    onOpenPresets: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = state.themeSettings
    SettingsPanel(modifier) {
        Group(title = "主题模式") {
            ThemeModeSelectPanel(
                currentMode = theme.mode,
                onModeSelected = { mode -> viewModel.updateThemeSettings(theme.copy(mode = mode)) },
                seedColor = theme.seedColor,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
            NoteRow("主页天气界面保持沉浸式深色风格，主题主要作用于设置等界面。")
        }
        Group(title = "主题色") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchRow(
                    title = "动态取色",
                    description = "跟随系统壁纸自动配色（Material You）",
                    checked = theme.useDynamicColor,
                    onCheckedChange = { checked -> viewModel.updateThemeSettings(theme.copy(useDynamicColor = checked)) },
                )
            } else {
                NoteRow("此设备不支持动态取色（需要 Android 12 及以上），可在下方选择固定主题色。")
            }
            Box(Modifier.alpha(if (theme.useDynamicColor) 0.5f else 1f)) {
                ThemePalette(
                    selectedColor = theme.seedColor.takeUnless { theme.useDynamicColor },
                    onSelect = { color ->
                        viewModel.updateThemeSettings(
                            ThemeSettings(mode = theme.mode, useDynamicColor = false, seedColorValue = color.toArgb().toLong()),
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                )
            }
        }
        Group(title = "外观预设", description = "把字体、Logo、背景与按天气背景打包成预设，一键切换或分享。") {
            Entry(
                icon = Icons.Outlined.Palette,
                title = "外观预设",
                trailingText = state.appearancePresets.selected?.name ?: "未使用",
                showChevron = true,
                onClick = onOpenPresets,
            )
        }
    }
}

/** 主题模式三选：浅色 / 深色 / 跟随系统（对角拼接预览），横向滚动。 */
@Composable
private fun ThemeModeSelectPanel(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    seedColor: Color,
    modifier: Modifier = Modifier,
) {
    val panelModifier = Modifier.size(96.dp, 140.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
    ) {
        ThemeMode.entries.forEach { mode ->
            val selected = currentMode == mode
            Column(
                modifier = Modifier.selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = { onModeSelected(mode) },
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (mode == ThemeMode.AUTO) {
                    DiagonalMixedPreviewPanel(seedColor, panelModifier)
                } else {
                    ThemePreviewPanel(
                        colorScheme = previewColorScheme(seedColor, isDark = mode == ThemeMode.DARK),
                        modifier = panelModifier,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected, onClick = null)
                    Spacer(Modifier.width(4.dp))
                    Text(modeLabel(mode), fontSize = 13.sp)
                }
            }
        }
    }
}

private fun modeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
    ThemeMode.AUTO -> "跟随系统"
}

/** 按种子色生成预览配色。 */
@Composable
private fun previewColorScheme(seedColor: Color, isDark: Boolean): ColorScheme =
    rememberDynamicColorScheme(seedColor = seedColor, isDark = isDark, isAmoled = false)

/** 迷你主题预览：顶栏 + 卡片 + 按钮/文本条，直观呈现配色差异。 */
@Composable
private fun ThemePreviewPanel(
    colorScheme: ColorScheme,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.surfaceVariant)
            .padding(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colorScheme.primary),
        )
        Spacer(Modifier.height(6.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(colorScheme.surface)
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.72f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colorScheme.onSurfaceVariant.copy(alpha = 0.7f)),
            )
            Box(
                Modifier
                    .fillMaxWidth(0.5f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(colorScheme.secondaryContainer))
                Box(
                    Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(colorScheme.primaryContainer),
                )
            }
        }
    }
}

/** 跟随系统：浅色（左上）与深色（右下）对角拼接。 */
@Composable
private fun DiagonalMixedPreviewPanel(seedColor: Color, modifier: Modifier = Modifier) {
    Box(modifier) {
        ThemePreviewPanel(
            colorScheme = previewColorScheme(seedColor, isDark = false),
            modifier = Modifier.fillMaxSize().clip(TopLeftDiagonalShape),
        )
        ThemePreviewPanel(
            colorScheme = previewColorScheme(seedColor, isDark = true),
            modifier = Modifier.fillMaxSize().clip(BottomRightDiagonalShape),
        )
    }
}

private val TopLeftDiagonalShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Generic(
            Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(0f, size.height)
                close()
            },
        )
}

private val BottomRightDiagonalShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Generic(
            Path().apply {
                moveTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            },
        )
}

/** 主题色色板：FlowRow 圆形色块，选中显示动画对勾。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemePalette(
    selectedColor: Color?,
    onSelect: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier.selectableGroup(),
        horizontalArrangement = Arrangement.Center,
    ) {
        themeColorOptions().forEach { color ->
            PaletteColorButton(
                onClick = { onSelect(color) },
                baseColor = color,
                selected = selectedColor == color,
                modifier = Modifier.padding(4.dp),
            )
        }
    }
}

@Composable
private fun PaletteColorButton(
    onClick: () -> Unit,
    baseColor: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerSize by animateDpAsState(targetValue = if (selected) 28.dp else 0.dp, label = "paletteCheckSize")
    val iconSize by animateDpAsState(targetValue = if (selected) 16.dp else 0.dp, label = "paletteCheckIcon")
    Surface(
        modifier = modifier
            .sizeIn(maxHeight = 72.dp, maxWidth = 72.dp, minHeight = 64.dp, minWidth = 64.dp)
            .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val hct = baseColor.toHct()
            val tone80 = Color(Hct.from(hct.hue, 40.0, 80.0).toInt())
            val tone90 = Color(Hct.from(hct.hue, 40.0, 90.0).toInt())
            val tone60 = Color(Hct.from(hct.hue, 40.0, 60.0).toInt())
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(tone80),
            ) {
                Box(Modifier.align(Alignment.BottomStart).size(22.dp).background(tone90))
                Box(Modifier.align(Alignment.BottomEnd).size(22.dp).background(tone60))
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(containerSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(iconSize),
                    )
                }
            }
        }
    }
}
