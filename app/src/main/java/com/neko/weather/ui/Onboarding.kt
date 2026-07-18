package com.neko.weather.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neko.weather.R
import com.neko.weather.model.AppLanguage
import com.neko.weather.model.CityLocation
import com.neko.weather.model.HOT_CITIES
import com.neko.weather.model.NotificationSettings
import com.neko.weather.model.TemperatureUnit
import com.neko.weather.model.WeatherRefreshSettings
import com.neko.weather.model.WindSpeedUnit
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

data class OnboardingResult(
    val language: AppLanguage,
    val temperatureUnit: TemperatureUnit,
    val windSpeedUnit: WindSpeedUnit,
    val refreshSettings: WeatherRefreshSettings,
    val qWeatherHost: String,
    val qWeatherKey: String,
    val selectedCity: CityLocation?,
    val useLocation: Boolean,
    val notificationSettings: NotificationSettings,
)

@Composable
fun OnboardingScreen(state: MainUiState, viewModel: MainViewModel, onFinished: (OnboardingResult) -> Unit) {
    val context = LocalContext.current
    var requestedPage by remember { mutableIntStateOf(0) }
    var visiblePage by remember { mutableIntStateOf(0) }
    var transitionDirection by remember { mutableIntStateOf(1) }
    val pageOpacity = remember { Animatable(1f) }
    val exitProgress = remember { Animatable(0f) }
    var pendingFinish by remember { mutableStateOf<OnboardingResult?>(null) }
    var language by remember { mutableStateOf(systemAppLanguage()) }
    var temperatureUnit by remember { mutableStateOf(TemperatureUnit.CELSIUS) }
    var windSpeedUnit by remember { mutableStateOf(WindSpeedUnit.KILOMETRES_PER_HOUR) }
    var autoRefresh by remember { mutableStateOf(true) }
    var refreshMinutes by remember { mutableIntStateOf(30) }
    var freshnessGuard by remember { mutableStateOf(true) }
    var qWeatherHost by remember { mutableStateOf("devapi.qweather.com") }
    var qWeatherKey by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf<CityLocation?>(null) }
    var useLocation by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf(NotificationSettings()) }
    var agreementsRead by remember { mutableStateOf(setOf<Int>()) }
    var locationPermissionRequested by remember { mutableStateOf(false) }
    var notificationPermissionRequested by remember { mutableStateOf(false) }
    val agreementDescriptors = remember {
        mapOf(
            6 to ("用户使用协议" to "terms_of_use.md"),
            7 to ("隐私政策" to "privacy_policy.md"),
            8 to ("第三方内容使用说明" to "third_party_notice.md"),
            9 to ("GNU GPLv3 开源协议" to "gpl_v3.txt"),
        )
    }
    val agreementDocuments = remember(context) {
        agreementDescriptors.values.flatMap { (_, baseAsset) ->
            if (baseAsset.endsWith(".md")) AppLanguage.entries.map { legalDocumentAsset(baseAsset, it) }
            else listOf(baseAsset)
        }.distinct().associateWith { assetName ->
            runCatching { context.assets.open(assetName).bufferedReader().use { it.readText() } }
                .getOrElse { "文件读取失败：${it.message}" }
        }
    }

    BackHandler(enabled = visiblePage > 0 && pendingFinish == null) { requestedPage = visiblePage - 1 }

    LaunchedEffect(requestedPage) {
        if (requestedPage == visiblePage) return@LaunchedEffect
        transitionDirection = if (requestedPage > visiblePage) 1 else -1
        pageOpacity.animateTo(0f, tween(75, easing = LinearEasing))
        visiblePage = requestedPage
        pageOpacity.snapTo(0f)
        pageOpacity.animateTo(1f, tween(155, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(pendingFinish) {
        val result = pendingFinish ?: return@LaunchedEffect
        exitProgress.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
        onFinished(result)
    }

    val locateWithoutSaving = {
        viewModel.locateForOnboarding { city ->
            selectedCity = city
            useLocation = true
        }
    }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        locationPermissionRequested = true
        useLocation = granted
        if (granted) locateWithoutSaving()
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationPermissionRequested = true
    }

    CompositionLocalProvider(LocalAppLanguage provides language) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                alpha = 1f - exitProgress.value
                scaleX = 1f + exitProgress.value * .025f
                scaleY = 1f + exitProgress.value * .025f
            },
        ) {
            Image(painterResource(R.drawable.neko1), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .62f)))
            Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 22.dp, vertical = 14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${visiblePage + 1} / 11", color = Color.White.copy(alpha = .72f), fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    CopyrightBadge()
                }
                Spacer(Modifier.height(18.dp))
                Box(
                    Modifier.weight(1f).fillMaxWidth().graphicsLayer {
                        alpha = pageOpacity.value
                        translationX = transitionDirection * (1f - pageOpacity.value) * 22.dp.toPx()
                    },
                ) {
                    key(visiblePage) { when (visiblePage) {
                        0 -> LanguageStep(language) { language = it }
                        1 -> UnitsStep(temperatureUnit, windSpeedUnit, autoRefresh, refreshMinutes, freshnessGuard,
                            { temperatureUnit = it }, { windSpeedUnit = it }, { autoRefresh = it }, { refreshMinutes = it }, { freshnessGuard = it })
                        2 -> ProviderStep(qWeatherHost, qWeatherKey, { qWeatherHost = it }, { qWeatherKey = it })
                        3 -> CityStep(state, selectedCity, useLocation, locationPermissionRequested, {
                            selectedCity = it; useLocation = false
                        }, onSearch = viewModel::searchCities) {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_COARSE_LOCATION,
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) locateWithoutSaving() else locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                        4 -> NotificationStep(notifications, notificationPermissionRequested, { notifications = it }) {
                            if (Build.VERSION.SDK_INT >= 33) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else notificationPermissionRequested = true
                        }
                        5 -> FeatureStep()
                        in 6..9 -> agreementDescriptors.getValue(visiblePage).let { (title, baseAsset) ->
                            AgreementDocumentStep(title, baseAsset, language, agreementDocuments) { agreementsRead += visiblePage }
                        }
                        else -> WelcomeStep()
                    } }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (visiblePage > 0 && visiblePage < 10) OutlinedButton({ requestedPage = visiblePage - 1 }, Modifier.weight(1f), enabled = requestedPage == visiblePage, contentPadding = ButtonDefaults.ContentPadding) { Text("上一步", maxLines = 1, softWrap = false) }
                    if (visiblePage in 6..9) {
                        val read = visiblePage in agreementsRead
                        Button(
                            onClick = { if (read) requestedPage = visiblePage + 1 },
                            enabled = read && requestedPage == visiblePage,
                            modifier = Modifier.weight(1.6f),
                            contentPadding = ButtonDefaults.ContentPadding,
                        ) { Text(if (read) "同意并继续" else "请完整阅读", maxLines = 1, softWrap = false) }
                    } else if (visiblePage < 10) {
                        if (visiblePage == 2) OutlinedButton(
                            { qWeatherKey = ""; requestedPage = visiblePage + 1 }, Modifier.weight(1f), enabled = requestedPage == visiblePage, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
                        ) { Text("暂时跳过", maxLines = 1, softWrap = false, fontSize = 12.sp) }
                        Button(
                            onClick = {
                                if (visiblePage == 4 && Build.VERSION.SDK_INT >= 33 && !notificationPermissionRequested) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                requestedPage = visiblePage + 1
                            },
                            enabled = requestedPage == visiblePage,
                            modifier = Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                        ) { Text("下一步", maxLines = 1, softWrap = false) }
                    } else {
                        Button({
                            pendingFinish = OnboardingResult(
                                language, temperatureUnit, windSpeedUnit,
                                WeatherRefreshSettings(autoRefresh, refreshMinutes, freshnessGuard, 5),
                                qWeatherHost, qWeatherKey, selectedCity, useLocation, notifications,
                            )
                        }, Modifier.fillMaxWidth(), enabled = pendingFinish == null) { Text("开始使用") }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepLayout(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Color.White.copy(alpha = .78f), fontSize = 14.sp)
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable private fun LanguageStep(selected: AppLanguage, onSelect: (AppLanguage) -> Unit) =
    StepLayout("选择语言", "Choose your language · 言語を選択") {
        listOf(
            AppLanguage.SIMPLIFIED_CHINESE to "简体中文",
            AppLanguage.TRADITIONAL_CHINESE to "繁體中文",
            AppLanguage.ENGLISH to "English",
            AppLanguage.JAPANESE to "日本語",
        ).forEach { (value, label) -> ChoiceButton(label, selected == value) { onSelect(value) } }
    }

@Composable
private fun UnitsStep(
    temperature: TemperatureUnit, wind: WindSpeedUnit, enabled: Boolean, minutes: Int, guard: Boolean,
    onTemperature: (TemperatureUnit) -> Unit, onWind: (WindSpeedUnit) -> Unit,
    onEnabled: (Boolean) -> Unit, onMinutes: (Int) -> Unit, onGuard: (Boolean) -> Unit,
) = StepLayout("单位与天气更新", "选择日常显示方式，稍后可在设置中修改") {
    Text("温度单位", color = Color.White, fontWeight = FontWeight.SemiBold)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(TemperatureUnit.CELSIUS to "°C", TemperatureUnit.FAHRENHEIT to "°F", TemperatureUnit.KELVIN to "K").forEach { (v, l) ->
            ChoiceButton(l, temperature == v, Modifier.weight(1f)) { onTemperature(v) }
        }
    }
    Text("风速单位", color = Color.White, fontWeight = FontWeight.SemiBold)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(WindSpeedUnit.KILOMETRES_PER_HOUR to "km/h", WindSpeedUnit.METRES_PER_SECOND to "m/s").forEach { (v, l) ->
            ChoiceButton(l, wind == v, Modifier.weight(1f)) { onWind(v) }
        }
    }
    ToggleRow("自动更新天气", enabled, onEnabled)
    Text("更新间隔", color = Color.White, fontWeight = FontWeight.SemiBold)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(15, 30, 60).forEach { value -> ChoiceButton("$value 分钟", minutes == value, Modifier.weight(1f)) { onMinutes(value) } }
    }
    ToggleRow("近期数据不重复更新", guard, onGuard)
}

