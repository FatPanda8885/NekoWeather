package com.neko.weather.ui.settings

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neko.weather.ui.Text

/** Marks the settings component DSL so nested scopes stay unambiguous. */
@DslMarker
annotation class SettingsDsl

/** Visual weight of an [com.neko.weather.ui.settings.SettingsScope.ActionRow] button. */
enum class ActionEmphasis { PRIMARY, OUTLINED, TEXT }

/** One selectable choice of a [com.neko.weather.ui.settings.SettingsScope.ChoiceRow]. */
data class ChoiceOption(val key: String, val label: String)

private class RowSlotCounter {
    private var claimed = 0

    fun claim(): Int = claimed++
}

private val LocalRowSlotCounter = compositionLocalOf<RowSlotCounter?> { null }

@Composable
private fun SettingsRowFrame(content: @Composable () -> Unit) {
    val counter = LocalRowSlotCounter.current
    val slot = remember { counter?.claim() ?: 0 }
    Column(modifier = Modifier.fillMaxWidth()) {
        if (slot > 0) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            )
        }
        content()
    }
}

/** Circled info icon that reveals [text] in a popup anchored next to it. */
@Composable
fun InfoHint(text: String, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme
    Box(modifier) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable { expanded = !expanded },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "说明",
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                text = text,
                fontSize = 13.sp,
                color = colorScheme.onSurface,
                modifier = Modifier.widthIn(max = 260.dp).padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

/** Row factory for one settings page section. */
@SettingsDsl
class SettingsScope internal constructor() {

    /** Renders a section title with its rounded container and automatic row separators. */
    @Composable
    fun Group(title: String, description: String? = null, content: @Composable ColumnScope.() -> Unit) {
        val colorScheme = MaterialTheme.colorScheme
        val counter = remember { RowSlotCounter() }
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.primary,
                )
                description?.let { InfoHint(it) }
            }
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.surfaceVariant.copy(alpha = 0.45f),
                ),
            ) {
                CompositionLocalProvider(LocalRowSlotCounter provides counter) {
                    Column(modifier = Modifier.fillMaxWidth(), content = content)
                }
            }
        }
    }

    /** Renders a list-style navigation or info row. */
    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    @Composable
    fun Entry(
        icon: ImageVector? = null,
        title: String,
        description: String? = null,
        trailingText: String? = null,
        trailing: (@Composable () -> Unit)? = null,
        showChevron: Boolean = false,
        onClick: (() -> Unit)? = null,
        onLongClick: (() -> Unit)? = null,
        hint: String? = null,
    ) {
        val colorScheme = MaterialTheme.colorScheme
        SettingsRowFrame {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        when {
                            onClick != null && onLongClick != null -> Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                            onClick != null -> Modifier.clickable(onClick = onClick)
                            else -> Modifier
                        },
                    )
                    .defaultMinSize(minHeight = 56.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    if (description != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = description,
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                }
                hint?.let { InfoHint(it) }
                if (trailingText != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = trailingText,
                        fontSize = 13.sp,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    trailing()
                }
                if (showChevron) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }

    /** Renders a tappable row with a trailing switch. */
    @Composable
    fun SwitchRow(
        title: String,
        description: String? = null,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        enabled: Boolean = true,
        hint: String? = null,
    ) {
        val colorScheme = MaterialTheme.colorScheme
        SettingsRowFrame {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (enabled) Modifier.clickable { onCheckedChange(!checked) } else Modifier)
                    .defaultMinSize(minHeight = 56.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    if (description != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = description,
                            fontSize = 12.sp,
                            lineHeight = 14.sp,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                }
                hint?.let { InfoHint(it) }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    enabled = enabled,
                )
            }
        }
    }

    /** Renders a titled slider showing its current value on the right. */
    @Composable
    fun SliderRow(
        title: String,
        valueText: String,
        value: Float,
        onValueChange: (Float) -> Unit,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int = 0,
        onValueChangeFinished: () -> Unit = {},
        enabled: Boolean = true,
    ) {
        val colorScheme = MaterialTheme.colorScheme
        SettingsRowFrame {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = title, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text(
                        text = valueText,
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    valueRange = valueRange,
                    steps = steps,
                    onValueChangeFinished = onValueChangeFinished,
                )
            }
        }
    }

    /** Renders a labeled set of mutually exclusive option buttons. */
    @Composable
    fun ChoiceRow(
        label: String,
        options: List<ChoiceOption>,
        selected: String,
        onSelect: (String) -> Unit,
        columns: Int = options.size,
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val perRow = columns.coerceAtLeast(1)
        SettingsRowFrame {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(text = label, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    options.chunked(perRow).forEach { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            chunk.forEach { option ->
                                OutlinedButton(
                                    onClick = { onSelect(option.key) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                    colors = if (option.key == selected) {
                                        ButtonDefaults.outlinedButtonColors(
                                            containerColor = colorScheme.primaryContainer,
                                        )
                                    } else {
                                        ButtonDefaults.outlinedButtonColors()
                                    },
                                ) {
                                    Text(
                                        text = option.label,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                            repeat(perRow - chunk.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    /** Renders a labeled text input row. */
    @Composable
    fun TextFieldRow(
        label: String,
        value: String,
        onValueChange: (String) -> Unit,
        supportingText: String? = null,
        isError: Boolean = false,
        singleLine: Boolean = true,
        minLines: Int = 1,
        monospace: Boolean = false,
        password: Boolean = false,
        hint: String? = null,
    ) {
        val textStyle = if (monospace) {
            MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
        } else {
            LocalTextStyle.current
        }
        val support: (@Composable () -> Unit)? = if (supportingText == null) {
            null
        } else {
            { Text(text = supportingText) }
        }
        SettingsRowFrame {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                textStyle = textStyle,
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = label)
                        hint?.let { InfoHint(it) }
                    }
                },
                supportingText = support,
                isError = isError,
                visualTransformation = if (password) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                singleLine = singleLine,
                minLines = minLines,
            )
        }
    }

    /** Renders a full-width action button row. */
    @Composable
    fun ActionRow(
        label: String,
        onClick: () -> Unit,
        enabled: Boolean = true,
        emphasis: ActionEmphasis = ActionEmphasis.OUTLINED,
        hint: String? = null,
    ) {
        SettingsRowFrame {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val buttonModifier = Modifier.weight(1f)
                when (emphasis) {
                    ActionEmphasis.PRIMARY -> Button(
                        onClick = onClick,
                        modifier = buttonModifier,
                        enabled = enabled,
                    ) {
                        Text(text = label)
                    }

                    ActionEmphasis.OUTLINED -> OutlinedButton(
                        onClick = onClick,
                        modifier = buttonModifier,
                        enabled = enabled,
                    ) {
                        Text(text = label)
                    }

                    ActionEmphasis.TEXT -> TextButton(
                        onClick = onClick,
                        modifier = buttonModifier,
                        enabled = enabled,
                    ) {
                        Text(text = label)
                    }
                }
                hint?.let { InfoHint(it) }
            }
        }
    }

    /** Renders a borderless explanatory note. */
    @Composable
    fun NoteRow(text: String, isError: Boolean = false) {
        val colorScheme = MaterialTheme.colorScheme
        SettingsRowFrame {
            Text(
                text = text,
                fontSize = 12.sp,
                color = if (isError) colorScheme.error else colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    /** Renders a section label without a container; [hint] hides behind an info icon. */
    @Composable
    fun SectionLabel(title: String, hint: String? = null) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            hint?.let { InfoHint(it) }
        }
    }

    /** Renders a single inset divider for use outside a [Group]. */
    @Composable
    fun SeparatorRow() {
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        )
    }
}

/** Scrollable settings page container that exposes the settings component DSL. */
@Composable
fun SettingsPanel(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable SettingsScope.() -> Unit,
) {
    val scope = remember { SettingsScope() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        scope.content()
    }
}
