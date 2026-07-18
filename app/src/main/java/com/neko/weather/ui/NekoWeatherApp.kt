package com.neko.weather.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Typeface
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material3.Typography
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.neko.weather.model.CityLocation
import com.neko.weather.model.DEFAULT_OPEN_METEO_QUERY
import com.neko.weather.model.DailyForecast
import com.neko.weather.model.HOT_CITIES
import com.neko.weather.model.HourlyForecast
import com.neko.weather.model.DEFAULT_AI_REQUEST_JSON
import com.neko.weather.model.WeatherProviderProfile
import com.neko.weather.model.AirProviderProfile
import com.neko.weather.model.AlertProviderProfile
import com.neko.weather.model.WeatherApiConfig
import com.neko.weather.model.WeatherDashboard
import com.neko.weather.model.CustomizationSettings
import com.neko.weather.model.Live2dPlacement
import com.neko.weather.model.TemperatureUnit
import com.neko.weather.model.WindSpeedUnit
import com.neko.weather.model.AppLanguage
import com.neko.weather.data.ConfigStore
import com.neko.weather.live2d.CubismRuntime
import com.neko.weather.live2d.Live2dModel
import com.neko.weather.data.ApiUsageTracker
import com.neko.weather.data.ApiQuota
import com.neko.weather.data.ApiUsageInterceptor
import com.neko.weather.data.BackupCategories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.time.Instant
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.min

private val NekoBlue = Color(0xFF3568D4)
private val Glass = Color.White.copy(alpha = 0.16f)
private val Divider = Color.White.copy(alpha = 0.20f)
private val LocalTemperatureUnit = staticCompositionLocalOf { TemperatureUnit.CELSIUS }
private val LocalWindSpeedUnit = staticCompositionLocalOf { WindSpeedUnit.KILOMETRES_PER_HOUR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NekoWeatherApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var providerMenuExpanded by remember { mutableStateOf(false) }
    var showQWeatherPreset by remember { mutableStateOf(false) }
    val settingsScrollState = rememberScrollState()
    var clockTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val isNight = state.weather?.let { isNightTime(it, clockTick) } == true
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val configStore = remember(context) { ConfigStore(context) }
    var onboardingDismissed by rememberSaveable { mutableStateOf(false) }
    val showOnboarding = !onboardingDismissed && !configStore.isOnboardingCompleted()

    BackHandler(enabled = page != 0) {
        page = when (page) {
            8 -> 6
            in listOf(2, 4, 5, 6, 7, 9, 10, 11, 12) -> 1
            else -> 0
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.refreshSettings.enabled, state.refreshSettings.intervalMinutes) {
        if (state.refreshSettings.enabled) {
            while (true) {
                delay(state.refreshSettings.intervalMinutes.coerceAtLeast(5) * 60_000L)
                viewModel.refresh(force = false)
            }
        }
    }
    val customFontFamily = remember(state.customization.fontPath) {
        state.customization.fontPath.takeIf(String::isNotBlank)?.let { path ->
            runCatching { FontFamily(Typeface.createFromFile(path)) }.getOrNull()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            clockTick = System.currentTimeMillis()
        }
    }

    SideEffect {
        view.context.findActivity()?.let { activity ->
            WindowCompat.getInsetsController(activity.window, view).apply {
                isAppearanceLightStatusBars = page != 0 || !isNight
                isAppearanceLightNavigationBars = page != 0 || !isNight
            }
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(localizedText(it, state.displaySettings.language))
            viewModel.clearMessage()
        }
    }

    CompositionLocalProvider(
        LocalAppLanguage provides state.displaySettings.language,
        LocalTemperatureUnit provides state.displaySettings.temperatureUnit,
        LocalWindSpeedUnit provides state.displaySettings.windSpeedUnit,
    ) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(primary = NekoBlue),
        typography = customFontFamily?.let(::customTypography) ?: Typography(),
    ) {
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val forward = targetState != 0 && (initialState == 0 || targetState > initialState)
                    val enterOffset: (Int) -> Int = { width -> if (forward) width else -width }
                    val exitOffset: (Int) -> Int = { width -> if (forward) -width / 3 else width / 3 }
                    (slideInHorizontally(tween(300), enterOffset) + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(260), exitOffset) + fadeOut(tween(180)))
                },
                label = "pageTransition",
                modifier = Modifier.fillMaxSize(),
            ) { targetPage ->
                if (targetPage == 0) {
                WeatherPage(
                    state = state,
                    viewModel = viewModel,
                    isNight = isNight,
                    forceStaticLive2d = page != 0,
                    openSettings = { page = 1 },
                    openCitySettings = { page = 3 },
                )
                } else {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = {
                                    Text(
                                        when (targetPage) {
                                            1 -> "设置"
                                            2 -> "内容供应商"
                                            3 -> "选择城市"
                                            4 -> "语音与音色"
                                            5 -> "关于"
                                            6 -> "外观与模型"
                                            8 -> "Live2D 布局编辑"
                                            9 -> "通知提醒"
                                            10 -> "备份与还原"
                                            11 -> "API 请求统计"
                                            12 -> "显示与天气更新"
                                            else -> "AI 建议"
                                        },
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = {
                                        page = when (targetPage) {
                                            8 -> 6
                                            in listOf(2, 4, 5, 6, 7, 9, 10, 11, 12) -> 1
                                            else -> 0
                                        }
                                    }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回天气")
                                    }
                                },
                                actions = {
                                    if (targetPage == 2) {
                                        Box {
                                            IconButton(onClick = { providerMenuExpanded = true }) {
                                                Icon(Icons.Default.MoreVert, "更多内容供应商设置")
                                            }
                                            DropdownMenu(
                                                expanded = providerMenuExpanded,
                                                onDismissRequest = { providerMenuExpanded = false },
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("和风天气组合设置") },
                                                    onClick = {
                                                        providerMenuExpanded = false
                                                        showQWeatherPreset = true
                                                    },
                                                )
                                            }
                                        }
                                    }
                                },
                            )
                        },
                    ) { padding ->
                        when (targetPage) {
                            1 -> SettingsPage(
                                state = state,
                                viewModel = viewModel,
                                openApi = { page = 2 },
                                openVoice = { page = 4 },
                                openAbout = { page = 5 },
                                openAppearance = { page = 6 },
                                openAi = { page = 7 },
                                openNotifications = { page = 9 },
                                openBackup = { page = 10 },
                                openDisplayAndRefresh = { page = 12 },
                                scrollState = settingsScrollState,
                                modifier = Modifier.padding(padding),
                            )
                            2 -> ApiConfigPage(state, viewModel, openStats = { page = 11 }, modifier = Modifier.padding(padding))
                            3 -> CityPage(state, viewModel, onClose = { page = 0 }, modifier = Modifier.padding(padding))
                            4 -> VoicePage(state, viewModel, Modifier.padding(padding))
                            5 -> AboutPage(state, viewModel, Modifier.padding(padding))
                            6 -> AppearancePage(state, viewModel, onOpenEditor = { page = 8 }, modifier = Modifier.padding(padding))
                            8 -> Live2dLayoutEditor(state, viewModel, Modifier.padding(padding))
                            9 -> NotificationSettingsPage(state, viewModel, Modifier.padding(padding))
                            10 -> BackupRestorePage(viewModel, Modifier.padding(padding))
                            11 -> ApiUsagePage(Modifier.padding(padding))
                            12 -> DisplayAndRefreshPage(state, viewModel, Modifier.padding(padding))
                            else -> AiSettingsPage(state, viewModel, Modifier.padding(padding))
                        }
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
            if (showQWeatherPreset) {
                QWeatherPresetDialog(state, viewModel) { showQWeatherPreset = false }
            }
            if (showOnboarding) {
                OnboardingScreen(state, viewModel) { result ->
                    viewModel.updateDisplaySettings(
                        state.displaySettings.copy(
                            language = result.language,
                            temperatureUnit = result.temperatureUnit,
                            windSpeedUnit = result.windSpeedUnit,
                        ),
                    )
                    viewModel.updateWeatherRefreshSettings(result.refreshSettings)
                    viewModel.updateNotificationSettings(result.notificationSettings)
                    if (result.qWeatherKey.isNotBlank()) {
                        viewModel.enableQWeatherPreset(result.qWeatherHost, result.qWeatherKey)
                    }
                    when {
                        result.useLocation -> viewModel.locateCurrentCity()
                        result.selectedCity != null -> viewModel.selectCity(result.selectedCity)
                    }
                    configStore.markOnboardingCompleted()
                    onboardingDismissed = true
                }
            }
        }
    }
    }
}

private fun customTypography(font: FontFamily): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = font),
        displayMedium = base.displayMedium.copy(fontFamily = font),
        displaySmall = base.displaySmall.copy(fontFamily = font),
        headlineLarge = base.headlineLarge.copy(fontFamily = font),
        headlineMedium = base.headlineMedium.copy(fontFamily = font),
        headlineSmall = base.headlineSmall.copy(fontFamily = font),
        titleLarge = base.titleLarge.copy(fontFamily = font),
        titleMedium = base.titleMedium.copy(fontFamily = font),
        titleSmall = base.titleSmall.copy(fontFamily = font),
        bodyLarge = base.bodyLarge.copy(fontFamily = font),
        bodyMedium = base.bodyMedium.copy(fontFamily = font),
        bodySmall = base.bodySmall.copy(fontFamily = font),
        labelLarge = base.labelLarge.copy(fontFamily = font),
        labelMedium = base.labelMedium.copy(fontFamily = font),
        labelSmall = base.labelSmall.copy(fontFamily = font),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherPage(
    state: MainUiState,
    viewModel: MainViewModel,
    isNight: Boolean,
    forceStaticLive2d: Boolean,
    openSettings: () -> Unit,
    openCitySettings: () -> Unit,
) {
    val twilight = state.weather?.let(::twilightMode)
    val condition = state.weather?.current?.condition.orEmpty()
    val topColor by animateColorAsState(
        when (twilight) {
            "DAWN" -> Color(0xFF705F91)
            "DUSK" -> Color(0xFF493B6B)
            else -> if (isNight) Color(0xFF07182D) else if ("阴" in condition || "雨" in condition) Color(0xFF40566D) else Color(0xFF4779CA)
        },
        animationSpec = tween(700),
        label = "weatherTopColor",
    )
    val middleColor by animateColorAsState(
        when (twilight) {
            "DAWN" -> Color(0xFFE89983)
            "DUSK" -> Color(0xFFCE715E)
            else -> if (isNight) Color(0xFF102D4C) else if ("阴" in condition || "雨" in condition) Color(0xFF66798B) else Color(0xFF6A9DDB)
        },
        animationSpec = tween(700),
        label = "weatherMiddleColor",
    )
    val bottomColor by animateColorAsState(
        when (twilight) {
            "DAWN" -> Color(0xFFFFC98E)
            "DUSK" -> Color(0xFFF0A26D)
            else -> if (isNight) Color(0xFF234B6B) else if ("阴" in condition || "雨" in condition) Color(0xFF91A0AD) else Color(0xFFA7CBEA)
        },
        animationSpec = tween(700),
        label = "weatherBottomColor",
    )
    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = { viewModel.refresh(force = true) },
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(topColor, middleColor, bottomColor),
                ),
            ),
        ) {
            CustomBackground(state.customization, condition)
            WeatherSceneLayer(condition, isNight)
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                WeatherToolbar(state, viewModel, openSettings, openCitySettings)
                when {
                    state.weather != null -> DashboardContent(
                        weather = state.weather!!,
                        customization = state.customization,
                        homeCities = state.savedCities.filter(CityLocation::showOnHome),
                        config = state.config,
                        onPreviousCity = { viewModel.switchHomeCity(-1) },
                        onNextCity = { viewModel.switchHomeCity(1) },
                        aiAdvice = state.aiAdvice,
                        aiLoading = state.aiLoading,
                        aiConfigured = state.aiSettings.apiKey.isNotBlank(),
                        onRequestAiAdvice = viewModel::requestAiAdvice,
                        onClearAiAdvice = viewModel::clearAiAdvice,
                        onToggleLive2d = { viewModel.setLive2dHidden(!state.customization.live2dHidden) },
                        forceStaticLive2d = forceStaticLive2d,
                    )
                    state.loading -> Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                    else -> GlassCard { Text("暂无天气数据，请检查 API 配置", color = Color.White) }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun WeatherSceneLayer(condition: String, isNight: Boolean) {
    val hasClouds = listOf("云", "阴", "雨", "雷", "雪", "雾").any(condition::contains)
    val hasRain = "雨" in condition || "雷" in condition
    val hasSnow = "雪" in condition

    if (!hasClouds && !hasRain && !hasSnow) {
        if (!isNight && "晴" in condition) {
            Canvas(Modifier.fillMaxSize().alpha(0.55f)) {
                drawCircle(Color(0xFFFFE89A).copy(alpha = 0.32f), radius = 150f, center = Offset(size.width * .83f, size.height * .12f))
                drawCircle(Color(0xFFFFF2B8).copy(alpha = 0.72f), radius = 52f, center = Offset(size.width * .83f, size.height * .12f))
            }
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "weatherScene")
    val cloudProgress by transition.animateFloat(
        0f, 1f,
        animationSpec = infiniteRepeatable(tween(22_000, easing = LinearEasing), RepeatMode.Restart),
        label = "cloudProgress",
    )
    val precipitationProgress by transition.animateFloat(
        0f, 1f,
        animationSpec = infiniteRepeatable(
            tween(if (hasSnow) 7_000 else 1_350, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "precipitationProgress",
    )
    Canvas(Modifier.fillMaxSize().alpha(0.55f)) {
        if (!isNight && "晴" in condition) {
            drawCircle(Color(0xFFFFE89A).copy(alpha = 0.32f), radius = 150f, center = Offset(size.width * .83f, size.height * .12f))
            drawCircle(Color(0xFFFFF2B8).copy(alpha = 0.72f), radius = 52f, center = Offset(size.width * .83f, size.height * .12f))
        }
        if (hasClouds) {
            val travelDistance = size.width + 700f
            repeat(4) { group ->
                // 每朵云只在完全离开屏幕后回到另一侧，循环边界不可见。
                val phase = (cloudProgress + group * 0.27f) % 1f
                val x = -300f + phase * travelDistance
                val y = 105f + (group % 3) * 105f
                val cloud = if ("阴" in condition || "雨" in condition) Color(0xFF33485B) else Color.White
                val cloudAlpha = if ("雾" in condition) .14f else .24f
                drawCircle(cloud.copy(alpha = cloudAlpha), 80f, Offset(x, y))
                drawCircle(cloud.copy(alpha = cloudAlpha), 105f, Offset(x + 75f, y - 20f))
                drawCircle(cloud.copy(alpha = cloudAlpha), 72f, Offset(x + 155f, y + 5f))
            }
        }
        if (hasRain) {
            repeat(45) { index ->
                val phase = (precipitationProgress + index / 45f) % 1f
                val x = (index * 83f) % size.width - phase * 90f
                val y = -35f + phase * (size.height + 70f)
                drawLine(Color(0xFFCDEBFF).copy(alpha = .45f), Offset(x, y), Offset(x - 7f, y + 25f), strokeWidth = 2.2f, cap = StrokeCap.Round)
            }
        } else if (hasSnow) {
            repeat(35) { index ->
                val phase = (precipitationProgress + index / 35f) % 1f
                val drift = kotlin.math.sin(phase * Math.PI * 2 + index) * 22f
                val x = (index * 97f) % size.width + drift.toFloat()
                val y = -20f + phase * (size.height + 40f)
                drawCircle(Color.White.copy(alpha = .65f), 3f + index % 4, Offset(x, y))
            }
        }
    }
}

@Composable
private fun CustomBackground(settings: CustomizationSettings, condition: String = "") {
    val weatherKey = when {
        "雷" in condition -> "thunder"
        "雪" in condition -> "snow"
        "雨" in condition -> "rain"
        "雾" in condition -> "fog"
        "阴" in condition || "云" in condition -> "cloudy"
        "晴" in condition -> "clear"
        else -> ""
    }
    val weatherPath = settings.weatherBackgrounds[weatherKey].orEmpty()
    val path = weatherPath.ifBlank { settings.backgroundPath }
    if (path.isBlank()) return
    if (weatherPath.isBlank() && settings.backgroundIsVideo) {
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    setVideoPath(path)
                    setOnPreparedListener { player -> player.isLooping = true; player.setVolume(0f, 0f); start() }
                }
            },
            update = { if (!it.isPlaying) it.start() },
            modifier = Modifier.fillMaxSize().alpha(0.62f),
        )
    } else {
        CustomImage(path, Modifier.fillMaxSize().alpha(if (weatherPath.isNotBlank()) 1f else .62f), ContentScale.Crop)
    }
}

@Composable
private fun CustomImage(path: String, modifier: Modifier, contentScale: ContentScale = ContentScale.Fit) {
    val bitmap = remember(path) { path.takeIf(String::isNotBlank)?.let(BitmapFactory::decodeFile)?.asImageBitmap() }
    bitmap?.let { Image(it, null, modifier = modifier, contentScale = contentScale) }
}

private fun twilightMode(weather: WeatherDashboard): String? = runCatching {
    val zone = ZoneId.of(weather.current.timezone ?: ZoneId.systemDefault().id)
    val now = ZonedDateTime.now(zone).toLocalDateTime()
    val today = weather.daily.firstOrNull { it.date == now.toLocalDate().toString() } ?: return null
    val sunrise = today.sunrise?.let(java.time.LocalDateTime::parse)
    val sunset = today.sunset?.let(java.time.LocalDateTime::parse)
    when {
        sunrise != null && kotlin.math.abs(java.time.Duration.between(sunrise, now).toMinutes()) <= 45 -> "DAWN"
        sunset != null && kotlin.math.abs(java.time.Duration.between(sunset, now).toMinutes()) <= 45 -> "DUSK"
        else -> null
    }
}.getOrNull()

@Composable
private fun WeatherToolbar(
    state: MainUiState,
    viewModel: MainViewModel,
    openSettings: () -> Unit,
    openCitySettings: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (state.customization.logoPath.isNotBlank()) {
            CustomImage(state.customization.logoPath, Modifier.size(38.dp))
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f).clickable(onClick = openCitySettings)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text(state.config.locationName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text(state.config.displayName, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
            state.weather?.let {
                Text(lastUpdatedText(it.current.fetchedAtMillis), color = Color.White.copy(alpha = .58f), fontSize = 10.sp)
            }
        }
        IconButton(viewModel::refresh, enabled = !state.loading) {
            Icon(Icons.Default.Refresh, "刷新", tint = Color.White)
        }
        IconButton(viewModel::speakWeather, enabled = state.weather != null) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, "播报天气", tint = Color.White)
        }
        IconButton(openSettings) { Icon(Icons.Default.Settings, "设置", tint = Color.White) }
    }
}