@Composable private fun ProviderStep(host: String, key: String, onHost: (String) -> Unit, onKey: (String) -> Unit) =
    StepLayout("内容供应商配置", "默认可直接使用免 Key 的 Open-Meteo；也可在此配置和风天气") {
        GlassCard {
            Text("Open-Meteo", fontWeight = FontWeight.Bold)
            Text("无需注册，跳过后将继续使用默认供应商。", fontSize = 12.sp)
        }
        GlassCard {
            Text("和风天气", fontWeight = FontWeight.Bold)
            OutlinedTextField(host, onHost, Modifier.fillMaxWidth(), label = { Text("API Host") }, singleLine = true)
            OutlinedTextField(key, onKey, Modifier.fillMaxWidth(), label = { Text("API Key") }, singleLine = true)
        }
    }

@Composable private fun CityStep(
    state: MainUiState,
    selected: CityLocation?,
    useLocation: Boolean,
    requested: Boolean,
    onCity: (CityLocation) -> Unit,
    onSearch: (String) -> Unit,
    onLocate: () -> Unit,
) =
    StepLayout("选择城市", "可使用定位，或先从热门城市中选择") {
        var query by remember { mutableStateOf("") }
        Button(onLocate, Modifier.fillMaxWidth(), enabled = !state.locating) {
            if (state.locating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Icon(Icons.Outlined.LocationOn, null)
            val locatedName = selected?.let { listOf(it.name, it.subtitle).filter(String::isNotBlank).joinToString(" · ") }
            Text(if (useLocation && locatedName != null) " $locatedName" else " 定位当前城市", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (requested && !useLocation) Text("未获得位置权限，仍可手动选择城市。", color = Color(0xFFFFC4C4), fontSize = 12.sp)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; onSearch(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("搜索全球城市") },
            placeholder = { Text("城市、行政区或邮政编码") },
            singleLine = true,
        )
        if (query.trim().length >= 2) {
            when {
                state.citySearching -> Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) }
                state.citySearchResults.isEmpty() -> Text("没有找到匹配城市", color = Color.White.copy(alpha = .72f), fontSize = 12.sp)
                else -> state.citySearchResults.take(6).forEach { city ->
                    GlassCard {
                        Row(Modifier.fillMaxWidth().clickable { onCity(city) }, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(city.name, fontWeight = FontWeight.SemiBold)
                                if (city.subtitle.isNotBlank()) Text(city.subtitle, fontSize = 11.sp)
                            }
                            if (selected?.id == city.id) Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Text("热门城市", color = Color.White, fontWeight = FontWeight.SemiBold)
        HOT_CITIES.take(9).chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { city -> ChoiceButton(city.name, selected?.id == city.id, Modifier.weight(1f)) { onCity(city) } }
            }
        }
    }

@Composable private fun NotificationStep(settings: NotificationSettings, requested: Boolean, onChange: (NotificationSettings) -> Unit, onPermission: () -> Unit) =
    StepLayout("配置通知", "选择希望接收的天气提醒") {
        ToggleRow("每日早间天气", settings.morningEnabled) { onChange(settings.copy(morningEnabled = it)) }
        ToggleRow("周日下周速览", settings.weeklyEnabled) { onChange(settings.copy(weeklyEnabled = it)) }
        ToggleRow("降雨提醒", settings.rainEnabled) { onChange(settings.copy(rainEnabled = it)) }
        ToggleRow("气象灾害预警", settings.alertEnabled) { onChange(settings.copy(alertEnabled = it)) }
        OutlinedButton(onPermission, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Notifications, null); Text(if (requested) " 已请求通知权限" else " 允许通知权限") }
    }

@Composable private fun FeatureStep() = StepLayout("特别功能", "这些功能稍后可在设置页面中详细配置") {
    FeatureCard(Icons.Outlined.AutoAwesome, "AI 建议", "使用兼容 Anthropic Messages 的服务生成个性化天气建议。")
    FeatureCard(Icons.Outlined.Palette, "外观皮肤与 Live2D", "更换字体、Logo、天气背景，并导入可互动 Live2D 模型。")
    FeatureCard(Icons.AutoMirrored.Filled.VolumeUp, "TTS 与音色", "使用系统 TTS，或导入自定义离线音色模型。")
}

@Composable
private fun AgreementDocumentStep(
    title: String,
    baseAsset: String,
    defaultLanguage: AppLanguage,
    documents: Map<String, String>,
    onReadToEnd: () -> Unit,
) {
    val translatable = baseAsset.endsWith(".md")
    var documentLanguage by remember(baseAsset, defaultLanguage) {
        mutableStateOf(if (translatable) defaultLanguage else AppLanguage.ENGLISH)
    }
    val document = documents[legalDocumentAsset(baseAsset, documentLanguage)].orEmpty()
    val scroll = rememberScrollState()
    LaunchedEffect(documentLanguage) { scroll.scrollTo(0) }
    LaunchedEffect(scroll) {
        snapshotFlow { scroll.value to scroll.maxValue }.collect { (value, maximum) ->
            if (maximum > 0 && value >= maximum - 8) onReadToEnd()
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("请滚动至文末后继续", color = Color.White.copy(alpha = .75f), fontSize = 13.sp)
            }
            if (translatable) LegalLanguageDropdown(documentLanguage) { documentLanguage = it }
            else Text("English", color = Color.White.copy(alpha = .8f), fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = .42f)).verticalScroll(scroll).padding(16.dp),
        ) {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.SIMPLIFIED_CHINESE) {
                MarkdownText(document)
            }
        }
    }
}

