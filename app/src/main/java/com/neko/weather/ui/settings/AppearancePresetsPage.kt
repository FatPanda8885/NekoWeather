package com.neko.weather.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.neko.weather.model.AppearancePreset
import com.neko.weather.ui.MainUiState
import com.neko.weather.ui.MainViewModel
import com.neko.weather.ui.PendingImageEdit
import com.neko.weather.ui.ImageCropDialog
import com.neko.weather.ui.Text

/** 外观预设列表：点击一键切换，长按多选管理，支持导入导出。 */
@Composable
internal fun AppearancePresetsPage(
    state: MainUiState,
    viewModel: MainViewModel,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    onToggleSelection: (String) -> Unit,
    onStartSelection: (String) -> Unit,
    onExitSelection: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val presets = state.appearancePresets
    var renameTargetId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var confirmingDelete by remember { mutableStateOf(false) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importAppearancePreset)
    }
    SettingsPanel(modifier) {
        if (selectionMode) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "已选 ${selectedIds.size} 项",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        val target = selectedIds.singleOrNull()
                        renameTargetId = target
                        renameText = presets.presets.firstOrNull { it.id == target }?.name.orEmpty()
                    },
                    enabled = selectedIds.size == 1,
                ) {
                    Text("重命名")
                }
                TextButton(onClick = { confirmingDelete = true }, enabled = selectedIds.isNotEmpty()) {
                    Text("删除")
                }
                TextButton(onClick = onExitSelection) {
                    Text("取消")
                }
            }
        }
        Group(title = "预设") {
            if (presets.presets.isEmpty()) {
                NoteRow("尚未保存预设。点击右上角 + 可把当前使用的字体、Logo 与背景保存为预设；也可从 ZIP 导入。")
            }
            presets.presets.forEach { preset ->
                Entry(
                    title = preset.name,
                    description = preset.summary(),
                    trailingText = if (preset.id == presets.selectedId) "使用中" else null,
                    trailing = {
                        if (selectionMode) {
                            Checkbox(checked = preset.id in selectedIds, onCheckedChange = { onToggleSelection(preset.id) })
                        } else {
                            IconButton(onClick = { onEdit(preset.id) }) {
                                Icon(Icons.Outlined.Edit, "编辑预设")
                            }
                        }
                    },
                    onClick = { if (selectionMode) onToggleSelection(preset.id) else viewModel.applyAppearancePreset(preset.id) },
                    onLongClick = { onStartSelection(preset.id) },
                )
            }
        }
        Group(title = "导入与导出") {
            ActionRow(
                label = "从 ZIP 导入预设",
                onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                hint = "ZIP 内需包含 manifest.json 与资源文件，可来自其他设备导出的预设。",
            )
            NoteRow("导出入口在各预设的编辑页内。")
        }
    }
    renameTargetId?.let { id ->
        AlertDialog(
            onDismissRequest = { renameTargetId = null },
            title = { Text("重命名预设") },
            text = {
                PresetNameField(renameText) { renameText = it }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameAppearancePreset(id, renameText)
                    renameTargetId = null
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { renameTargetId = null }) { Text("取消") } },
        )
    }
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("删除预设") },
            text = { Text("确定删除所选预设？若删除的是正在使用的预设，外观将恢复默认。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAppearancePresets(selectedIds)
                    confirmingDelete = false
                    onExitSelection()
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("取消") } },
        )
    }
}