private fun lastUpdatedText(timestamp: Long): String {
    val time = java.time.Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
    val now = ZonedDateTime.now()
    val prefix = if (time.toLocalDate() == now.toLocalDate()) "" else time.format(DateTimeFormatter.ofPattern("M/d "))
    return "上次更新于 $prefix${time.format(DateTimeFormatter.ofPattern("HH:mm"))}"
}

@Composable
private fun DashboardContent(
    weather: WeatherDashboard,
    customization: CustomizationSettings,
    homeCities: List<CityLocation>,
    config: WeatherApiConfig,
    onPreviousCity: () -> Unit,
    onNextCity: () -> Unit,
    aiAdvice: String?,
    aiLoading: Boolean,
    aiConfigured: Boolean,
    onRequestAiAdvice: () -> Unit,
    onClearAiAdvice: () -> Unit,
    onToggleLive2d: () -> Unit,
    forceStaticLive2d: Boolean,
) {
    var swipeDirection by remember { mutableIntStateOf(1) }
    val swipeModifier = Modifier.pointerInput(homeCities.map(CityLocation::id)) {
        var horizontalDrag = 0f
        detectHorizontalDragGestures(
            onDragStart = { horizontalDrag = 0f },
            onHorizontalDrag = { change, amount ->
                horizontalDrag += amount
                change.consume()
            },
            onDragEnd = {
                when {
                    horizontalDrag <= -90f -> { swipeDirection = 1; onNextCity() }
                    horizontalDrag >= 90f -> { swipeDirection = -1; onPreviousCity() }
                }
                horizontalDrag = 0f
            },
            onDragCancel = { horizontalDrag = 0f },
        )
    }
    val cityKey = "${config.latitude},${config.longitude}"
    AnimatedContent(
        targetState = cityKey,
        transitionSpec = {
            val direction = swipeDirection
            (slideInHorizontally(tween(320)) { width -> direction * width } + fadeIn(tween(220))) togetherWith
                (slideOutHorizontally(tween(280)) { width -> -direction * width } + fadeOut(tween(180)))
        },
        label = "homeCitySwipe",
        modifier = Modifier.fillMaxWidth(),
    ) {
        CurrentHero(weather, customization, onToggleLive2d, forceStaticLive2d, swipeModifier)
    }
    if (homeCities.size > 1) {
        val latitude = config.latitude.toDoubleOrNull()
        val longitude = config.longitude.toDoubleOrNull()
        val selectedIndex = homeCities.indexOfFirst {
            latitude != null && longitude != null &&
                kotlin.math.abs(it.latitude - latitude) < 0.0001 && kotlin.math.abs(it.longitude - longitude) < 0.0001
        }.coerceAtLeast(0)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            homeCities.forEachIndexed { index, _ ->
                Box(
                    Modifier.padding(horizontal = 3.dp).size(if (index == selectedIndex) 18.dp else 6.dp, 6.dp)
                        .background(Color.White.copy(alpha = if (index == selectedIndex) .9f else .35f), CircleShape),
                )
            }
        }
    }
    if (weather.alerts.isNotEmpty()) AlertsCard(weather)
    CompactHourlyCard(weather.hourly)
    SectionCard("未来 15 日") { DailyList(weather.daily) }
    AirQualityCard(weather)
    MetricsCard(weather)
    SunCard(weather)
    AdviceCard(weather)
    AiAdviceCard(aiAdvice, aiLoading, aiConfigured, onRequestAiAdvice, onClearAiAdvice)
    RainCard(
        weather.hourly,
        config.latitude.toDoubleOrNull(),
        config.longitude.toDoubleOrNull(),
        config.locationCountry,
        config.radarMapProvider,
        config.amapWebApiKey,
        config.petalMapApiKey,
        config.baiduMapAk,
    )
    if (weather.alerts.isEmpty()) AlertsCard(weather)
    val airSource = if (config.airQualityEndpoint.contains("qweather")) "和风天气空气质量" else "CAMS / Open-Meteo"
    val alertSource = if (config.alertProvider == "QWEATHER") "和风天气预警" else weather.alertSource
    Text(
        listOfNotNull("天气：${config.displayName}", "空气质量：$airSource", alertSource?.let { "预警：$it" }).joinToString(" · "),
        color = Color.White.copy(alpha = 0.7f),
        fontSize = 11.sp,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun CurrentHero(
    weather: WeatherDashboard,
    customization: CustomizationSettings,
    onToggleLive2d: () -> Unit,
    forceStaticLive2d: Boolean,
    modifier: Modifier = Modifier,
) {
    val current = weather.current
    val unit = LocalTemperatureUnit.current
    val temperatureNumber = convertTemperature(current.temperature, unit).roundToInt().toString()
    val unitMark = if (unit == TemperatureUnit.KELVIN) "K" else "°"
    var numberWidthPx by remember(temperatureNumber) { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val placement = customization.live2dModels.firstOrNull { it.id == customization.selectedLive2dId }?.placement
        ?: Live2dPlacement()
    Column(modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = weatherIcon(current.condition),
            contentDescription = current.condition,
            tint = Color.White,
            modifier = Modifier.size(58.dp),
        )
        Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            Text(
                temperatureNumber,
                color = Color.White,
                fontSize = 82.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.onGloballyPositioned { numberWidthPx = it.size.width },
            )
            if (customization.live2dEnabled && customization.live2dModelPath.isNotBlank() && !forceStaticLive2d) {
                if (!customization.live2dHidden) {
                    Live2dModel(
                        modelPath = customization.live2dModelPath,
                        previewPath = customization.live2dPreviewPath,
                        forceStatic = false,
                        modifier = Modifier.align(Alignment.CenterEnd)
                            .offset(x = placement.offsetX.dp, y = placement.offsetY.dp)
                            .padding(end = 6.dp)
                            .size(96.dp, 116.dp)
                            .graphicsLayer {
                                scaleX = placement.scale
                                scaleY = placement.scale
                                rotationZ = placement.rotation
                            },
                    )
                    Box(
                        modifier = Modifier.align(Alignment.CenterEnd)
                            .offset(x = placement.offsetX.dp, y = placement.offsetY.dp - 48.dp)
                            .size(14.dp)
                            .background(Color.Black.copy(alpha = .14f), CircleShape)
                            .clickable(onClick = onToggleLive2d),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "收起模型", tint = Color.White.copy(alpha = .75f), modifier = Modifier.size(8.dp)) }
                } else {
                    Box(
                        modifier = Modifier.align(Alignment.CenterEnd).size(16.dp)
                            .background(Color.Black.copy(alpha = .14f), CircleShape)
                            .clickable(onClick = onToggleLive2d),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "展开模型", tint = Color.White.copy(alpha = .75f), modifier = Modifier.size(9.dp)) }
                }
            }
            Text(
                unitMark,
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.align(Alignment.Center).offset(
                    x = with(density) { (numberWidthPx / 2f).toDp() + 5.dp },
                    y = (-19).dp,
                ),
            )
        }
        Text(current.condition, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        weather.daily.firstOrNull()?.let { today ->
            Text(
                "最高 ${temperatureText(today.high, unit)}  ·  最低 ${temperatureText(today.low, unit)}",
                color = Color.White.copy(alpha = .82f),
                fontSize = 13.sp,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("体感 ${current.apparentTemperature?.let { temperatureText(it, unit) } ?: "—"}", color = Color.White)
            Text("湿度 ${current.humidity?.roundToInt()?.let { "$it%" } ?: "—"}", color = Color.White)
            weather.airQuality?.let { air ->
                air.aqi?.let { Text("空气 ${aqiLabel(it, air.standard)} ${it.roundToInt()}", color = Color.White) }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    GlassCard {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun CompactHourlyCard(hourly: List<HourlyForecast>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Glass),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text("逐小时预报", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            HourlyStrip(hourly)
        }
    }
}

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Glass),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), content = content)
    }
}