@Composable private fun WelcomeStep() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Confetti()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("配置完毕", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 34.sp)
            Spacer(Modifier.height(12.dp))
            Text("欢迎使用猫猫风雨", color = Color.White, fontSize = 20.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable private fun CopyrightBadge() {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(if (expanded) 1f else 0f, tween(210, easing = FastOutSlowInEasing), label = "copyrightProgress")
    LaunchedEffect(expanded) { if (expanded) { delay(3000); expanded = false } }
    Box(
        Modifier.width(210.dp).height(38.dp).pointerInput(expanded) {
            detectTapGestures { position ->
                if (expanded || position.x >= size.width - 42.dp.toPx()) {
                    if (expanded) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://x.com/@Nakkar7")))
                    else expanded = true
                }
            }
        },
    ) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                scaleX = .18f + progress * .82f
                transformOrigin = TransformOrigin(1f, .5f)
                alpha = progress
            }.clip(RoundedCornerShape(19.dp)).background(Color.Black.copy(alpha = .48f)),
        )
        Box(
            Modifier.align(Alignment.CenterEnd).size(38.dp).graphicsLayer { alpha = 1f - progress }
                .background(Color.Black.copy(alpha = .48f), CircleShape),
        )
        Text(
            "© Background @Nakkar7",
            color = Color.White,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.align(Alignment.Center).graphicsLayer { alpha = progress; translationX = (1f - progress) * 12.dp.toPx() },
        )
        Text("©", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp).graphicsLayer { alpha = 1f - progress })
    }
}