/** 单个预设编辑：名称、四类皮肤资源、应用 / 导出 / 删除。 */
@Composable
internal fun AppearancePresetEditorPage(
    state: MainUiState,
    viewModel: MainViewModel,
    editingId: String?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preset = state.appearancePresets.presets.firstOrNull { it.id == editingId }
    var pendingImageEdit by remember { mutableStateOf<PendingImageEdit?>(null) }
    var pendingWeatherBackground by remember { mutableStateOf("weather_clear") }
    var confirmingDelete by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { viewModel.exportAppearancePreset(editingId.orEmpty(), it) }
    }
    if (preset == null) {
        SettingsPanel(modifier) { NoteRow("预设不存在或已被删除。") }
        return
    }
    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importPresetResource(preset.id, it, "font") }
    }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { pendingImageEdit = PendingImageEdit(it, "logo", "裁切主页 Logo") }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { pendingImageEdit = PendingImageEdit(it, "background", "裁切通用背景") }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importPresetResource(preset.id, it, "video") }
    }
    val weatherBackgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { pendingImageEdit = PendingImageEdit(it, pendingWeatherBackground, "裁切天气背景") }
    }
    val isActive = preset.id == state.appearancePresets.selectedId
    SettingsPanel(modifier) {
        Group(title = "名称") {
            TextFieldRow(
                label = "预设名称",
                value = preset.name,
                onValueChange = { viewModel.renameAppearancePreset(preset.id, it) },
                singleLine = true,
            )
        }
        Group(
            title = "皮肤资源",
            description = "资源保存在应用私有目录，可分别替换字体、Logo 和主界面背景。视频背景会静音循环播放。",
        ) {
            Entry(
                title = if (preset.fontPath.isBlank()) "导入字体" else "更换字体",
                trailingText = if (preset.fontPath.isNotBlank()) "已导入" else null,
                trailing = {
                    IconButton(onClick = { viewModel.resetPresetResource(preset.id, "font") }, enabled = preset.fontPath.isNotBlank()) {
                        Icon(Icons.Default.Refresh, "恢复默认")
                    }
                },
                onClick = { fontPicker.launch(arrayOf("font/ttf", "font/otf", "application/octet-stream")) },
            )
            Entry(
                title = if (preset.logoPath.isBlank()) "导入主页 Logo" else "更换主页 Logo",
                trailingText = if (preset.logoPath.isNotBlank()) "已导入" else null,
                trailing = {
                    IconButton(onClick = { viewModel.resetPresetResource(preset.id, "logo") }, enabled = preset.logoPath.isNotBlank()) {
                        Icon(Icons.Default.Refresh, "恢复默认")
                    }
                },
                onClick = { logoPicker.launch(arrayOf("image/*")) },
            )
            Entry(
                title = if (preset.backgroundPath.isNotBlank() && !preset.backgroundIsVideo) "更换静态背景" else "选择静态背景",
                trailingText = if (preset.backgroundPath.isNotBlank() && !preset.backgroundIsVideo) "已导入" else null,
                trailing = {
                    IconButton(
                        onClick = { viewModel.resetPresetResource(preset.id, "background") },
                        enabled = preset.backgroundPath.isNotBlank() && !preset.backgroundIsVideo,
                    ) {
                        Icon(Icons.Default.Refresh, "恢复默认")
                    }
                },
                onClick = { imagePicker.launch(arrayOf("image/*")) },
            )
            Entry(
                title = if (preset.backgroundPath.isNotBlank() && preset.backgroundIsVideo) "更换视频动态背景" else "选择视频动态背景",
                trailingText = if (preset.backgroundPath.isNotBlank() && preset.backgroundIsVideo) "已导入" else null,
                trailing = {
                    IconButton(
                        onClick = { viewModel.resetPresetResource(preset.id, "video") },
                        enabled = preset.backgroundPath.isNotBlank() && preset.backgroundIsVideo,
                    ) {
                        Icon(Icons.Default.Refresh, "恢复默认")
                    }
                },
                onClick = { videoPicker.launch(arrayOf("video/mp4", "video/webm")) },
            )
        }
        Group("按天气覆盖背景") {
            listOf("clear" to "晴天", "cloudy" to "多云/阴天", "rain" to "雨天", "snow" to "雪天", "fog" to "雾天", "thunder" to "雷暴").forEach { (key, label) ->
                val hasCustomBackground = !preset.weatherBackgrounds[key].isNullOrBlank()
                Entry(
                    title = label,
                    trailingText = if (hasCustomBackground) "已设置" else null,
                    trailing = {
                        IconButton(onClick = { viewModel.resetPresetResource(preset.id, "weather_$key") }, enabled = hasCustomBackground) {
                            Icon(Icons.Default.Refresh, "恢复默认")
                        }
                    },
                    onClick = {
                        pendingWeatherBackground = "weather_$key"
                        weatherBackgroundPicker.launch(arrayOf("image/*"))
                    },
                )
            }
        }
        Group("操作") {
            if (!isActive) {
                ActionRow(
                    label = "应用此预设",
                    onClick = { viewModel.applyAppearancePreset(preset.id) },
                    emphasis = ActionEmphasis.PRIMARY,
                )
            } else {
                NoteRow("该预设正在使用，编辑资源后立即生效。")
            }
            ActionRow(
                label = "导出为 ZIP",
                onClick = { exportLauncher.launch("${preset.name}.zip") },
                hint = "包含 manifest.json 与全部资源文件，可在其他设备导入。",
            )
            ActionRow(
                label = "删除预设",
                onClick = { confirmingDelete = true },
                emphasis = ActionEmphasis.TEXT,
            )
        }
    }
    pendingImageEdit?.let { pending ->
        ImageCropDialog(
            pending = pending,
            onDismiss = { pendingImageEdit = null },
            onConfirm = { bitmap ->
                viewModel.importPresetEditedImage(bitmap, preset.id, pending.kind)
                pendingImageEdit = null
            },
        )
    }
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("删除预设") },
            text = { Text("确定删除该预设？该操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAppearancePresets(setOf(preset.id))
                    confirmingDelete = false
                    onDone()
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun PresetNameField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text("名称") },
    )
}