@Composable
private fun HourlyStrip(hourly: List<HourlyForecast>) {
    if (hourly.isEmpty()) {
        Text("当前 API 未提供逐小时数据", color = Color.White.copy(alpha = 0.8f))
        return
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        hourly.forEachIndexed { index, item ->
            Column(Modifier.width(55.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (index == 0) "现在" else item.time.takeLast(5), color = Color.White, fontSize = 11.sp)
                Icon(
                    weatherIcon(item.condition),
                    item.condition,
                    tint = Color.White,
                    modifier = Modifier.padding(vertical = 5.dp).size(21.dp),
                )
                Text(temperatureText(item.temperature), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    item.precipitationProbability?.roundToInt()?.let { "$it%" } ?: " ",
                    color = Color(0xFFD5ECFF),
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun DailyList(daily: List<DailyForecast>) {
    if (daily.isEmpty()) {
        Text("当前 API 未提供每日预报", color = Color.White.copy(alpha = 0.8f))
        return
    }
    var expanded by rememberSaveable(daily.firstOrNull()?.date, daily.size) { mutableStateOf(false) }
    val today = LocalDate.now()
    val defaultCount = daily.indexOfLast { item ->
        runCatching { LocalDate.parse(item.date) <= today.plusDays(5) }.getOrDefault(false)
    }.let { if (it >= 0) it + 1 else minOf(7, daily.size) }
    daily.take(defaultCount).forEach { day ->
        DailyForecastRow(day, today)
        HorizontalDivider(color = Divider)
    }
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(animationSpec = tween(320)) + fadeIn(tween(260)),
        exit = shrinkVertically(animationSpec = tween(280)) + fadeOut(tween(180)),
    ) {
        Column {
            daily.drop(defaultCount).forEachIndexed { index, day ->
                DailyForecastRow(day, today)
                if (index != daily.size - defaultCount - 1) HorizontalDivider(color = Divider)
            }
        }
    }
    if (daily.size > defaultCount) {
        Spacer(Modifier.height(8.dp))
        val arrowRotation by animateFloatAsState(
            targetValue = if (expanded) 270f else 90f,
            animationSpec = tween(300),
            label = "forecastArrow",
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.09f)),
        ) {
            Button(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                elevation = null,
            ) {
                Text(if (expanded) "收起" else "展开后续 ${daily.size - defaultCount} 天", color = Color.White)
                Spacer(Modifier.width(5.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = arrowRotation },
                )
            }
        }
    }
}

@Composable
private fun DailyForecastRow(day: DailyForecast, today: LocalDate) {
    val isYesterday = runCatching { LocalDate.parse(day.date) == today.minusDays(1) }.getOrDefault(false)
    Row(
        Modifier.fillMaxWidth().alpha(if (isYesterday) 0.48f else 1f).padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(dayLabel(day.date), color = Color.White, modifier = Modifier.width(76.dp))
        Icon(
            weatherIcon(day.condition),
            day.condition,
            tint = Color.White,
            modifier = Modifier.width(42.dp).size(23.dp),
        )
        Text(day.condition, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(
            day.precipitationProbability?.roundToInt()?.takeIf { it > 0 }?.let { "$it%" }.orEmpty(),
            color = Color(0xFFD5ECFF),
            fontSize = 12.sp,
            modifier = Modifier.width(42.dp),
        )
        Text("${temperatureText(day.low)}  /  ${temperatureText(day.high)}", color = Color.White, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AirQualityCard(weather: WeatherDashboard) {
    val air = weather.airQuality
    SectionCard("空气质量") {
        if (air == null) {
            Text("当前数据源未提供空气质量", color = Color.White.copy(alpha = 0.8f))
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(air.aqi?.roundToInt()?.toString() ?: "—", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Light)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(air.aqi?.let { aqiLabel(it, air.standard) } ?: "未知", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (air.standard == "EU") "AQI（欧标）" else "AQI（美标）",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Pollutant("PM2.5", air.pm25)
                Pollutant("PM10", air.pm10)
                Pollutant("NO₂", air.nitrogenDioxide)
                Pollutant("O₃", air.ozone)
            }
        }
    }
}

@Composable
private fun Pollutant(name: String, value: Double?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value?.roundToInt()?.toString() ?: "—", color = Color.White, fontWeight = FontWeight.Bold)
        Text(name, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
    }
}

@Composable
private fun MetricsCard(weather: WeatherDashboard) {
    val current = weather.current
    val today = weather.daily.firstOrNull()
    val windUnit = LocalWindSpeedUnit.current
    SectionCard("今日详情") {
        MetricRow(
            Icons.Outlined.Air,
            current.windSpeed?.let { "${windDirection(current.windDirection)}${windForceLevel(it)}级" } ?: "风速",
            current.windSpeed?.let { windSpeedText(it, windUnit) } ?: "—",
            Icons.Outlined.WbSunny, "紫外线", today?.uvIndex?.let { "${uvLabel(it)} ${"%.1f".format(it)}" } ?: "—",
        )
        Spacer(Modifier.height(18.dp))
        MetricRow(
            Icons.Outlined.Thermostat, "体感温度", current.apparentTemperature?.let { temperatureText(it) } ?: "—",
            Icons.Outlined.WaterDrop, "湿度", current.humidity?.roundToInt()?.let { "$it%" } ?: "—",
        )
        Spacer(Modifier.height(18.dp))
        MetricRow(
            Icons.Outlined.Speed, "气压", current.pressure?.roundToInt()?.let { "$it hPa" } ?: "—",
            Icons.Outlined.Visibility, "能见度", current.visibility?.let { "${"%.1f".format(it)} km" } ?: "—",
        )
    }
}

@Composable
private fun MetricRow(icon1: ImageVector, label1: String, value1: String, icon2: ImageVector, label2: String, value2: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Metric(icon1, label1, value1, Modifier.weight(1f))
        Metric(icon2, label2, value2, Modifier.weight(1f))
    }
}

@Composable
private fun Metric(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, label, tint = Color.White, modifier = Modifier.size(25.dp))
        Spacer(Modifier.width(9.dp))
        Column {
            Text(label, color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SunCard(weather: WeatherDashboard) {
    val observationDate = weather.current.observationTime?.take(10)
    val todayIndex = weather.daily.indexOfFirst { it.date == observationDate }.let { if (it >= 0) it else 0 }
    val today = weather.daily.getOrNull(todayIndex)
    val tomorrow = weather.daily.getOrNull(todayIndex + 1)
    val currentMinute = minuteOfDay(weather.current.observationTime)
    val sunsetMinute = minuteOfDay(today?.sunset)
    val afterSunset = currentMinute != null && sunsetMinute != null && currentMinute >= sunsetMinute
    val firstLabel = if (afterSunset) "日落" else "日出"
    val firstTime = if (afterSunset) today?.sunset else today?.sunrise
    val secondLabel = if (afterSunset) "日出" else "日落"
    val secondTime = if (afterSunset) tomorrow?.sunrise else today?.sunset
    val elevation = weather.current.solarElevation ?: 0.0
    val sunriseMinute = minuteOfDay(today?.sunrise)
    val daySunsetMinute = minuteOfDay(today?.sunset)
    val daylightPoints = weather.solarDay.filter { point ->
        val minute = minuteOfDay(point.time)
        val sameDate = point.time.length < 10 || today?.date == null || point.time.take(10) == today.date
        sameDate && minute != null && sunriseMinute != null && daySunsetMinute != null && minute in sunriseMinute..daySunsetMinute
    }
    val solarNoon = daylightPoints.maxByOrNull { it.elevation }?.time?.takeLast(5) ?: "12:00"
    val solarNoonMinute = minuteOfDay(daylightPoints.maxByOrNull { it.elevation }?.time)
    val sameHalfDayIndices = daylightPoints.indices.filter { index ->
        val pointMinute = minuteOfDay(daylightPoints[index].time)
        when {
            currentMinute == null || solarNoonMinute == null || pointMinute == null -> true
            currentMinute <= solarNoonMinute -> pointMinute <= solarNoonMinute
            else -> pointMinute >= solarNoonMinute
        }
    }
    // 高度角在日出和日落附近近似对称，单独按角度会选到另一个半日。
    // 先锁定上午/下午，再以观测时间为主，角度只用于时间相同情况下的决胜。
    val currentHighlightIndex = sameHalfDayIndices.minWithOrNull(
        compareBy<Int> { abs((minuteOfDay(daylightPoints[it].time) ?: 0) - (currentMinute ?: 0)) }
            .thenBy { abs(daylightPoints[it].elevation - elevation) },
    )
    var selectedSolarIndex by remember(daylightPoints) { mutableStateOf<Int?>(null) }

    SectionCard(if (afterSunset) "日落与日出" else "日出与日落") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SunTime(firstLabel, firstTime)
            SunTime(secondLabel, secondTime)
        }
        Spacer(Modifier.height(8.dp))
        Text("当前太阳高度角 ${"%.1f".format(elevation)}°", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(96.dp).padding(horizontal = 10.dp)) {
            Canvas(
                Modifier.fillMaxSize().pointerInput(daylightPoints) {
                    fun selectAt(x: Float) {
                        if (daylightPoints.isNotEmpty()) {
                            selectedSolarIndex = (x / size.width * daylightPoints.lastIndex).roundToInt().coerceIn(daylightPoints.indices)
                        }
                    }
                    detectDragGesturesAfterLongPress(
                        onDragStart = { selectAt(it.x) },
                        onDrag = { change, _ -> selectAt(change.position.x); change.consume() },
                        onDragEnd = { selectedSolarIndex = null },
                        onDragCancel = { selectedSolarIndex = null },
                    )
                },
            ) {
                val baseline = size.height - 4.dp.toPx()
                val step = if (daylightPoints.size <= 1) size.width else size.width / (daylightPoints.size - 1)
                val width = minOf(8.dp.toPx(), step * .62f)
                drawLine(Color.White.copy(alpha = .32f), Offset(0f, baseline), Offset(size.width, baseline), 2f)
                daylightPoints.forEachIndexed { index, point ->
                    val x = index * step
                    val height = (point.elevation.coerceIn(0.0, 90.0) / 90.0 * (baseline - 6.dp.toPx())).toFloat()
                    drawRoundRect(
                        if (index == currentHighlightIndex) Color(0xFFFFB52E) else Color(0xFFFFDA72).copy(alpha = .48f),
                        Offset(x - width / 2f, baseline - height),
                        Size(width, height.coerceAtLeast(2f)),
                        CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    )
                }
                selectedSolarIndex?.let { index ->
                    val point = daylightPoints.getOrNull(index) ?: return@let
                    val x = index * step
                    val y = baseline - (point.elevation.coerceIn(0.0, 90.0) / 90.0 * (baseline - 6.dp.toPx())).toFloat()
                    drawLine(Color.White.copy(alpha = .55f), Offset(x, 0f), Offset(x, baseline), 1.5f)
                    drawCircle(Color.White, 4.dp.toPx(), Offset(x, y))
                }
            }
            selectedSolarIndex?.let { index ->
                daylightPoints.getOrNull(index)?.let { point ->
                    Text(
                        "${point.time.takeLast(5)} · ${"%.1f".format(point.elevation)}°",
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.TopCenter).background(Color(0xCC26384A), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(today?.sunrise?.takeLast(5) ?: "日出", color = Color.White.copy(alpha = .65f), fontSize = 10.sp)
            Text(solarNoon, color = Color.White.copy(alpha = .65f), fontSize = 10.sp)
            Text(today?.sunset?.takeLast(5) ?: "日落", color = Color.White.copy(alpha = .65f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun SunTime(label: String, value: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.LightMode, label, tint = Color.White, modifier = Modifier.size(25.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            Text(value?.takeLast(5) ?: "—", color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun AdviceCard(weather: WeatherDashboard) {
    val current = weather.current
    val rain = weather.hourly.take(12).mapNotNull { it.precipitationProbability }.maxOrNull() ?: 0.0
    val uv = weather.daily.firstOrNull()?.uvIndex ?: 0.0
    val air = weather.airQuality
    SectionCard("生活建议") {
        Advice(Icons.Outlined.Checkroom, "穿衣", when {
            current.temperature < 10 -> "天气偏冷，建议穿厚外套"
            current.temperature > 28 -> "天气炎热，建议轻薄透气着装"
            else -> "体感舒适，薄外套即可"
        })
        HorizontalDivider(color = Divider, modifier = Modifier.padding(vertical = 10.dp))
        Advice(Icons.Outlined.Umbrella, "雨具", if (rain >= 40) "降雨概率较高，出门请带伞" else "近期降雨概率较低")
        HorizontalDivider(color = Divider, modifier = Modifier.padding(vertical = 10.dp))
        Advice(Icons.Outlined.WbSunny, "防晒", if (uv >= 6) "紫外线较强，请使用防晒用品" else "紫外线不强，适度防护")
        HorizontalDivider(color = Divider, modifier = Modifier.padding(vertical = 10.dp))
        Advice(Icons.AutoMirrored.Outlined.DirectionsRun, "运动", if (air?.let(::isAirUnhealthy) == true) "空气质量不佳，减少户外运动" else "空气条件适合户外活动")
    }
}

@Composable
private fun Advice(icon: ImageVector, title: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, title, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(text, color = Color.White.copy(alpha = 0.78f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun AiAdviceCard(
    advice: String?,
    loading: Boolean,
    configured: Boolean,
    onRequest: () -> Unit,
    onClear: () -> Unit,
) {
    var visibleCharacters by remember(advice) { mutableIntStateOf(0) }
    var expanded by rememberSaveable { mutableStateOf(true) }
    val characterAlpha = remember(advice) { Animatable(1f) }
    LaunchedEffect(advice) {
        visibleCharacters = 0
        expanded = true
        val text = advice ?: return@LaunchedEffect
        while (visibleCharacters < text.length) {
            characterAlpha.snapTo(0f)
            visibleCharacters++
            characterAlpha.animateTo(1f, tween(34))
        }
    }
    SectionCard("AI 天气建议") {
        when {
            advice != null && expanded -> MarkdownText(advice.take(visibleCharacters), characterAlpha.value)
            advice != null -> Text("建议内容已折叠", color = Color.White.copy(alpha = .65f), fontSize = 12.sp)
            configured -> Text("基于当前天气、逐小时预报、空气质量和预警生成个性化建议。", color = Color.White.copy(alpha = .75f), fontSize = 12.sp)
            else -> Text("请先在设置中配置 Anthropic 兼容 API。", color = Color.White.copy(alpha = .75f), fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onRequest,
                enabled = configured && !loading,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = .18f)),
            ) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(17.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (advice == null) "生成建议" else "重新生成", color = Color.White)
            }
            Spacer(Modifier.weight(1f))
            if (advice != null) {
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "折叠" else "展开", color = Color.White) }
                TextButton(onClick = onClear) { Text("清空", color = Color.White.copy(alpha = .8f)) }
            }
        }
        Text("AI 内容仅供参考；请求可能产生 API 费用。", color = Color.White.copy(alpha = .52f), fontSize = 10.sp)
    }
}

@Composable
internal fun MarkdownText(markdown: String, trailingAlpha: Float = 1f) {
    Column(Modifier.fillMaxWidth()) {
        val lines = markdown.lines()
        var inCodeBlock = false
        var index = 0
        while (index < lines.size) {
        val rawLine = lines[index]
        val trimmed = rawLine.trim()
        val fade = if (index == lines.lastIndex) trailingAlpha else 1f
        if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
            inCodeBlock = !inCodeBlock
            index++
            continue
        }
        when {
            inCodeBlock -> Text(
                inlineMarkdown(rawLine, fade),
                color = Color(0xFFE8F1FF),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = .2f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
            )
            rawLine.isBlank() -> Spacer(Modifier.height(5.dp))
            index + 1 < lines.size && lines[index + 1].trim().matches(Regex("=+")) -> {
                Text(inlineMarkdown(rawLine, fade), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                index++
            }
            index + 1 < lines.size && lines[index + 1].trim().matches(Regex("-+")) -> {
                Text(inlineMarkdown(rawLine, fade), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                index++
            }
            markdownHeading(rawLine) != null -> {
                val (level, content) = markdownHeading(rawLine)!!
                Text(
                    inlineMarkdown(content, fade),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = when (level) { 1 -> 20.sp; 2 -> 18.sp; 3 -> 16.sp; 4 -> 15.sp; 5 -> 14.sp; else -> 13.sp },
                    modifier = Modifier.padding(top = if (level <= 2) 5.dp else 2.dp, bottom = 2.dp),
                )
            }
            isMarkdownDivider(trimmed) -> HorizontalDivider(color = Color.White.copy(alpha = .28f), modifier = Modifier.padding(vertical = 7.dp))
            isMarkdownTableDivider(trimmed) -> HorizontalDivider(color = Color.White.copy(alpha = .22f), modifier = Modifier.padding(vertical = 2.dp))
            trimmed.startsWith("|") && trimmed.endsWith("|") -> MarkdownTableRow(trimmed, fade)
            Regex("^\\s*[-*+]\\s+.*").matches(rawLine) -> Row(Modifier.padding(start = (rawLine.length - rawLine.trimStart().length).dp.coerceAtMost(24.dp), top = 2.dp, bottom = 2.dp)) {
                val content = rawLine.trimStart().drop(2)
                val task = Regex("^\\[([ xX])]\\s+(.*)").find(content)
                Text(if (task == null) "•  " else if (task.groupValues[1].isBlank()) "☐  " else "☑  ", color = Color(0xFFFFD36B), fontWeight = FontWeight.Bold)
                Text(inlineMarkdown(task?.groupValues?.get(2) ?: content, fade), color = Color.White.copy(alpha = .9f), lineHeight = 21.sp, modifier = Modifier.weight(1f))
            }
            rawLine.trimStart().matches(Regex("\\d+[.)] .*")) -> {
                val split = rawLine.trimStart().indexOf(' ')
                Row(Modifier.padding(vertical = 2.dp)) {
                    Text(rawLine.trimStart().take(split + 1), color = Color(0xFFFFD36B), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Text(inlineMarkdown(rawLine.trimStart().drop(split + 1), fade), color = Color.White.copy(alpha = .9f), lineHeight = 21.sp, modifier = Modifier.weight(1f))
                }
            }
            rawLine.trimStart().startsWith(">") -> Row(Modifier.padding(vertical = 2.dp)) {
                Box(Modifier.width(3.dp).height(21.dp).background(Color.White.copy(alpha = .35f), RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(8.dp))
                Text(inlineMarkdown(rawLine.trimStart().removePrefix(">").trimStart(), fade), color = Color.White.copy(alpha = .72f), fontStyle = FontStyle.Italic, modifier = Modifier.weight(1f))
            }
            else -> Text(inlineMarkdown(rawLine, fade), color = Color.White.copy(alpha = .9f), lineHeight = 21.sp)
        }
            index++
        }
    }
}

private fun markdownHeading(line: String): Pair<Int, String>? {
    val match = Regex("^\\s*(#{1,6})\\s+(.+?)\\s*#*\\s*$").find(line) ?: return null
    return match.groupValues[1].length to match.groupValues[2]
}

private fun isMarkdownDivider(line: String): Boolean {
    val compact = line.replace(" ", "")
    return compact.length >= 3 && compact.all { it == '-' } || compact.length >= 3 && compact.all { it == '*' } || compact.length >= 3 && compact.all { it == '_' }
}

private fun isMarkdownTableDivider(line: String): Boolean =
    line.contains('|') && line.trim('|', ' ').split('|').all { it.trim().matches(Regex(":?-{3,}:?")) }

@Composable
private fun MarkdownTableRow(line: String, trailingAlpha: Float) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        line.trim('|').split('|').forEach { cell ->
            Text(inlineMarkdown(cell.trim(), trailingAlpha), color = Color.White.copy(alpha = .88f), fontSize = 12.sp, modifier = Modifier.weight(1f))
        }
    }
}

internal fun inlineMarkdown(source: String, trailingAlpha: Float = 1f) = buildAnnotatedString {
    var index = 0
    while (index < source.length) {
        when {
            source[index] == '\\' && index + 1 < source.length -> { append(source[index + 1]); index += 2 }
            source.startsWith("**", index) || source.startsWith("__", index) -> {
                val marker = source.substring(index, index + 2)
                val end = source.indexOf(marker, index + 2)
                if (end > index) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold)); append(source.substring(index + 2, end)); pop(); index = end + 2
                } else { append(source[index]); index++ }
            }
            source.startsWith("~~", index) -> {
                val end = source.indexOf("~~", index + 2)
                if (end > index) {
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)); append(source.substring(index + 2, end)); pop(); index = end + 2
                } else { append(source[index]); index++ }
            }
            source[index] == '`' -> {
                val end = source.indexOf('`', index + 1)
                if (end > index) {
                    pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color.Black.copy(alpha = .24f))); append(source.substring(index + 1, end)); pop(); index = end + 1
                } else { append(source[index]); index++ }
            }
            source[index] == '*' || source[index] == '_' -> {
                val marker = source[index]
                val end = source.indexOf(marker, index + 1)
                if (end > index) {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic)); append(source.substring(index + 1, end)); pop(); index = end + 1
                } else { append(source[index]); index++ }
            }
            source[index] == '[' -> {
                val labelEnd = source.indexOf(']', index + 1)
                val urlStart = if (labelEnd >= 0 && labelEnd + 1 < source.length && source[labelEnd + 1] == '(') labelEnd + 2 else -1
                val urlEnd = if (urlStart >= 0) source.indexOf(')', urlStart) else -1
                if (urlEnd > urlStart) {
                    val start = length
                    pushStyle(SpanStyle(color = Color(0xFFB9DCFF), textDecoration = TextDecoration.Underline)); append(source.substring(index + 1, labelEnd)); pop()
                    addStringAnnotation("URL", source.substring(urlStart, urlEnd), start, length)
                    index = urlEnd + 1
                } else { append(source[index]); index++ }
            }
            source[index] == '!' && index + 1 < source.length && source[index + 1] == '[' -> {
                val labelEnd = source.indexOf(']', index + 2)
                val urlEnd = if (labelEnd >= 0 && labelEnd + 1 < source.length && source[labelEnd + 1] == '(') source.indexOf(')', labelEnd + 2) else -1
                if (urlEnd > labelEnd) { append("图片：${source.substring(index + 2, labelEnd)}"); index = urlEnd + 1 } else { append(source[index]); index++ }
            }
            source[index] == '<' -> {
                val end = source.indexOf('>', index + 1)
                if (end > index && source.substring(index + 1, end).let { it.startsWith("http://") || it.startsWith("https://") }) {
                    val url = source.substring(index + 1, end)
                    val start = length
                    pushStyle(SpanStyle(color = Color(0xFFB9DCFF), textDecoration = TextDecoration.Underline)); append(url); pop()
                    addStringAnnotation("URL", url, start, length)
                    index = end + 1
                } else { append(source[index]); index++ }
            }
            else -> { append(source[index]); index++ }
        }
    }
    if (length > 0 && trailingAlpha < 1f) {
        addStyle(
            // 仅改变绘制透明度，不改变基线或字号，避免最后一行逐字出现时重新测量而抖动。
            SpanStyle(color = Color.White.copy(alpha = trailingAlpha)),
            length - 1,
            length,
        )
    }
}

@Composable
private fun RainCard(
    hourly: List<HourlyForecast>,
    latitude: Double?,
    longitude: Double?,
    country: String,
    mapProvider: String,
    amapKey: String,
    petalKey: String,
    baiduAk: String,
) {
    val entries = hourly.take(24)
    val values = entries.map { (it.precipitationProbability ?: 0.0).toFloat() }
    val amounts = entries.map { (it.precipitationAmount ?: 0.0).toFloat().coerceAtLeast(0f) }
    var selectedRainIndex by remember(entries) { mutableStateOf<Int?>(null) }
    SectionCard("降雨趋势与云图") {
        Text("折线：降雨概率 · 柱状：预计降雨量（mm）", color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
        if (values.isEmpty()) {
            Text("当前 API 未提供降雨数据", color = Color.White)
        } else {
            Box(Modifier.fillMaxWidth().height(168.dp)) {
            Canvas(
                Modifier.fillMaxSize().padding(top = 12.dp).pointerInput(entries) {
                    fun selectAt(x: Float) {
                        if (entries.isNotEmpty()) {
                            val left = 28.dp.toPx()
                            val right = 5.dp.toPx()
                            val width = (size.width - left - right).coerceAtLeast(1f)
                            selectedRainIndex = ((x - left) / width * entries.lastIndex).roundToInt().coerceIn(entries.indices)
                        }
                    }
                    detectDragGesturesAfterLongPress(
                        onDragStart = { selectAt(it.x) },
                        onDrag = { change, _ -> selectAt(change.position.x); change.consume() },
                        onDragEnd = { selectedRainIndex = null },
                        onDragCancel = { selectedRainIndex = null },
                    )
                },
            ) {
                val left = 28.dp.toPx()
                val right = 5.dp.toPx()
                val top = 13.dp.toPx()
                val bottom = 8.dp.toPx()
                val plotWidth = (size.width - left - right).coerceAtLeast(1f)
                val plotHeight = (size.height - top - bottom).coerceAtLeast(1f)
                val step = if (values.size <= 1) plotWidth else plotWidth / (values.size - 1)
                val labelPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(155, 255, 255, 255)
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
                listOf(0, 50, 100).forEach { probability ->
                    val y = top + plotHeight * (1f - probability / 100f)
                    drawLine(Color.White.copy(alpha = .16f), Offset(left, y), Offset(size.width - right, y), 1.5f)
                    drawIntoCanvas { it.nativeCanvas.drawText("$probability%", left - 4.dp.toPx(), y + 3.dp.toPx(), labelPaint) }
                }
                val maxAmount = (amounts.maxOrNull() ?: 0f).coerceAtLeast(.1f)
                val amountPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(205, 220, 242, 255)
                    textSize = 8.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                amounts.forEachIndexed { index, amount ->
                    if (amount <= 0f) return@forEachIndexed
                    val x = left + index * step
                    val barHeight = amount / maxAmount * plotHeight * .72f
                    val barWidth = minOf(8.dp.toPx(), step * .62f)
                    drawRoundRect(
                        Color(0xFF79BCE8).copy(alpha = .52f),
                        Offset(x - barWidth / 2f, top + plotHeight - barHeight),
                        Size(barWidth, barHeight),
                        CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    )
                    drawIntoCanvas {
                        it.nativeCanvas.drawText(
                            if (amount < 10f) "%.1f".format(amount) else amount.roundToInt().toString(),
                            x,
                            (top + plotHeight - barHeight - 3.dp.toPx()).coerceAtLeast(amountPaint.textSize),
                            amountPaint,
                        )
                    }
                }
                values.zipWithNext().forEachIndexed { index, (first, second) ->
                    val a = Offset(left + index * step, top + plotHeight * (1f - first / 100f))
                    val b = Offset(left + (index + 1) * step, top + plotHeight * (1f - second / 100f))
                    drawLine(Color(0xFFECF8FF), a, b, 4f, StrokeCap.Round)
                }
                selectedRainIndex?.let { index ->
                    val x = left + index * step
                    val y = top + plotHeight * (1f - values[index] / 100f)
                    drawLine(Color.White.copy(alpha = .55f), Offset(x, top), Offset(x, top + plotHeight), 1.5f)
                    drawCircle(Color.White, 4.dp.toPx(), Offset(x, y))
                }
            }
            selectedRainIndex?.let { index ->
                val probability = values.getOrNull(index) ?: return@let
                Text(
                    "${entries[index].time.takeLast(5)} · ${probability.roundToInt()}%",
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.TopCenter).background(Color(0xCC26384A), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            }
            val timeIndexes = listOf(0, entries.size / 4, entries.size / 2, entries.size * 3 / 4, entries.lastIndex)
                .filter { it in entries.indices }
                .distinct()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                timeIndexes.forEach { index ->
                    Text(
                        if (index == 0) "现在" else entries[index].time.takeLast(5),
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 10.sp,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            RadarMap(latitude, longitude, country, mapProvider, amapKey, petalKey, baiduAk)
        }
    }
}

private data class RadarFrame(
    val bitmap: Bitmap?,
    val observedAt: String = "",
    val baseMapName: String = "OpenStreetMap",
    val error: String? = null,
)

// RainViewer's 512px coordinate image covers one tile at its requested zoom,
// while a 512px standard basemap viewport covers two 256px tiles. Advancing
// the basemap by one zoom level makes both layers cover the same world area.
private const val RAIN_RADAR_ZOOM = 5
private const val RADAR_BASE_MAP_ZOOM = RAIN_RADAR_ZOOM + 1

@Composable
private fun RadarMap(latitude: Double?, longitude: Double?, country: String, mapProvider: String, amapKey: String, petalKey: String, baiduAk: String) {
    if (latitude == null || longitude == null) {
        Text("当前城市缺少坐标，无法加载降雨云图", color = Color.White.copy(alpha = .7f), fontSize = 11.sp)
        return
    }
    val context = LocalContext.current
    val baiduMcode = remember(context) { appSigningMcode(context) }
    val client = remember {
        OkHttpClient.Builder()
            .cache(Cache(File(context.cacheDir, "weather-map-tiles"), 24L * 1024 * 1024))
            .addInterceptor(ApiUsageInterceptor())
            .build()
    }
    val resolvedProvider = when (mapProvider) {
        "AMAP" -> if (amapKey.isNotBlank()) "AMAP" else "OSM"
        "PETAL" -> if (petalKey.isNotBlank()) "PETAL" else "OSM"
        "BAIDU" -> if (baiduAk.isNotBlank()) "BAIDU" else "OSM"
        "OSM" -> "OSM"
        else -> if (isChinaRegion(country, latitude, longitude) && amapKey.isNotBlank()) "AMAP" else "OSM"
    }
    val frame by produceState<RadarFrame?>(null, latitude, longitude, client, resolvedProvider, amapKey, petalKey, baiduAk, baiduMcode) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val metadataRequest = Request.Builder()
                    .url("https://api.rainviewer.com/public/weather-maps.json")
                    .header("User-Agent", "NekoWeather/0.6.0 (com.neko.weather)")
                    .build()
                val metadata = client.newCall(metadataRequest).execute().use { response ->
                    check(response.isSuccessful) { "元数据请求失败：${response.code}" }
                    JSONObject(response.body?.string().orEmpty())
                }
                val radar = metadata.getJSONObject("radar").getJSONArray("past")
                check(radar.length() > 0) { "暂时没有可用雷达帧" }
                val latest = radar.getJSONObject(radar.length() - 1)
                val host = metadata.getString("host")
                val path = latest.getString("path")
                val imageUrl = "$host$path/512/$RAIN_RADAR_ZOOM/$latitude/$longitude/2/1_1.png"
                val imageRequest = Request.Builder().url(imageUrl)
                    .header("User-Agent", "NekoWeather/0.6.0 (com.neko.weather)").build()
                val radarBitmap = client.newCall(imageRequest).execute().use { response ->
                    check(response.isSuccessful) { "雷达图请求失败：${response.code}" }
                    response.body?.byteStream()?.use(BitmapFactory::decodeStream)
                }
                checkNotNull(radarBitmap) { "雷达图解码失败" }
                val bitmap = when (resolvedProvider) {
                    "AMAP" -> composeAmapRadarMap(client, latitude, longitude, amapKey, radarBitmap)
                    "PETAL" -> composeTileRadarMap(client, latitude, longitude, radarBitmap, petalKey)
                    "BAIDU" -> composeBaiduRadarMap(client, latitude, longitude, baiduAk, baiduMcode, radarBitmap)
                    else -> composeTileRadarMap(client, latitude, longitude, radarBitmap)
                }
                val time = Instant.ofEpochSecond(latest.getLong("time"))
                    .atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
                val baseName = when (resolvedProvider) { "AMAP" -> "高德地图"; "PETAL" -> "花瓣地图"; "BAIDU" -> "百度地图"; else -> "OpenStreetMap" }
                RadarFrame(bitmap, time, baseName)
            }.getOrElse {
                val baseName = when (resolvedProvider) { "AMAP" -> "高德地图"; "PETAL" -> "花瓣地图"; "BAIDU" -> "百度地图"; else -> "OpenStreetMap" }
                RadarFrame(null, baseMapName = baseName, error = it.message ?: "加载失败")
            }
        }
    }
    Text("降雨云图", color = Color.White, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
    when (val current = frame) {
        null -> Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
        }
        else -> if (current.bitmap != null) {
            Image(
                bitmap = current.bitmap.asImageBitmap(),
                contentDescription = "当前城市降雨云图",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(210.dp).background(Color(0xFF203443), RoundedCornerShape(14.dp)),
            )
            Text(
                when (current.baseMapName) {
                    "高德地图" -> "雷达：RainViewer · 底图：高德地图 · ${current.observedAt}"
                    "花瓣地图" -> "雷达：RainViewer · 底图：花瓣地图 / Huawei Map Kit · ${current.observedAt}"
                    "百度地图" -> "雷达：RainViewer · 底图：百度地图 · ${current.observedAt}"
                    else -> "雷达：RainViewer · 地图：© OpenStreetMap contributors · ${current.observedAt}"
                },
                color = Color.White.copy(alpha = .68f), fontSize = 10.sp,
            )
        } else {
            Text("降雨云图暂不可用：${current.error}", color = Color.White.copy(alpha = .7f), fontSize = 11.sp)
        }
    }
}

private fun composeTileRadarMap(client: OkHttpClient, latitude: Double, longitude: Double, radar: Bitmap, petalKey: String? = null): Bitmap {
    val zoom = RADAR_BASE_MAP_ZOOM
    val tileSize = 256
    val outputSize = 512
    val worldTiles = 1 shl zoom
    val safeLatitude = latitude.coerceIn(-85.0511, 85.0511)
    val latitudeRadians = Math.toRadians(safeLatitude)
    val centerTileX = (longitude + 180.0) / 360.0 * worldTiles
    val centerTileY = (1.0 - kotlin.math.ln(kotlin.math.tan(latitudeRadians) + 1.0 / kotlin.math.cos(latitudeRadians)) / Math.PI) / 2.0 * worldTiles
    val leftPixel = centerTileX * tileSize - outputSize / 2.0
    val topPixel = centerTileY * tileSize - outputSize / 2.0
    val firstTileX = kotlin.math.floor(leftPixel / tileSize).toInt()
    val lastTileX = kotlin.math.floor((leftPixel + outputSize - 1) / tileSize).toInt()
    val firstTileY = kotlin.math.floor(topPixel / tileSize).toInt()
    val lastTileY = kotlin.math.floor((topPixel + outputSize - 1) / tileSize).toInt()
    val output = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(output)
    canvas.drawColor(android.graphics.Color.rgb(225, 232, 236))
    for (tileY in firstTileY..lastTileY) for (tileX in firstTileX..lastTileX) {
        if (tileY !in 0 until worldTiles) continue
        val wrappedX = ((tileX % worldTiles) + worldTiles) % worldTiles
        val tileUrl = if (petalKey.isNullOrBlank()) {
            "https://tile.openstreetmap.org/$zoom/$wrappedX/$tileY.png".toHttpUrl()
        } else {
            "https://mapapi.cloud.huawei.com/mapApi/v1/mapService/getTile".toHttpUrl().newBuilder()
                .addQueryParameter("x", wrappedX.toString()).addQueryParameter("y", tileY.toString())
                .addQueryParameter("z", zoom.toString()).addQueryParameter("language", "zh")
                .addQueryParameter("scale", "1").addQueryParameter("key", petalKey).build()
        }
        val request = Request.Builder()
            .url(tileUrl)
            .header("User-Agent", "NekoWeather/0.6.0 (com.neko.weather)")
            .build()
        val tile = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                if (!petalKey.isNullOrBlank()) error("花瓣地图瓦片请求失败：${response.code}")
                null
            } else {
                val bytes = response.body?.bytes() ?: ByteArray(0)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: if (!petalKey.isNullOrBlank()) {
                    val message = runCatching {
                        val json = JSONObject(String(bytes))
                        json.optString("returnDesc").ifBlank { json.optString("message") }
                    }.getOrNull()
                    error(message?.takeIf(String::isNotBlank) ?: "花瓣地图瓦片解码失败，请检查 Map Kit API Key")
                } else null
            }
        } ?: continue
        val drawX = (tileX * tileSize - leftPixel).toFloat()
        val drawY = (tileY * tileSize - topPixel).toFloat()
        canvas.drawBitmap(tile, drawX, drawY, null)
        tile.recycle()
    }
    val radarDestination = android.graphics.Rect(0, 0, outputSize, outputSize)
    canvas.drawBitmap(radar, null, radarDestination, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG))
    radar.recycle()
    val markerPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
    canvas.drawCircle(outputSize / 2f, outputSize / 2f, 8f, markerPaint)
    markerPaint.color = android.graphics.Color.rgb(211, 52, 52)
    canvas.drawCircle(outputSize / 2f, outputSize / 2f, 5f, markerPaint)
    return output
}

private fun composeAmapRadarMap(client: OkHttpClient, latitude: Double, longitude: Double, key: String, radar: Bitmap): Bitmap {
    val coordinateUrl = "https://restapi.amap.com/v3/assistant/coordinate/convert".toHttpUrl().newBuilder()
        .addQueryParameter("locations", "${"%.6f".format(Locale.US, longitude)},${"%.6f".format(Locale.US, latitude)}")
        .addQueryParameter("coordsys", "gps")
        .addQueryParameter("output", "json")
        .addQueryParameter("key", key)
        .build()
    val converted = client.newCall(Request.Builder().url(coordinateUrl).build()).execute().use { response ->
        check(response.isSuccessful) { "高德坐标转换失败：${response.code}" }
        val root = JSONObject(response.body?.string().orEmpty())
        check(root.optString("status") == "1") { root.optString("info", "高德坐标转换失败") }
        root.getString("locations").split(',').also { check(it.size >= 2) { "高德返回了无效坐标" } }
    }
    val staticMapUrl = "https://restapi.amap.com/v3/staticmap".toHttpUrl().newBuilder()
        .addQueryParameter("location", "${converted[0]},${converted[1]}")
        .addQueryParameter("zoom", RADAR_BASE_MAP_ZOOM.toString())
        .addQueryParameter("size", "512*512")
        .addQueryParameter("scale", "1")
        .addQueryParameter("traffic", "0")
        .addQueryParameter("key", key)
        .build()
    val baseMap = client.newCall(Request.Builder().url(staticMapUrl).build()).execute().use { response ->
        check(response.isSuccessful) { "高德静态地图请求失败：${response.code}" }
        val bytes = response.body?.bytes() ?: ByteArray(0)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: run {
            val message = runCatching { JSONObject(String(bytes)).optString("info") }.getOrNull()
            error(message?.takeIf(String::isNotBlank) ?: "高德静态地图解码失败，请检查 Web 服务 Key")
        }
    }
    val output = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(output)
    val destination = android.graphics.Rect(0, 0, 512, 512)
    canvas.drawBitmap(baseMap, null, destination, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG))
    canvas.drawBitmap(radar, null, destination, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG))
    baseMap.recycle()
    radar.recycle()
    val markerPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
    canvas.drawCircle(256f, 256f, 8f, markerPaint)
    markerPaint.color = android.graphics.Color.rgb(211, 52, 52)
    canvas.drawCircle(256f, 256f, 5f, markerPaint)
    return output
}

private fun composeBaiduRadarMap(client: OkHttpClient, latitude: Double, longitude: Double, ak: String, mcode: String?, radar: Bitmap): Bitmap {
    val staticMapUrl = "https://api.map.baidu.com/staticimage/v2".toHttpUrl().newBuilder()
        .addQueryParameter("ak", ak)
        .apply { if (!mcode.isNullOrBlank()) addQueryParameter("mcode", mcode) }
        .addQueryParameter("center", "${"%.6f".format(Locale.US, longitude)},${"%.6f".format(Locale.US, latitude)}")
        .addQueryParameter("coordtype", "wgs84ll")
        .addQueryParameter("width", "512")
        .addQueryParameter("height", "512")
        .addQueryParameter("zoom", RADAR_BASE_MAP_ZOOM.toString())
        .addQueryParameter("scale", "1")
        .build()
    val baseMap = client.newCall(Request.Builder().url(staticMapUrl).build()).execute().use { response ->
        val bytes = response.body?.bytes() ?: ByteArray(0)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: run {
            val details = runCatching {
                val json = JSONObject(String(bytes))
                json.optInt("status", -1) to json.optString("message").ifBlank { json.optString("msg") }
            }.getOrNull()
            val message = when (details?.first) {
                240 -> "百度 AK 未启用静态图 API（状态码 240），请在百度地图控制台编辑应用并启用该服务"
                102 -> "百度 Android AK 缺少 mcode（状态码 102），请确认控制台 SHA1 与应用实际签名一致"
                else -> details?.second?.takeIf(String::isNotBlank)
                    ?: "百度静态地图请求失败：HTTP ${response.code}，请检查 AK 与服务权限"
            }
            error(message)
        }
    }
    val output = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(output)
    val destination = android.graphics.Rect(0, 0, 512, 512)
    canvas.drawBitmap(baseMap, null, destination, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG))
    canvas.drawBitmap(radar, null, destination, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG))
    baseMap.recycle()
    radar.recycle()
    val markerPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
    canvas.drawCircle(256f, 256f, 8f, markerPaint)
    markerPaint.color = android.graphics.Color.rgb(211, 52, 52)
    canvas.drawCircle(256f, 256f, 5f, markerPaint)
    return output
}