@Composable private fun ChoiceButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    if (selected) Button(
        onClick,
        modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = .72f)),
    ) { Text(label, maxLines = 1) }
    else OutlinedButton(onClick, modifier.fillMaxWidth()) { Text(label, color = Color.White, maxLines = 1) }
}

@Composable private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    GlassCard { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Switch(checked, onChange)
    } }
}

@Composable private fun FeatureCard(icon: ImageVector, title: String, description: String) = GlassCard {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, Modifier.size(28.dp))
        Column { Text(title, fontWeight = FontWeight.Bold); Text(description, fontSize = 12.sp) }
    }
}

@Composable private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .62f))) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable private fun Confetti() {
    val transition = rememberInfiniteTransition(label = "confetti")
    val progress by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(3800, easing = LinearEasing)), label = "confettiProgress")
    val colors = listOf(Color(0xFFFFD54F), Color(0xFFEF5350), Color(0xFF42A5F5), Color(0xFF66BB6A), Color(0xFFAB47BC))
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            // 前 72% 完成喷射和落下，后 28% 保持空场，与下一轮未喷出状态无缝衔接。
            if (progress < .72f) repeat(64) { i ->
                val local = (progress / .72f - (i % 9) * .012f).coerceIn(0f, 1f)
                if (local > 0f && local < 1f) {
                    val fromLeft = i % 2 == 0
                    val originX = if (fromLeft) size.width * .18f else size.width * .82f
                    val direction = if (fromLeft) 1f else -1f
                    val spread = 90f + ((i * 47) % 180)
                    val x = originX + direction * local * spread + sin(local * PI * 3 + i).toFloat() * 14f
                    val y = size.height * .82f - sin(local * PI).toFloat() * size.height * (.42f + (i % 7) * .025f) + local * local * 90f
                    drawRect(
                        colors[i % colors.size].copy(alpha = (1f - local).coerceIn(0f, 1f)),
                        topLeft = androidx.compose.ui.geometry.Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(7f + i % 3, 13f),
                    )
                }
            }
        }
        val burstPulse = if (progress < .18f) sin(progress / .18f * PI).toFloat().coerceAtLeast(0f) else 0f
        Text(
            "🎉",
            fontSize = 52.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 34.dp).graphicsLayer {
                scaleX = 1f + burstPulse * .18f
                scaleY = 1f - burstPulse * .10f
                rotationZ = -8f + burstPulse * 5f
                translationY = burstPulse * 5.dp.toPx()
            },
        )
        Text(
            "🎉",
            fontSize = 52.sp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 34.dp).graphicsLayer {
                scaleX = -(1f + burstPulse * .18f)
                scaleY = 1f - burstPulse * .10f
                rotationZ = 8f - burstPulse * 5f
                translationY = burstPulse * 5.dp.toPx()
            },
        )
    }
}