@Suppress("DEPRECATION")
private fun appSigningMcode(context: Context): String? = runCatching {
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
    } else {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
    }
    val signature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.signingInfo?.let { signingInfo ->
            if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners.firstOrNull()
            else signingInfo.signingCertificateHistory.firstOrNull()
        }
    } else {
        packageInfo.signatures?.firstOrNull()
    } ?: return@runCatching null
    val sha1 = java.security.MessageDigest.getInstance("SHA-1")
        .digest(signature.toByteArray())
        .joinToString(":") { byte -> "%02X".format(Locale.US, byte.toInt() and 0xFF) }
    "$sha1;${context.packageName}"
}.getOrNull()

private fun isChinaRegion(country: String, latitude: Double, longitude: Double): Boolean {
    val normalized = country.trim().lowercase(Locale.ROOT)
    if (normalized.isNotBlank()) {
        return listOf("中国", "china", "cn", "台湾", "taiwan", "香港", "hong kong", "澳门", "macao", "macau")
            .any { normalized == it || normalized.contains(it) }
    }
    return latitude in 3.5..53.8 && longitude in 73.0..135.5
}

@Composable
private fun AlertsCard(weather: WeatherDashboard) {
    var expanded by rememberSaveable(weather.alerts.map { it.title to it.expiresAt }) { mutableStateOf(false) }
    SectionCard("气象灾害预警") {
        if (weather.alerts.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(Color(0xFF9FE1B0), CircleShape))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        if (weather.alertsChecked) "暂无生效预警" else "预警数据不可用",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(weather.alertStatusMessage, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }
        } else {
            Column(
                Modifier.fillMaxWidth().animateContentSize(animationSpec = tween(320)),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val visibleAlerts = if (expanded) weather.alerts else if (weather.alerts.size > 1) weather.alerts.take(3) else weather.alerts.take(1)
                visibleAlerts.forEachIndexed { index, alert ->
                    Text("${alert.severity} · ${alert.title}", color = Color.White, fontWeight = FontWeight.Bold)
                    if (expanded || weather.alerts.size == 1) {
                        Text(
                            alert.description,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = if (expanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    AnimatedVisibility(
                        visible = expanded,
                        enter = fadeIn(tween(220)),
                        exit = fadeOut(tween(140)),
                    ) {
                        Text("来源：${alert.source}", color = Color.White.copy(alpha = 0.58f), fontSize = 10.sp)
                    }
                    if (expanded && index != weather.alerts.lastIndex) HorizontalDivider(color = Divider, modifier = Modifier.padding(vertical = 4.dp))
                }
                if (!expanded && weather.alerts.size > 3) {
                    Text("另有 ${weather.alerts.size - 3} 条生效预警", color = Color.White.copy(alpha = .62f), fontSize = 11.sp)
                }
                val arrowRotation by animateFloatAsState(if (expanded) 270f else 90f, tween(300), label = "alertExpandArrow")
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(if (expanded) "收起" else "展开", color = Color.White)
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = arrowRotation },
                    )
                }
            }
        }
    }
}

@Composable
private fun CityPage(
    state: MainUiState,
    viewModel: MainViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.locateCurrentCity() else viewModel.locationPermissionDenied()
    }

    LaunchedEffect(query) {
        delay(350)
        viewModel.searchCities(query)
    }

    fun select(city: CityLocation) {
        viewModel.selectCity(city)
        onClose()
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("搜索全球城市") },
            placeholder = { Text("城市、行政区或邮政编码") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            singleLine = true,
        )

        if (query.isNotBlank()) {
            Text("搜索结果", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            when {
                state.citySearching -> Box(Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                query.length < 2 -> Text("请至少输入两个字符", color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.citySearchResults.isEmpty() -> Text("没有找到匹配城市", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> state.citySearchResults.forEach { city ->
                    CityResultRow(city, selected = false, onClick = { select(city) })
                }
            }
            Spacer(Modifier.height(8.dp))
            return@Column
        }

        Card(
            modifier = Modifier.fillMaxWidth().clickable {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    viewModel.locateCurrentCity()
                } else {
                    permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.MyLocation, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("定位当前城市", fontWeight = FontWeight.Bold)
                    Text("仅在点击时获取前台位置", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (state.locating) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        }

        if (state.savedCities.isNotEmpty()) {
            Text("我的城市", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Card(Modifier.fillMaxWidth()) {
                state.savedCities.forEachIndexed { index, city ->
                    Row(
                        Modifier.fillMaxWidth().clickable { select(city) }.padding(start = 16.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (city.isCurrentLocation) Icons.Outlined.MyLocation else Icons.Outlined.LocationCity,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(city.name, fontWeight = FontWeight.Medium)
                            if (city.subtitle.isNotBlank()) Text(city.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("主页", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Switch(
                                checked = city.showOnHome,
                                onCheckedChange = { viewModel.setCityShownOnHome(city.id, it) },
                                modifier = Modifier.size(48.dp, 32.dp),
                            )
                        }
                        if (state.savedCities.size > 1) {
                            IconButton(onClick = { viewModel.deleteCity(city.id) }) { Icon(Icons.Default.Delete, "删除城市") }
                        }
                    }
                    if (index != state.savedCities.lastIndex) HorizontalDivider()
                }
            }
            Text("打开“主页”开关后，可在天气主页的大温度区域左右滑动切换。", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Text("热门城市", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        HOT_CITIES.chunked(3).forEach { rowCities ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowCities.forEach { city ->
                    OutlinedButton(onClick = { select(city) }, modifier = Modifier.weight(1f)) {
                        Text(city.name, maxLines = 1)
                    }
                }
                repeat(3 - rowCities.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Text(
            "全球城市由 Open-Meteo Geocoding 提供，输入中文、英文城市名或邮政编码即可搜索。",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun CityResultRow(city: CityLocation, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (city.isCurrentLocation) Icons.Outlined.MyLocation else Icons.Outlined.LocationOn,
            null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(city.name, fontWeight = FontWeight.Medium)
            if (city.subtitle.isNotBlank()) Text(city.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "${"%.2f".format(city.latitude)}, ${"%.2f".format(city.longitude)}",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider()
}

@Composable
private fun SettingsPage(
    state: MainUiState,
    viewModel: MainViewModel,
    openApi: () -> Unit,
    openVoice: () -> Unit,
    openAbout: () -> Unit,
    openAppearance: () -> Unit,
    openAi: () -> Unit,
    openNotifications: () -> Unit,
    openBackup: () -> Unit,
    openDisplayAndRefresh: () -> Unit,
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SettingsCard(
            icon = Icons.Outlined.Cloud,
            title = "内容供应商",
            subtitle = if (state.config.endpoint.contains("open-meteo.com")) {
                "Open-Meteo · ${if (state.config.airQualityStandard == "EU") "AQI（欧标）" else "AQI（美标）"}"
            } else {
                "自定义 API · ${state.config.displayName}"
            },
            onClick = openApi,
        )
        SettingsCard(
            icon = Icons.Default.GraphicEq,
            title = "TTS 与音色",
            subtitle = "系统 TTS · 已导入 ${state.importedModels.size} 个离线模型",
            onClick = openVoice,
        )
        SettingsCard(
            icon = Icons.Outlined.Palette,
            title = "外观、皮肤与 Live2D",
            subtitle = "字体、Logo、背景及模型导入",
            onClick = openAppearance,
        )
        SettingsCard(
            icon = Icons.Outlined.LightMode,
            title = "AI 建议",
            subtitle = if (state.aiSettings.apiKey.isBlank()) "Anthropic 兼容 API · 尚未配置" else state.aiSettings.model,
            onClick = openAi,
        )
        SettingsCard(Icons.Outlined.Notifications, "通知提醒", "每日天气、周报、降雨和灾害预警", openNotifications)
        SettingsCard(Icons.Outlined.Info, "备份与还原", "选择配置类别并导出 ZIP 备份", openBackup)
        SettingsCard(Icons.Default.Refresh, "显示与天气更新", "温度、风速单位与自动更新", openDisplayAndRefresh)
        SettingsCard(Icons.Outlined.Info, "关于", "版本信息、开源仓库与检查更新", openAbout)
    }
}

@Composable
private fun DisplayAndRefreshPage(state: MainUiState, viewModel: MainViewModel, modifier: Modifier = Modifier) {
    var refreshInterval by remember(state.refreshSettings.intervalMinutes) { mutableIntStateOf(state.refreshSettings.intervalMinutes) }
    var freshnessMinutes by remember(state.refreshSettings.freshnessMinutes) { mutableIntStateOf(state.refreshSettings.freshnessMinutes) }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("温度单位", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TemperatureUnit.entries.forEach { unit ->
                val selected = state.displaySettings.temperatureUnit == unit
                OutlinedButton(
                    onClick = { viewModel.updateDisplaySettings(state.displaySettings.copy(temperatureUnit = unit)) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                ) { Text(when (unit) { TemperatureUnit.CELSIUS -> "摄氏"; TemperatureUnit.FAHRENHEIT -> "华氏"; TemperatureUnit.KELVIN -> "开尔文" }, fontSize = 12.sp) }
            }
        }
        Text("风速单位", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WindSpeedUnit.entries.forEach { unit ->
                val selected = state.displaySettings.windSpeedUnit == unit
                OutlinedButton(
                    onClick = { viewModel.updateDisplaySettings(state.displaySettings.copy(windSpeedUnit = unit)) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
                ) { Text(if (unit == WindSpeedUnit.METRES_PER_SECOND) "m/s" else "km/h") }
            }
        }
        Text("天气更新", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text("自动更新天气", Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Switch(
                        checked = state.refreshSettings.enabled,
                        onCheckedChange = { viewModel.updateWeatherRefreshSettings(state.refreshSettings.copy(enabled = it)) },
                    )
                }
                AnimatedVisibility(state.refreshSettings.enabled) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        Text("每 $refreshInterval 分钟更新", fontSize = 13.sp)
                        Slider(
                            value = refreshInterval.toFloat(),
                            onValueChange = { refreshInterval = ((it / 5f).roundToInt() * 5).coerceIn(5, 120) },
                            onValueChangeFinished = { viewModel.updateWeatherRefreshSettings(state.refreshSettings.copy(intervalMinutes = refreshInterval)) },
                            valueRange = 5f..120f,
                            steps = 22,
                        )
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("近期数据不重复更新", Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Switch(
                        checked = state.refreshSettings.freshnessGuardEnabled,
                        onCheckedChange = { viewModel.updateWeatherRefreshSettings(state.refreshSettings.copy(freshnessGuardEnabled = it)) },
                    )
                }
                AnimatedVisibility(state.refreshSettings.freshnessGuardEnabled) {
                    Column {
                        Text("有效期 $freshnessMinutes 分钟", fontSize = 13.sp)
                        Slider(
                            value = freshnessMinutes.toFloat(),
                            onValueChange = { freshnessMinutes = it.roundToInt().coerceIn(1, 120) },
                            onValueChangeFinished = { viewModel.updateWeatherRefreshSettings(state.refreshSettings.copy(freshnessMinutes = freshnessMinutes)) },
                            valueRange = 1f..120f,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupRestorePage(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    var categories by remember { mutableStateOf(BackupCategories.all) }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri -> uri?.let { viewModel.backup(it, categories) } }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::restore) }
    val options = listOf(
        BackupCategories.SETTINGS to "设置、城市、供应商、AI 与语音配置",
        BackupCategories.APPEARANCE to "皮肤、背景、Logo 与 Live2D 模型文件",
        BackupCategories.API_USAGE to "API 请求统计与配额",
        BackupCategories.NOTIFICATION_HISTORY to "通知发送历史",
    )
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("API Key 和 AI Key 可能包含在“设置”备份中，请妥善保存备份文件。", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        options.forEach { (key, label) ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, Modifier.weight(1f))
                    Switch(key in categories, { enabled -> categories = if (enabled) categories + key else categories - key })
                }
            }
        }
        Button(onClick = { create.launch("NekoWeather-backup-${LocalDate.now()}.zip") }, enabled = categories.isNotEmpty(), modifier = Modifier.fillMaxWidth()) { Text("导出所选内容") }
        OutlinedButton(onClick = { open.launch(arrayOf("application/zip", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) { Text("选择备份文件并还原") }
    }
}

@Composable
private fun NotificationSettingsPage(state: MainUiState, viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun requestPermissionIfNeeded(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NotificationSwitch("每日早间天气", state.notificationSettings.morningEnabled) {
            viewModel.updateNotificationSettings(state.notificationSettings.copy(morningEnabled = it)); requestPermissionIfNeeded(it)
        }
        NotificationSwitch("周日下周速览", state.notificationSettings.weeklyEnabled) {
            viewModel.updateNotificationSettings(state.notificationSettings.copy(weeklyEnabled = it)); requestPermissionIfNeeded(it)
        }
        NotificationSwitch("降雨提醒", state.notificationSettings.rainEnabled) {
            viewModel.updateNotificationSettings(state.notificationSettings.copy(rainEnabled = it)); requestPermissionIfNeeded(it)
        }
        NotificationSwitch("气象灾害预警", state.notificationSettings.alertEnabled) {
            viewModel.updateNotificationSettings(state.notificationSettings.copy(alertEnabled = it)); requestPermissionIfNeeded(it)
        }
    }
}

@Composable
private fun NotificationSwitch(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Notifications, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(title, Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Switch(checked, onChecked)
        }
    }
}

@Composable
private fun SettingsCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AboutPage(state: MainUiState, viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var openDocument by remember { mutableStateOf<Pair<String, String>?>(null) }
    var documentLanguage by remember { mutableStateOf(state.displaySettings.language) }
    val version = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.1.0"
    }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(Icons.Outlined.WbSunny, null, tint = NekoBlue, modifier = Modifier.size(72.dp))
        Text("NekoWeather", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("猫猫风雨", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("版本 $version", color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        Button(onClick = viewModel::checkUpdate, enabled = !state.checkingUpdate, modifier = Modifier.fillMaxWidth()) {
            if (state.checkingUpdate) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("检查更新")
        }
        OutlinedButton(
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/FatPanda8885/NekoWeather"))) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("查看开源仓库") }
        OutlinedButton(
            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://nekoweather.12am.moe"))) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("应用官网") }
        HorizontalDivider()
        listOf(
            "用户使用协议" to "terms_of_use.md",
            "隐私政策" to "privacy_policy.md",
            "第三方内容使用说明" to "third_party_notice.md",
            "GNU GPLv3 开源协议" to "gpl_v3.txt",
        ).forEach { document ->
            OutlinedButton(onClick = {
                documentLanguage = if (document.second.endsWith(".md")) state.displaySettings.language else AppLanguage.ENGLISH
                openDocument = document
            }, modifier = Modifier.fillMaxWidth()) {
                Text(document.first)
            }
        }
    }
    state.updateInfo?.let { info ->
        AlertDialog(
            onDismissRequest = viewModel::dismissUpdate,
            title = { Text(if (info.updateAvailable) "发现新版本 ${info.version}" else "已是最新版本") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("来源：${info.source}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text(info.changelog)
                }
            },
            confirmButton = {
                if (info.updateAvailable) TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl ?: info.pageUrl)))
                    viewModel.dismissUpdate()
                }) { Text("立即更新") }
                else TextButton(onClick = viewModel::dismissUpdate) { Text("确定") }
            },
            dismissButton = if (info.updateAvailable) ({ TextButton(onClick = viewModel::dismissUpdate) { Text("稍后") } }) else null,
        )
    }
    openDocument?.let { (title, assetName) ->
        val translatable = assetName.endsWith(".md")
        val localizedAsset = legalDocumentAsset(assetName, if (translatable) documentLanguage else AppLanguage.ENGLISH)
        val document = remember(localizedAsset) {
            runCatching { context.assets.open(localizedAsset).bufferedReader().use { it.readText() } }
                .getOrElse { "文件读取失败：${it.message}" }
        }
        val documentScroll = rememberScrollState()
        LaunchedEffect(localizedAsset) { documentScroll.scrollTo(0) }
        AlertDialog(
            onDismissRequest = { openDocument = null },
            title = {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    if (translatable) LegalLanguageDropdown(documentLanguage) { documentLanguage = it }
                    else Text("English", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            },
            text = {
                Box(
                    Modifier.fillMaxWidth().height(520.dp).clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF20252D)).verticalScroll(documentScroll).padding(14.dp),
                ) {
                    CompositionLocalProvider(LocalAppLanguage provides AppLanguage.SIMPLIFIED_CHINESE) {
                        MarkdownText(document)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { openDocument = null }) { Text("关闭") } },
        )
    }
}

private data class PendingImageEdit(val uri: Uri, val kind: String, val title: String)

@Composable
private fun ImageCropDialog(
    pending: PendingImageEdit,
    onDismiss: () -> Unit,
    onConfirm: (Bitmap) -> Unit,
) {
    val context = LocalContext.current
    val source by produceState<Bitmap?>(null, pending.uri) {
        value = withContext(Dispatchers.IO) { decodeEditableBitmap(context, pending.uri) }
    }
    var zoom by remember(pending.uri) { mutableStateOf(1f) }
    var rotation by remember(pending.uri) { mutableStateOf(0f) }
    var offset by remember(pending.uri) { mutableStateOf(Offset.Zero) }
    var previewSize by remember(pending.uri) { mutableStateOf(IntSize.Zero) }
    val isBackground = pending.kind == "background" || pending.kind.startsWith("weather_")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(pending.title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("拖动图片调整裁切区域，双指可缩放和旋转。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(
                    modifier = (if (isBackground) Modifier.width(190.dp).aspectRatio(9f / 16f)
                    else if (pending.kind == "logo") Modifier.fillMaxWidth().aspectRatio(2f)
                    else Modifier.width(260.dp).aspectRatio(1f))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF20252D))
                        .onGloballyPositioned { previewSize = it.size }
                        .pointerInput(pending.uri) {
                            detectTransformGestures { _, pan, gestureZoom, gestureRotation ->
                                offset += pan
                                zoom = (zoom * gestureZoom).coerceIn(1f, 5f)
                                rotation += gestureRotation
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    source?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "图片裁切预览",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().graphicsLayer {
                                scaleX = zoom
                                scaleY = zoom
                                rotationZ = rotation
                                translationX = offset.x
                                translationY = offset.y
                            },
                        )
                    } ?: CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { rotation = (rotation + 90f) % 360f }) {
                        Icon(Icons.AutoMirrored.Filled.RotateRight, "顺时针旋转 90 度")
                    }
                    Text("缩放", fontSize = 12.sp)
                    Slider(zoom, { zoom = it }, valueRange = 1f..5f, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = source != null && previewSize.width > 0 && previewSize.height > 0,
                onClick = {
                    val bitmap = source ?: return@TextButton
                    onConfirm(
                        renderCroppedBitmap(
                            source = bitmap,
                            kind = pending.kind,
                            rotation = rotation,
                            zoom = zoom,
                            offsetXFraction = offset.x / previewSize.width,
                            offsetYFraction = offset.y / previewSize.height,
                        ),
                    )
                },
            ) { Text("应用") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun decodeEditableBitmap(context: Context, uri: Uri): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}.getOrNull()

private fun renderCroppedBitmap(
    source: Bitmap,
    kind: String,
    rotation: Float,
    zoom: Float,
    offsetXFraction: Float,
    offsetYFraction: Float,
): Bitmap {
    val (width, height) = when {
        kind == "logo" -> 1200 to 600
        kind == "appicon" -> 1024 to 1024
        else -> 1080 to 1920
    }
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(output)
    canvas.drawColor(if (kind == "logo" || kind == "appicon") android.graphics.Color.TRANSPARENT else android.graphics.Color.BLACK)
    val quarterTurn = ((rotation % 180f) + 180f) % 180f in 45f..135f
    val rotatedWidth = if (quarterTurn) source.height.toFloat() else source.width.toFloat()
    val rotatedHeight = if (quarterTurn) source.width.toFloat() else source.height.toFloat()
    val baseScale = kotlin.math.max(width / rotatedWidth, height / rotatedHeight)
    canvas.save()
    canvas.translate(width / 2f + offsetXFraction * width, height / 2f + offsetYFraction * height)
    canvas.rotate(rotation)
    canvas.scale(baseScale * zoom, baseScale * zoom)
    canvas.drawBitmap(
        source,
        -source.width / 2f,
        -source.height / 2f,
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG),
    )
    canvas.restore()
    return output
}

@Composable
private fun AppearancePage(
    state: MainUiState,
    viewModel: MainViewModel,
    onOpenEditor: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingImageEdit by remember { mutableStateOf<PendingImageEdit?>(null) }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    val fontPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { uri -> viewModel.importCustomization(uri, "font") } }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { pendingImageEdit = PendingImageEdit(it, "logo", "裁切主页 Logo") }
    }
    val appIconPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { pendingImageEdit = PendingImageEdit(it, "appicon", "裁切桌面图标") }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { pendingImageEdit = PendingImageEdit(it, "background", "裁切通用背景") }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { uri -> viewModel.importCustomization(uri, "video") } }
    val live2dPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(viewModel::importLive2d) }
    var pendingWeatherBackground by remember { mutableStateOf("weather_clear") }
    val weatherBackgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { pendingImageEdit = PendingImageEdit(it, pendingWeatherBackground, "裁切天气背景") }
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f))) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("语言", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Box {
                    OutlinedButton(onClick = { languageMenuExpanded = true }) {
                        Text(when (state.displaySettings.language) {
                            AppLanguage.SIMPLIFIED_CHINESE -> "简体中文"
                            AppLanguage.TRADITIONAL_CHINESE -> "繁體中文"
                            AppLanguage.ENGLISH -> "English"
                            AppLanguage.JAPANESE -> "日本語"
                        })
                    }
                    DropdownMenu(languageMenuExpanded, { languageMenuExpanded = false }) {
                        listOf(
                            AppLanguage.SIMPLIFIED_CHINESE to "简体中文",
                            AppLanguage.TRADITIONAL_CHINESE to "繁體中文",
                            AppLanguage.ENGLISH to "English",
                            AppLanguage.JAPANESE to "日本語",
                        ).forEach { (language, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.updateDisplaySettings(state.displaySettings.copy(language = language))
                                    languageMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
        Text("皮肤资源", fontWeight = FontWeight.Bold, fontSize = 19.sp)
        Text("资源保存在应用私有目录，可分别替换字体、Logo 和主界面背景。视频背景会静音循环播放。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        ImportOptionRow(if (state.customization.fontPath.isBlank()) "导入字体" else "更换字体（已导入）", state.customization.fontPath.isNotBlank(), { fontPicker.launch(arrayOf("font/ttf", "font/otf", "application/octet-stream")) }) { viewModel.resetCustomizationResource("font") }
        ImportOptionRow(if (state.customization.logoPath.isBlank()) "导入主页 Logo" else "更换主页 Logo", state.customization.logoPath.isNotBlank(), { logoPicker.launch(arrayOf("image/*")) }) { viewModel.resetCustomizationResource("logo") }
        ImportOptionRow(if (state.customization.appIconPath.isBlank()) "导入桌面应用图标" else "更换桌面应用图标", state.customization.appIconPath.isNotBlank(), { appIconPicker.launch(arrayOf("image/*")) }) { viewModel.resetCustomizationResource("appicon") }
        if (state.customization.appIconPath.isNotBlank()) {
            Button(viewModel::createDesktopShortcut, Modifier.fillMaxWidth()) { Text("添加自定义图标到桌面") }
            Text("Android 不允许应用直接替换已安装入口图标；这里会创建指向本应用的自定义桌面快捷方式。", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ImportOptionRow("选择静态背景", state.customization.backgroundPath.isNotBlank() && !state.customization.backgroundIsVideo, { imagePicker.launch(arrayOf("image/*")) }) { viewModel.resetCustomizationResource("background") }
        ImportOptionRow("选择视频动态背景", state.customization.backgroundPath.isNotBlank() && state.customization.backgroundIsVideo, { videoPicker.launch(arrayOf("video/mp4", "video/webm")) }) { viewModel.resetCustomizationResource("background") }
        Text("按天气覆盖背景", fontWeight = FontWeight.SemiBold)
        listOf("clear" to "晴天", "cloudy" to "多云/阴天", "rain" to "雨天", "snow" to "雪天", "fog" to "雾天", "thunder" to "雷暴").chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (key, label) ->
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = {
                            pendingWeatherBackground = "weather_$key"
                            weatherBackgroundPicker.launch(arrayOf("image/*"))
                        }, modifier = Modifier.weight(1f)) { Text(if (state.customization.weatherBackgrounds[key].isNullOrBlank()) label else "$label ✓", fontSize = 12.sp) }
                        val hasCustomBackground = !state.customization.weatherBackgrounds[key].isNullOrBlank()
                        IconButton(
                            onClick = { viewModel.resetCustomizationResource("weather_$key") },
                            enabled = hasCustomBackground,
                        ) {
                            Icon(Icons.Default.Refresh, "恢复默认")
                        }
                    }
                }
            }
        }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Live2D", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Switch(
                checked = state.customization.live2dEnabled,
                onCheckedChange = viewModel::setLive2dEnabled,
            )
        }
        Text("实验性功能，可能会造成卡顿", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Cubism 完整运行时已内置", fontWeight = FontWeight.SemiBold)
                Text(CubismRuntime.displayName, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(
                    "支持动作、表情、物理、呼吸、眨眼、触摸视线与点击反馈。模型资源仍单独导入，不会打进安装包。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
        Text("可导入多个 ZIP 模型包并随时切换。必须包含 .model3.json、.moc3 和纹理；preview.png 为可选列表封面。", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        OutlinedButton({ live2dPicker.launch(arrayOf("application/zip", "application/octet-stream")) }, Modifier.fillMaxWidth()) { Text("导入新模型") }
        state.customization.live2dModels.forEach { model ->
            val selected = model.id == state.customization.selectedLive2dId
            Card(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.selectLive2d(model.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (model.previewPath.isNotBlank()) CustomImage(model.previewPath, Modifier.size(52.dp, 66.dp))
                    else Icon(Icons.Outlined.Person, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(model.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (selected) "当前使用" else "点击切换", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { viewModel.deleteLive2d(model.id) }) { Icon(Icons.Default.Delete, "删除模型") }
                }
            }
        }
        if (state.customization.live2dModelPath.isNotBlank()) {
            Live2dModel(
                modelPath = state.customization.live2dModelPath,
                previewPath = state.customization.live2dPreviewPath,
                modifier = Modifier.align(Alignment.CenterHorizontally).size(190.dp, 230.dp),
            )
            Button(onClick = onOpenEditor, modifier = Modifier.fillMaxWidth()) { Text("可视化调整大小、位置与角度") }
        }
        TextButton(onClick = viewModel::resetCustomization, modifier = Modifier.fillMaxWidth()) { Text("恢复默认外观") }
    }
    pendingImageEdit?.let { pending ->
        ImageCropDialog(
            pending = pending,
            onDismiss = { pendingImageEdit = null },
            onConfirm = { bitmap ->
                viewModel.importEditedImage(bitmap, pending.kind)
                pendingImageEdit = null
            },
        )
    }
}

@Composable
private fun ImportOptionRow(label: String, imported: Boolean, onImport: () -> Unit, onReset: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) { Text(label) }
        IconButton(onClick = onReset, enabled = imported) { Icon(Icons.Default.Refresh, "恢复默认") }
    }
}

@Composable
private fun Live2dLayoutEditor(state: MainUiState, viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val active = state.customization.live2dModels.firstOrNull { it.id == state.customization.selectedLive2dId }
    if (active == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("请先导入并选择 Live2D 模型") }
        return
    }
    var placement by remember(active.id, active.placement) { mutableStateOf(active.placement) }
    val density = LocalDensity.current
    val weather = state.weather
    val isNight = weather?.let { isNightTime(it, System.currentTimeMillis()) } ?: false
    val condition = weather?.current?.condition.orEmpty()
    val colors = if (isNight) listOf(Color(0xFF07182D), Color(0xFF102D4C), Color(0xFF234B6B))
        else listOf(Color(0xFF4779CA), Color(0xFF6A9DDB), Color(0xFFA7CBEA))

    Box(modifier.fillMaxSize().background(Brush.verticalGradient(colors))) {
        CustomBackground(state.customization, condition)
        WeatherSceneLayer(condition, isNight)
        Column(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 16.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(weatherIcon(condition), null, tint = Color.White, modifier = Modifier.size(58.dp))
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text(weather?.current?.temperature?.roundToInt()?.toString() ?: "22", color = Color.White, fontSize = 82.sp, fontWeight = FontWeight.Light)
                Box(
                    Modifier.align(Alignment.CenterEnd)
                        .offset(x = placement.offsetX.dp, y = placement.offsetY.dp)
                        .size(96.dp, 116.dp)
                        .graphicsLayer {
                            scaleX = placement.scale
                            scaleY = placement.scale
                            rotationZ = placement.rotation
                        }
                        .pointerInput(active.id) {
                            detectTransformGestures { _, pan, zoom, rotation ->
                                placement = placement.copy(
                                    offsetX = (placement.offsetX + pan.x / density.density).coerceIn(-500f, 500f),
                                    offsetY = (placement.offsetY + pan.y / density.density).coerceIn(-800f, 800f),
                                    scale = (placement.scale * zoom).coerceIn(.25f, 4f),
                                    rotation = placement.rotation + rotation,
                                )
                            }
                        },
                ) {
                    Live2dModel(active.modelPath, active.previewPath, Modifier.fillMaxSize(), interactionEnabled = false)
                }
            }
            Text(condition.ifBlank { "晴" }, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        }
        Card(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xEFFFFFFF)),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("双指缩放/旋转，单指拖动", fontWeight = FontWeight.Bold)
                Text("位置 ${placement.offsetX.roundToInt()}, ${placement.offsetY.roundToInt()} dp · 缩放 ${"%.2f".format(placement.scale)}× · 角度 ${placement.rotation.roundToInt()}°", fontSize = 12.sp)
                Text("大小", fontSize = 12.sp)
                Slider(placement.scale, { placement = placement.copy(scale = it) }, valueRange = .25f..4f)
                Text("旋转", fontSize = 12.sp)
                Slider(placement.rotation.coerceIn(-180f, 180f), { placement = placement.copy(rotation = it) }, valueRange = -180f..180f)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton({ placement = Live2dPlacement() }, Modifier.weight(1f)) { Text("重置") }
                    Button({ viewModel.updateLive2dPlacement(active.id, placement) }, Modifier.weight(1f)) { Text("保存") }
                }
            }
        }
    }
}

@Composable
private fun AiSettingsPage(state: MainUiState, viewModel: MainViewModel, modifier: Modifier = Modifier) {
    var config by remember(state.aiSettings) { mutableStateOf(state.aiSettings) }
    var maxTokensText by remember(state.aiSettings.maxTokens) { mutableStateOf(state.aiSettings.maxTokens.toString()) }
    var maxCharactersText by remember(state.aiSettings.maxOutputCharacters) { mutableStateOf(state.aiSettings.maxOutputCharacters.toString()) }
    val maxTokensValue = maxTokensText.toIntOrNull()
    val maxCharactersValue = maxCharactersText.toIntOrNull()
    val jsonError = remember(config.rawRequestJson) {
        runCatching {
            val preview = config.rawRequestJson
                .replace("{{model_json}}", org.json.JSONObject.quote(config.model))
                .replace("{{max_tokens}}", config.maxTokens.toString())
                .replace("{{context_json}}", org.json.JSONObject.quote(config.context))
                .replace("{{weather_json}}", org.json.JSONObject.quote("weather"))
            org.json.JSONObject(preview)
        }.exceptionOrNull()?.message
    }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Anthropic Messages 兼容配置", fontWeight = FontWeight.Bold, fontSize = 19.sp)
        Text("请求会同时发送 x-api-key、Authorization Bearer 和 anthropic-version 请求头，以兼容官方 API 与常见网关。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ConfigField("API 地址（HTTPS）", config.endpoint) { config = config.copy(endpoint = it) }
        OutlinedButton(
            onClick = {
                config = config.copy(
                    endpoint = "https://api.deepseek.com/anthropic/v1/messages",
                    model = "deepseek-v4-flash",
                    rawRequestJson = DEFAULT_AI_REQUEST_JSON,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("使用 DeepSeek（Anthropic 兼容）") }
        Text("填入 DeepSeek 基础地址时也会自动补全 /anthropic/v1/messages。", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = config.apiKey,
            onValueChange = { config = config.copy(apiKey = it) },
            label = { Text("API Key") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        ConfigField("模型", config.model) { config = config.copy(model = it) }
        ConfigField("每次最大 Token（64～8192）", maxTokensText) {
            maxTokensText = it.filter(Char::isDigit)
            maxTokensText.toIntOrNull()?.let { value -> config = config.copy(maxTokens = value) }
        }
        if (maxTokensValue == null || maxTokensValue !in 64..8192) Text("请输入 64～8192", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
        ConfigField("最长输出字数（100～10000）", maxCharactersText) {
            maxCharactersText = it.filter(Char::isDigit)
            maxCharactersText.toIntOrNull()?.let { value -> config = config.copy(maxOutputCharacters = value) }
        }
        if (maxCharactersValue == null || maxCharactersValue !in 100..10000) Text("请输入 100～10000", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
        OutlinedTextField(
            value = config.context,
            onValueChange = { config = config.copy(context = it) },
            label = { Text("上下文 / System Prompt") },
            minLines = 3,
            maxLines = 8,
            modifier = Modifier.fillMaxWidth(),
        )
        Text("原始请求 JSON", fontWeight = FontWeight.Bold)
        Text("可用占位符：{{model_json}}、{{max_tokens}}、{{context_json}}、{{weather_json}}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = config.rawRequestJson,
            onValueChange = { config = config.copy(rawRequestJson = it) },
            label = { Text("请求体") },
            minLines = 10,
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            isError = jsonError != null,
            supportingText = { Text(jsonError?.let { "JSON 无效：$it" } ?: "发送前会替换占位符并再次校验") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = { config = config.copy(rawRequestJson = DEFAULT_AI_REQUEST_JSON) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("恢复默认请求 JSON") }
        Button(
            onClick = { viewModel.saveAiSettings(config.copy(maxTokens = maxTokensValue!!, maxOutputCharacters = maxCharactersValue!!)) },
            enabled = config.endpoint.startsWith("https://") && config.model.isNotBlank() && jsonError == null && maxTokensValue in 64..8192 && maxCharactersValue in 100..10000,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("保存 AI 配置") }
        Text("API Key 存储在应用私有 SharedPreferences 中，但尚未使用 Android Keystore 加密；请勿在截图或导出的配置中泄露。", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun QWeatherPresetDialog(state: MainUiState, viewModel: MainViewModel, onDismiss: () -> Unit) {
    var host by remember(state.config.qWeatherApiHost) { mutableStateOf(state.config.qWeatherApiHost) }
    var key by remember(state.config.qWeatherApiKey) { mutableStateOf(state.config.qWeatherApiKey) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("和风天气组合设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("一次配置 GeoAPI、天气、空气质量和气象预警。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ConfigField("API Host（可选）", host) { host = it }
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("和风天气 API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = key.isNotBlank(),
                onClick = { viewModel.enableQWeatherPreset(host, key); onDismiss() },
            ) { Text("保存并启用") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ApiConfigPage(state: MainUiState, viewModel: MainViewModel, openStats: () -> Unit, modifier: Modifier = Modifier) {
    val profiles = state.providerProfiles
    var radarMapProvider by remember(state.config.radarMapProvider) { mutableStateOf(state.config.radarMapProvider) }
    var amapWebKey by remember(state.config.amapWebApiKey) { mutableStateOf(state.config.amapWebApiKey) }
    var petalMapKey by remember(state.config.petalMapApiKey) { mutableStateOf(state.config.petalMapApiKey) }
    var baiduMapAk by remember(state.config.baiduMapAk) { mutableStateOf(state.config.baiduMapAk) }
    val selectedWeather = profiles.weather.first { it.id == profiles.selectedWeatherId }
    val selectedAir = profiles.air.first { it.id == profiles.selectedAirId }
    val selectedAlert = profiles.alerts.first { it.id == profiles.selectedAlertId }
    var weatherEditor by remember(profiles.selectedWeatherId, profiles.weather) { mutableStateOf(selectedWeather) }
    var airEditor by remember(profiles.selectedAirId, profiles.air) { mutableStateOf(selectedAir) }
    var alertEditor by remember(profiles.selectedAlertId, profiles.alerts) { mutableStateOf(selectedAlert) }
    var editWeather by rememberSaveable { mutableStateOf(false) }
    var editAir by rememberSaveable { mutableStateOf(false) }
    var editAlert by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsCard(Icons.Outlined.Info, "API 请求次数统计与限制", "按供应商分别统计日、周、月请求次数", openStats)
        Text("降雨云图底图", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("自动模式在中国大陆使用高德，其他地区使用 OpenStreetMap。也可以手动固定供应商。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("AUTO" to "自动", "AMAP" to "高德", "BAIDU" to "百度", "OSM" to "OSM", "PETAL" to "花瓣")
                .chunked(3).forEach { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowItems.forEach { (value, label) ->
                        OutlinedButton(
                            onClick = { radarMapProvider = value },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (radarMapProvider == value) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            ),
                        ) { Text(label) }
                    }
                    repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        AnimatedVisibility(radarMapProvider in setOf("AUTO", "AMAP"), enter = expandVertically(tween(240)) + fadeIn(), exit = shrinkVertically(tween(180)) + fadeOut()) {
            OutlinedTextField(
                value = amapWebKey,
                onValueChange = { amapWebKey = it },
                label = { Text("高德 Web 服务 API Key") },
                supportingText = { Text("需要在高德控制台申请“Web 服务 API”类型 Key；为空时自动回退 OSM。") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AnimatedVisibility(radarMapProvider == "PETAL", enter = expandVertically(tween(240)) + fadeIn(), exit = shrinkVertically(tween(180)) + fadeOut()) {
            OutlinedTextField(
                value = petalMapKey,
                onValueChange = { petalMapKey = it },
                label = { Text("花瓣地图 Map Kit API Key") },
                supportingText = { Text("需要在 AppGallery Connect 中启用 Map Kit；为空时自动回退 OSM。") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AnimatedVisibility(radarMapProvider == "BAIDU", enter = expandVertically(tween(240)) + fadeIn(), exit = shrinkVertically(tween(180)) + fadeOut()) {
            OutlinedTextField(
                value = baiduMapAk,
                onValueChange = { baiduMapAk = it },
                label = { Text("百度地图静态图 API AK") },
                supportingText = { Text("支持服务端或 Android 类型 AK；Android AK 会自动附带当前签名 mcode。请在控制台启用静态图 API。为空时回退 OSM。") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Button(
            onClick = { viewModel.updateRadarMapProvider(radarMapProvider, amapWebKey, petalMapKey, baiduMapAk) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("保存云图底图设置") }
        HorizontalDivider()
        Text("天气供应商", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("预设", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        profiles.weather.filter { it.builtIn }.forEach { profile ->
            ProviderProfileCard(profile.name, profile.endpoint, profile.id == profiles.selectedWeatherId, true, { viewModel.selectWeatherProvider(profile.id) })
        }
        Text("我的模板", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        profiles.weather.filterNot { it.builtIn }.forEach { profile ->
            ProviderProfileCard(profile.name, profile.endpoint, profile.id == profiles.selectedWeatherId, false, {
                viewModel.selectWeatherProvider(profile.id); editWeather = true
            }, { viewModel.deleteProviderTemplate("weather", profile.id) })
        }
        OutlinedButton(onClick = {
            weatherEditor = WeatherProviderProfile("", "新天气模板", endpoint = "", extraQuery = "")
            editWeather = true
        }, modifier = Modifier.fillMaxWidth()) { Text("新增天气供应商模板") }
        AnimatedVisibility(editWeather && !weatherEditor.builtIn, enter = expandVertically(tween(280)) + fadeIn(), exit = shrinkVertically(tween(220)) + fadeOut()) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ConfigField("模板名称", weatherEditor.name) { weatherEditor = weatherEditor.copy(name = it) }
            ConfigField("API 地址（HTTPS）", weatherEditor.endpoint) { weatherEditor = weatherEditor.copy(endpoint = it) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConfigField("纬度参数", weatherEditor.latitudeParameter, Modifier.weight(1f)) { weatherEditor = weatherEditor.copy(latitudeParameter = it) }
                ConfigField("经度参数", weatherEditor.longitudeParameter, Modifier.weight(1f)) { weatherEditor = weatherEditor.copy(longitudeParameter = it) }
            }
            ConfigField("额外查询参数", weatherEditor.extraQuery) { weatherEditor = weatherEditor.copy(extraQuery = it) }
            ConfigField("API Key 名称", weatherEditor.apiKeyName) { weatherEditor = weatherEditor.copy(apiKeyName = it) }
            OutlinedTextField(
                value = weatherEditor.apiKey,
                onValueChange = { weatherEditor = weatherEditor.copy(apiKey = it) },
                label = { Text("API Key") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("把 API Key 放入请求 Header", Modifier.weight(1f))
                Switch(weatherEditor.apiKeyInHeader, { weatherEditor = weatherEditor.copy(apiKeyInHeader = it) })
            }
            ConfigField("温度 JSON 路径", weatherEditor.temperaturePath) { weatherEditor = weatherEditor.copy(temperaturePath = it) }
            ConfigField("天气状况 JSON 路径", weatherEditor.conditionPath) { weatherEditor = weatherEditor.copy(conditionPath = it) }
            ConfigField("湿度 JSON 路径", weatherEditor.humidityPath) { weatherEditor = weatherEditor.copy(humidityPath = it) }
            ConfigField("风速 JSON 路径", weatherEditor.windSpeedPath) { weatherEditor = weatherEditor.copy(windSpeedPath = it) }
            Button(
                onClick = { viewModel.saveWeatherTemplate(weatherEditor) },
                enabled = weatherEditor.name.isNotBlank() && weatherEditor.endpoint.startsWith("https://") && weatherEditor.temperaturePath.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (weatherEditor.id.isBlank()) "保存并启用模板" else "保存模板修改") }
          }
        }

        HorizontalDivider()
        Text("空气质量供应商", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        profiles.air.forEach { profile ->
            ProviderProfileCard(profile.name, "${profile.endpoint} · ${profile.standard}", profile.id == profiles.selectedAirId, profile.builtIn, {
                viewModel.selectAirProvider(profile.id); editAir = !profile.builtIn
            }, if (profile.builtIn) null else ({ viewModel.deleteProviderTemplate("air", profile.id) }))
        }
        OutlinedButton(onClick = {
            airEditor = AirProviderProfile("", "新空气质量模板", endpoint = "")
            editAir = true
        }, modifier = Modifier.fillMaxWidth()) { Text("新增空气质量供应商模板") }
        AnimatedVisibility(editAir && !airEditor.builtIn, enter = expandVertically(tween(280)) + fadeIn(), exit = shrinkVertically(tween(220)) + fadeOut()) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ConfigField("模板名称", airEditor.name) { airEditor = airEditor.copy(name = it) }
            ConfigField("API 地址（HTTPS）", airEditor.endpoint) { airEditor = airEditor.copy(endpoint = it) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (airEditor.standard == "EU") "AQI 欧标" else "AQI 美标", Modifier.weight(1f))
                Switch(airEditor.standard == "EU", { airEditor = airEditor.copy(standard = if (it) "EU" else "US") })
            }
            Button(onClick = { viewModel.saveAirTemplate(airEditor) }, enabled = airEditor.name.isNotBlank() && airEditor.endpoint.startsWith("https://"), modifier = Modifier.fillMaxWidth()) {
                Text(if (airEditor.id.isBlank()) "保存并启用模板" else "保存模板修改")
            }
          }
        }

        HorizontalDivider()
        Text("气象预警供应商", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        profiles.alerts.forEach { profile ->
            val description = when (profile.mode) {
                "AUTO" -> "按地区自动选择 NWS / DWD"
                "QWEATHER" -> "和风天气全球气象预警"
                "OFF" -> "不请求气象预警数据"
                else -> profile.endpoint
            }
            ProviderProfileCard(profile.name, description, profile.id == profiles.selectedAlertId, profile.builtIn, {
                viewModel.selectAlertProvider(profile.id); editAlert = !profile.builtIn
            }, if (profile.builtIn) null else ({ viewModel.deleteProviderTemplate("alert", profile.id) }))
        }
        OutlinedButton(onClick = {
            alertEditor = AlertProviderProfile("", "新预警模板", mode = "CUSTOM", endpoint = "")
            editAlert = true
        }, modifier = Modifier.fillMaxWidth()) { Text("新增预警供应商模板") }
        AnimatedVisibility(editAlert && !alertEditor.builtIn, enter = expandVertically(tween(280)) + fadeIn(), exit = shrinkVertically(tween(220)) + fadeOut()) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ConfigField("模板名称", alertEditor.name) { alertEditor = alertEditor.copy(name = it) }
            ConfigField("GeoJSON/CAP API 地址（HTTPS）", alertEditor.endpoint) { alertEditor = alertEditor.copy(endpoint = it) }
            Button(onClick = { viewModel.saveAlertTemplate(alertEditor) }, enabled = alertEditor.name.isNotBlank() && alertEditor.endpoint.startsWith("https://"), modifier = Modifier.fillMaxWidth()) {
                Text(if (alertEditor.id.isBlank()) "保存并启用模板" else "保存模板修改")
            }
          }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ApiUsagePage(modifier: Modifier = Modifier) {
    var revision by remember { mutableIntStateOf(0) }
    val summaries = remember(revision) { ApiUsageTracker.summaries() }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("请求在发送前计数；达到任一已开启上限后，该供应商的新请求会被阻止。不同域名和内容类别分别统计。", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (summaries.isEmpty()) Text("尚无 API 请求记录，刷新一次天气后即可看到。")
        summaries.forEach { summary ->
            var dailyEnabled by remember(summary.key, revision) { mutableStateOf(summary.quota.dailyEnabled) }
            var weeklyEnabled by remember(summary.key, revision) { mutableStateOf(summary.quota.weeklyEnabled) }
            var monthlyEnabled by remember(summary.key, revision) { mutableStateOf(summary.quota.monthlyEnabled) }
            var daily by remember(summary.key, revision) { mutableStateOf(summary.quota.daily.toString()) }
            var weekly by remember(summary.key, revision) { mutableStateOf(summary.quota.weekly.toString()) }
            var monthly by remember(summary.key, revision) { mutableStateOf(summary.quota.monthly.toString()) }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(summary.provider, fontWeight = FontWeight.Bold)
                    Text(summary.url, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(summary.category, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Text("今日 ${summary.daily} · 本周 ${summary.weekly} · 本月 ${summary.monthly}")
                    QuotaInput("每日限制", dailyEnabled, { dailyEnabled = it }, daily) { daily = it }
                    QuotaInput("每周限制", weeklyEnabled, { weeklyEnabled = it }, weekly) { weekly = it }
                    QuotaInput("每月限制", monthlyEnabled, { monthlyEnabled = it }, monthly) { monthly = it }
                    Button(onClick = {
                        ApiUsageTracker.saveQuota(summary.key, ApiQuota(
                            dailyEnabled = dailyEnabled,
                            weeklyEnabled = weeklyEnabled,
                            monthlyEnabled = monthlyEnabled,
                            daily = daily.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                            weekly = weekly.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                            monthly = monthly.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                        ))
                        revision++
                    }, Modifier.fillMaxWidth()) { Text("保存限制") }
                }
            }
        }
    }
}

@Composable
private fun QuotaInput(label: String, enabled: Boolean, onEnabled: (Boolean) -> Unit, value: String, onValue: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = enabled, onCheckedChange = onEnabled)
    }
    AnimatedVisibility(enabled) {
        ConfigField("$label（次）", value) { onValue(it.filter(Char::isDigit)) }
    }
}

@Composable
private fun ProviderProfileCard(
    title: String,
    description: String,
    selected: Boolean,
    builtIn: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Text(if (builtIn) "预设" else "模板", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                }
                Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            if (selected) Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            onDelete?.let { IconButton(it) { Icon(Icons.Default.Delete, "删除模板") } }
        }
    }
}

@Composable
private fun ApiProviderCard(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Cloud, null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                Modifier.size(20.dp).background(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ConfigField(label: String, value: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
    )
}

@Composable
private fun VoicePage(state: MainUiState, viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importVoiceModel)
    }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("实验性功能，可能会造成卡顿", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
        Text("系统 TTS 语速：${"%.1f".format(state.voiceSettings.rate)}")
        Slider(state.voiceSettings.rate, { viewModel.updateVoiceSettings(state.voiceSettings.copy(rate = it)) }, valueRange = 0.5f..1.8f)
        Text("系统 TTS 音调：${"%.1f".format(state.voiceSettings.pitch)}")
        Slider(state.voiceSettings.pitch, { viewModel.updateVoiceSettings(state.voiceSettings.copy(pitch = it)) }, valueRange = 0.5f..1.8f)
        HorizontalDivider()
        Text("离线自定义音色", fontWeight = FontWeight.Bold)
        Text("导入 VITS/Piper ONNX ZIP 模型包，根目录需包含 manifest.json、model.onnx、tokens.txt。")
        OutlinedButton(onClick = { importer.launch(arrayOf("application/zip", "application/octet-stream")) }) {
            Text("导入模型包")
        }
        if (state.importedModels.isEmpty()) Text("尚未导入离线音色")
        state.importedModels.forEach { model ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(model.name, fontWeight = FontWeight.Bold)
                        Text("${model.engine} · ${model.language} · ${model.sampleRate} Hz")
                    }
                    IconButton({ viewModel.deleteVoiceModel(model.id) }) { Icon(Icons.Default.Delete, "删除") }
                }
            }
        }
        Text("当前天气朗读使用系统 TTS；ONNX 推理将在 sherpa-onnx 运行库接入后启用。", color = MaterialTheme.colorScheme.error)
    }
}

private fun weatherIcon(condition: String): ImageVector = when {
    "雷" in condition -> Icons.Outlined.Thunderstorm
    "雪" in condition -> Icons.Outlined.AcUnit
    "雨" in condition -> Icons.Outlined.Grain
    "雾" in condition -> Icons.Outlined.Cloud
    "阴" in condition -> Icons.Outlined.Cloud
    "云" in condition -> Icons.Outlined.Cloud
    else -> Icons.Outlined.WbSunny
}

private fun dayLabel(date: String): String {
    return runCatching {
        val parsed = LocalDate.parse(date)
        when (parsed) {
            LocalDate.now().minusDays(1) -> "昨天"
            LocalDate.now() -> "今天"
            LocalDate.now().plusDays(1) -> "明天"
            else -> parsed.format(DateTimeFormatter.ofPattern("M/d E", Locale.CHINA))
        }
    }.getOrDefault(date.takeLast(5))
}

private fun minuteOfDay(value: String?): Int? = runCatching {
    val time = value?.takeLast(5) ?: return null
    val parts = time.split(':')
    parts[0].toInt() * 60 + parts[1].toInt()
}.getOrNull()

private fun isNightTime(weather: WeatherDashboard, clockMillis: Long): Boolean {
    val cityNow = runCatching {
        ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(clockMillis), ZoneId.of(weather.current.timezone))
    }.getOrNull()
    val date = cityNow?.toLocalDate()?.toString() ?: weather.current.observationTime?.take(10)
    val currentMinute = cityNow?.let { it.hour * 60 + it.minute } ?: minuteOfDay(weather.current.observationTime)
    val day = weather.daily.firstOrNull { it.date == date }
    val sunrise = minuteOfDay(day?.sunrise)
    val sunset = minuteOfDay(day?.sunset)
    return if (currentMinute != null && sunrise != null && sunset != null) {
        currentMinute < sunrise || currentMinute >= sunset
    } else {
        (weather.current.solarElevation ?: 1.0) < 0.0
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun windDirection(degrees: Double?): String {
    if (degrees == null) return "—"
    val directions = listOf("北风", "东北风", "东风", "东南风", "南风", "西南风", "西风", "西北风")
    return directions[((degrees + 22.5) / 45.0).toInt() % 8]
}

private fun convertTemperature(celsius: Double, unit: TemperatureUnit): Double = when (unit) {
    TemperatureUnit.CELSIUS -> celsius
    TemperatureUnit.FAHRENHEIT -> celsius * 9.0 / 5.0 + 32.0
    TemperatureUnit.KELVIN -> celsius + 273.15
}

private fun temperatureText(celsius: Double, unit: TemperatureUnit): String {
    val suffix = when (unit) {
        TemperatureUnit.CELSIUS -> "°C"
        TemperatureUnit.FAHRENHEIT -> "°F"
        TemperatureUnit.KELVIN -> " K"
    }
    return "${convertTemperature(celsius, unit).roundToInt()}$suffix"
}

@Composable
private fun temperatureText(celsius: Double): String = temperatureText(celsius, LocalTemperatureUnit.current)

private fun windForceLevel(kilometresPerHour: Double): Int =
    listOf(1.0, 6.0, 12.0, 20.0, 29.0, 39.0, 50.0, 62.0, 75.0, 89.0, 103.0, 118.0)
        .indexOfFirst { kilometresPerHour < it }
        .let { if (it < 0) 12 else it }

private fun windSpeedText(kilometresPerHour: Double, unit: WindSpeedUnit): String = when (unit) {
    WindSpeedUnit.KILOMETRES_PER_HOUR -> "${kilometresPerHour.roundToInt()} km/h"
    WindSpeedUnit.METRES_PER_SECOND -> "${"%.1f".format(Locale.getDefault(), kilometresPerHour / 3.6)} m/s"
}

private fun uvLabel(index: Double): String = when {
    index < 3 -> "弱"
    index < 6 -> "中等"
    index < 8 -> "强"
    index < 11 -> "很强"
    else -> "极强"
}

private fun aqiLabel(aqi: Double, standard: String): String = if (standard == "EU") {
    when {
        aqi <= 20 -> "优"
        aqi <= 40 -> "良"
        aqi <= 60 -> "中等"
        aqi <= 80 -> "较差"
        aqi <= 100 -> "很差"
        else -> "极差"
    }
} else {
    when {
        aqi <= 50 -> "优"
        aqi <= 100 -> "良"
        aqi <= 150 -> "轻度污染"
        aqi <= 200 -> "中度污染"
        aqi <= 300 -> "重度污染"
        else -> "严重污染"
    }
}

private fun isAirUnhealthy(air: com.neko.weather.model.AirQuality): Boolean =
    (air.aqi ?: 0.0) > if (air.standard == "EU") 60.0 else 100.0
