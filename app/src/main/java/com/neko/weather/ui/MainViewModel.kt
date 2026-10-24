package com.neko.weather.ui

import android.app.Application
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import com.neko.weather.MainActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neko.weather.data.ConfigStore
import com.neko.weather.data.LauncherLabelManager
import com.neko.weather.data.CityRepository
import com.neko.weather.data.CityStore
import com.neko.weather.data.DeviceLocationService
import com.neko.weather.data.WeatherRepository
import com.neko.weather.data.WeatherCacheStore
import com.neko.weather.model.ImportedVoiceModel
import com.neko.weather.model.CityLocation
import com.neko.weather.model.VoiceSettings
import com.neko.weather.model.WeatherApiConfig
import com.neko.weather.model.WeatherDashboard
import com.neko.weather.model.NotificationSettings
import com.neko.weather.model.UpdateInfo
import com.neko.weather.data.UpdateRepository
import com.neko.weather.data.CustomizationManager
import com.neko.weather.model.CustomizationSettings
import com.neko.weather.model.Live2dPlacement
import com.neko.weather.model.WeatherRefreshSettings
import com.neko.weather.model.AiModelProfile
import com.neko.weather.model.AiProfiles
import com.neko.weather.model.DisplaySettings
import com.neko.weather.model.TemperatureUnit
import com.neko.weather.data.AiAdviceRepository
import com.neko.weather.data.AiProfileStore
import com.neko.weather.data.ProviderProfileStore
import com.neko.weather.data.BackupManager
import com.neko.weather.data.ApiUsageTracker
import com.neko.weather.data.AppearancePresetStore
import com.neko.weather.model.AppearancePresets
import com.neko.weather.model.ThemeSettings
import com.neko.weather.model.ProviderProfiles
import com.neko.weather.model.WeatherProviderProfile
import com.neko.weather.model.AirProviderProfile
import com.neko.weather.model.AlertProviderProfile
import com.neko.weather.model.defaultProviderProfiles
import com.neko.weather.notification.WeatherNotificationPublisher
import com.neko.weather.notification.WeatherNotificationScheduler
import com.neko.weather.tts.SystemTtsController
import com.neko.weather.tts.VoiceModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.roundToInt

data class MainUiState(
    val config: WeatherApiConfig = WeatherApiConfig(),
    val weather: WeatherDashboard? = null,
    val loading: Boolean = false,
    val message: String? = null,
    val voiceSettings: VoiceSettings = VoiceSettings(),
    val importedModels: List<ImportedVoiceModel> = emptyList(),
    val savedCities: List<CityLocation> = emptyList(),
    val citySearchResults: List<CityLocation> = emptyList(),
    val citySearching: Boolean = false,
    val locating: Boolean = false,
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val checkingUpdate: Boolean = false,
    val updateInfo: UpdateInfo? = null,
    val customization: CustomizationSettings = CustomizationSettings(),
    val refreshSettings: WeatherRefreshSettings = WeatherRefreshSettings(),
    val aiProfiles: AiProfiles = AiProfiles(),
    val aiAdvice: String? = null,
    val aiLoading: Boolean = false,
    val providerProfiles: ProviderProfiles = defaultProviderProfiles(),
    val displaySettings: DisplaySettings = DisplaySettings(),
    val themeSettings: ThemeSettings = ThemeSettings(),
    val appearancePresets: AppearancePresets = AppearancePresets(),
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ConfigStore(application)
    private val repository = WeatherRepository()
    private val weatherCache = WeatherCacheStore(application)
    private val cityRepository = CityRepository()
    private val cityStore = CityStore(application)
    private val locationService = DeviceLocationService(application)
    private val systemTts = SystemTtsController(application)
    private val voiceModels = VoiceModelManager(application)
    private val updateRepository = UpdateRepository()
    private val customizationManager = CustomizationManager(application)
    private val aiAdviceRepository = AiAdviceRepository()
    private val providerProfileStore = ProviderProfileStore(application)
    private val aiProfileStore = AiProfileStore(application)
    private val backupManager = BackupManager(application)
    private val appearancePresetStore = AppearancePresetStore(application)

    private val initialConfig = store.loadWeatherConfig().also {
        ApiUsageTracker.registerProvider(it.endpoint, it.displayName)
    }
    private val initialCities = cityStore.load().ifEmpty {
        cityStore.add(
            CityLocation(
                id = "initial-${initialConfig.latitude}-${initialConfig.longitude}",
                name = initialConfig.locationName,
                latitude = initialConfig.latitude.toDoubleOrNull() ?: 31.2304,
                longitude = initialConfig.longitude.toDoubleOrNull() ?: 121.4737,
            ),
        )
    }
    private val _state = MutableStateFlow(
        MainUiState(
            config = initialConfig,
            weather = weatherCache.load(initialConfig),
            voiceSettings = store.loadVoiceSettings(),
            importedModels = voiceModels.list(),
            savedCities = initialCities,
            notificationSettings = store.loadNotificationSettings(),
            customization = customizationManager.load(),
            refreshSettings = store.loadWeatherRefreshSettings(),
            aiProfiles = aiProfileStore.load(store.loadAiSettings()),
            providerProfiles = providerProfileStore.load(initialConfig),
            displaySettings = store.loadDisplaySettings(),
            themeSettings = store.loadThemeSettings(),
            appearancePresets = appearancePresetStore.load(),
        ),
    )
    val state = _state.asStateFlow()
    private var citySearchJob: Job? = null
    private var weatherRefreshJob: Job? = null
    private var refreshGeneration = 0L

    init {
        systemTts.speakErrorHandler = { code ->
            _state.update {
                it.copy(message = if (code == null) "TTS 播报失败" else "TTS 播报出错（错误码 $code）")
            }
        }
        syncLauncherLabel(_state.value.displaySettings.language)
        refresh()
    }

    fun refresh(force: Boolean = false) {
        if (_state.value.loading && !force) return
        val snapshot = _state.value
        if (!force && snapshot.refreshSettings.freshnessGuardEnabled) {
            val age = System.currentTimeMillis() - (snapshot.weather?.current?.fetchedAtMillis ?: 0L)
            if (snapshot.weather != null && age in 0 until snapshot.refreshSettings.freshnessMinutes * 60_000L) return
        }
        val generation = ++refreshGeneration
        val config = _state.value.config
        weatherRefreshJob?.cancel()
        weatherRefreshJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            runCatching { repository.fetch(config) }
                .onSuccess { weather ->
                    if (generation == refreshGeneration) {
                        store.saveLastWeatherUpdateMillis(weather.current.fetchedAtMillis)
                        weatherCache.save(config, weather)
                        // 前台刷新也走通知判定：部分 ROM 会大幅推迟周期 Job，共用发布器
                        // 与去重键保证使用期间通知照常到期
                        runCatching { WeatherNotificationPublisher.publish(getApplication(), weather) }
                        _state.update { it.copy(weather = weather, loading = false) }
                    }
                }
                .onFailure { error ->
                    if (generation == refreshGeneration && error !is kotlinx.coroutines.CancellationException) {
                        _state.update { it.copy(loading = false, message = error.message ?: "天气查询失败") }
                    }
                }
        }
    }

    fun saveConfig(config: WeatherApiConfig) {
        store.saveWeatherConfig(config)
        ApiUsageTracker.registerProvider(config.endpoint, config.displayName)
        _state.update { it.copy(config = config, message = "API 配置已保存") }
        refresh(force = true)
    }

    fun updateRadarMapProvider(provider: String, amapKey: String, petalKey: String, baiduAk: String) {
        val normalized = provider.takeIf { it in setOf("AUTO", "AMAP", "OSM", "PETAL", "BAIDU") } ?: "AUTO"
        val config = _state.value.config.copy(
            radarMapProvider = normalized,
            amapWebApiKey = amapKey.trim(),
            petalMapApiKey = petalKey.trim(),
            baiduMapAk = baiduAk.trim(),
        )
        store.saveWeatherConfig(config)
        _state.update { it.copy(config = config, message = "云图底图供应商已保存") }
    }

    fun searchCities(query: String) {
        citySearchJob?.cancel()
        if (query.trim().length < 2) {
            _state.update { it.copy(citySearchResults = emptyList(), citySearching = false) }
            return
        }
        citySearchJob = viewModelScope.launch {
            _state.update { it.copy(citySearching = true) }
            runCatching { cityRepository.search(query, _state.value.config) }
                .onSuccess { results -> _state.update { it.copy(citySearchResults = results, citySearching = false) } }
                .onFailure { error ->
                    if (error !is kotlinx.coroutines.CancellationException) {
                        _state.update { it.copy(citySearching = false, message = error.message ?: "城市搜索失败") }
                    }
                }
        }
    }

    fun selectCity(city: CityLocation, promoteInList: Boolean = true) {
        val config = _state.value.config.copy(
            locationName = city.name,
            locationAdmin = city.admin,
            locationCountry = city.country,
            latitude = city.latitude.toString(),
            longitude = city.longitude.toString(),
        )
        store.saveWeatherConfig(config)
        val saved = if (promoteInList) cityStore.add(city) else _state.value.savedCities
        _state.update {
            it.copy(
                config = config,
                aiAdvice = null,
                savedCities = saved,
                citySearchResults = emptyList(),
                message = "已切换到${city.name}",
            )
        }
        refresh(force = true)
    }

    fun switchHomeCity(direction: Int) {
        val currentState = _state.value
        val homeCities = currentState.savedCities.filter(CityLocation::showOnHome)
        if (homeCities.size < 2) {
            _state.update { it.copy(message = "请先在城市列表中选择至少两个主页城市") }
            return
        }
        val currentIndex = homeCities.indexOfFirst { city ->
            kotlin.math.abs(city.latitude - (currentState.config.latitude.toDoubleOrNull() ?: Double.NaN)) < 0.0001 &&
                kotlin.math.abs(city.longitude - (currentState.config.longitude.toDoubleOrNull() ?: Double.NaN)) < 0.0001
        }
        val targetIndex = if (currentIndex < 0) 0 else Math.floorMod(currentIndex + direction, homeCities.size)
        selectCity(homeCities[targetIndex], promoteInList = false)
    }

    fun setCityShownOnHome(id: String, enabled: Boolean) {
        _state.update { it.copy(savedCities = cityStore.setShowOnHome(id, enabled)) }
    }

    fun updateWeatherRefreshSettings(settings: WeatherRefreshSettings) {
        val normalized = settings.copy(
            intervalMinutes = settings.intervalMinutes.coerceIn(5, 120),
            freshnessMinutes = settings.freshnessMinutes.coerceIn(1, 120),
        )
        store.saveWeatherRefreshSettings(normalized)
        _state.update { it.copy(refreshSettings = normalized) }
    }

    fun updateDisplaySettings(settings: DisplaySettings) {
        store.saveDisplaySettings(settings)
        _state.update { it.copy(displaySettings = settings) }
        syncLauncherLabel(settings.language)
        WeatherNotificationScheduler.createChannels(getApplication())
    }

    private fun syncLauncherLabel(language: com.neko.weather.model.AppLanguage) {
        LauncherLabelManager.sync(getApplication(), language)
    }

    fun saveAiProfile(profile: AiModelProfile) {
        val current = _state.value.aiProfiles
        val saved = profile.copy(
            id = profile.id.ifBlank { UUID.randomUUID().toString() },
            maxTokens = profile.maxTokens.coerceIn(64, 8192),
            maxOutputCharacters = profile.maxOutputCharacters.coerceIn(100, 10000),
        )
        val models = if (current.models.any { it.id == saved.id }) {
            current.models.map { if (it.id == saved.id) saved else it }
        } else {
            current.models + saved
        }
        val updated = current.copy(models = models, selectedId = current.selectedId.ifBlank { saved.id })
        aiProfileStore.save(updated)
        _state.update { it.copy(aiProfiles = updated, message = "模型已保存") }
    }

    fun selectAiProfile(id: String) {
        val current = _state.value.aiProfiles
        if (current.models.none { it.id == id }) return
        val updated = current.copy(selectedId = id)
        aiProfileStore.save(updated)
        _state.update { it.copy(aiProfiles = updated) }
    }

    fun deleteAiProfiles(ids: Set<String>) {
        if (ids.isEmpty()) return
        val current = _state.value.aiProfiles
        val models = current.models.filterNot { it.id in ids }
        val selectedId = if (current.selectedId in ids || models.none { it.id == current.selectedId }) {
            models.firstOrNull()?.id.orEmpty()
        } else {
            current.selectedId
        }
        val updated = current.copy(models = models, selectedId = selectedId)
        aiProfileStore.save(updated)
        _state.update { it.copy(aiProfiles = updated, message = "已删除 ${current.models.size - models.size} 个模型") }
    }

    fun renameAiProfile(id: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val current = _state.value.aiProfiles
        val updated = current.copy(models = current.models.map { if (it.id == id) it.copy(name = trimmed) else it })
        aiProfileStore.save(updated)
        _state.update { it.copy(aiProfiles = updated, message = "模型已重命名") }
    }

    fun enableQWeatherPreset(apiHost: String, apiKey: String) {
        val host = apiHost.trim().removePrefix("https://").removePrefix("http://").trimEnd('/').ifBlank { "devapi.qweather.com" }
        if (apiKey.isBlank()) {
            _state.update { it.copy(message = "请填写和风天气 API Key") }
            return
        }
        val profiles = _state.value.providerProfiles.copy(
            selectedWeatherId = "builtin-qweather",
            selectedAirId = "builtin-qweather-air",
            selectedAlertId = "builtin-qweather-alerts",
        )
        val config = _state.value.config.copy(
            displayName = "和风天气",
            endpoint = "https://$host/v7/weather/now",
            latitudeParameter = "", longitudeParameter = "", extraQuery = "",
            temperaturePath = "now.temp", conditionPath = "now.text", humidityPath = "now.humidity", windSpeedPath = "now.windSpeed",
            airQualityEndpoint = "https://$host/airquality/v1/current",
            alertProvider = "QWEATHER", alertEndpoint = "https://$host/weatheralert/v1/current",
            qWeatherApiHost = host, qWeatherApiKey = apiKey.trim(),
        )
        providerProfileStore.save(profiles)
        store.saveWeatherConfig(config)
        _state.update { it.copy(config = config, providerProfiles = profiles, message = "已启用和风天气 Geo、天气、空气质量和预警 API") }
        refresh(force = true)
    }

    fun selectWeatherProvider(id: String) {
        val profiles = _state.value.providerProfiles
        val profile = profiles.weather.firstOrNull { it.id == id } ?: return
        val updated = profiles.copy(selectedWeatherId = id)
        providerProfileStore.save(updated)
        applyWeatherProfile(profile, updated)
    }

    fun saveWeatherTemplate(profile: WeatherProviderProfile) {
        val id = profile.id.ifBlank { "weather-${java.util.UUID.randomUUID()}" }
        val saved = profile.copy(id = id, builtIn = false)
        val current = _state.value.providerProfiles
        val updated = current.copy(
            weather = current.weather.filterNot { it.id == id } + saved,
            selectedWeatherId = id,
        )
        providerProfileStore.save(updated)
        applyWeatherProfile(saved, updated)
    }

    fun selectAirProvider(id: String) {
        val profiles = _state.value.providerProfiles
        val profile = profiles.air.firstOrNull { it.id == id } ?: return
        val updated = profiles.copy(selectedAirId = id)
        providerProfileStore.save(updated)
        applyAirProfile(profile, updated)
    }

    fun saveAirTemplate(profile: AirProviderProfile) {
        val id = profile.id.ifBlank { "air-${java.util.UUID.randomUUID()}" }
        val saved = profile.copy(id = id, builtIn = false)
        val current = _state.value.providerProfiles
        val updated = current.copy(air = current.air.filterNot { it.id == id } + saved, selectedAirId = id)
        providerProfileStore.save(updated)
        applyAirProfile(saved, updated)
    }

    fun selectAlertProvider(id: String) {
        val profiles = _state.value.providerProfiles
        val profile = profiles.alerts.firstOrNull { it.id == id } ?: return
        val updated = profiles.copy(selectedAlertId = id)
        providerProfileStore.save(updated)
        applyAlertProfile(profile, updated)
    }

    fun saveAlertTemplate(profile: AlertProviderProfile) {
        val id = profile.id.ifBlank { "alert-${java.util.UUID.randomUUID()}" }
        val saved = profile.copy(id = id, builtIn = false, mode = "CUSTOM")
        val current = _state.value.providerProfiles
        val updated = current.copy(alerts = current.alerts.filterNot { it.id == id } + saved, selectedAlertId = id)
        providerProfileStore.save(updated)
        applyAlertProfile(saved, updated)
    }

    fun deleteProviderTemplate(category: String, id: String) {
        val current = _state.value.providerProfiles
        when (category) {
            "weather" -> {
                if (current.weather.firstOrNull { it.id == id }?.builtIn != false) return
                val list = current.weather.filterNot { it.id == id }
                val selected = if (current.selectedWeatherId == id) list.first().id else current.selectedWeatherId
                val updated = current.copy(weather = list, selectedWeatherId = selected)
                providerProfileStore.save(updated)
                applyWeatherProfile(list.first { it.id == selected }, updated)
            }
            "air" -> {
                if (current.air.firstOrNull { it.id == id }?.builtIn != false) return
                val list = current.air.filterNot { it.id == id }
                val selected = if (current.selectedAirId == id) list.first().id else current.selectedAirId
                val updated = current.copy(air = list, selectedAirId = selected)
                providerProfileStore.save(updated)
                applyAirProfile(list.first { it.id == selected }, updated)
            }
            "alert" -> {
                if (current.alerts.firstOrNull { it.id == id }?.builtIn != false) return
                val list = current.alerts.filterNot { it.id == id }
                val selected = if (current.selectedAlertId == id) list.first().id else current.selectedAlertId
                val updated = current.copy(alerts = list, selectedAlertId = selected)
                providerProfileStore.save(updated)
                applyAlertProfile(list.first { it.id == selected }, updated)
            }
        }
    }

    private fun applyWeatherProfile(profile: WeatherProviderProfile, profiles: ProviderProfiles) {
        val endpoint = if (profile.id == "builtin-qweather") "https://${_state.value.config.qWeatherApiHost}/v7/weather/now" else profile.endpoint
        val config = _state.value.config.copy(
            displayName = profile.name, endpoint = endpoint,
            latitudeParameter = profile.latitudeParameter, longitudeParameter = profile.longitudeParameter,
            apiKey = profile.apiKey, apiKeyName = profile.apiKeyName, apiKeyInHeader = profile.apiKeyInHeader,
            extraQuery = profile.extraQuery, temperaturePath = profile.temperaturePath,
            conditionPath = profile.conditionPath, humidityPath = profile.humidityPath, windSpeedPath = profile.windSpeedPath,
        )
        store.saveWeatherConfig(config)
        _state.update { it.copy(config = config, providerProfiles = profiles, message = "已切换天气供应商：${profile.name}") }
        refresh(force = true)
    }

    private fun applyAirProfile(profile: AirProviderProfile, profiles: ProviderProfiles) {
        val endpoint = if (profile.id == "builtin-qweather-air") "https://${_state.value.config.qWeatherApiHost}/airquality/v1/current" else profile.endpoint
        val config = _state.value.config.copy(airQualityEndpoint = endpoint, airQualityStandard = profile.standard)
        store.saveWeatherConfig(config)
        _state.update { it.copy(config = config, providerProfiles = profiles, message = "已切换空气质量供应商：${profile.name}") }
        refresh(force = true)
    }

    private fun applyAlertProfile(profile: AlertProviderProfile, profiles: ProviderProfiles) {
        val endpoint = if (profile.id == "builtin-qweather-alerts") "https://${_state.value.config.qWeatherApiHost}/weatheralert/v1/current" else profile.endpoint
        val config = _state.value.config.copy(alertProvider = profile.mode, alertEndpoint = endpoint)
        store.saveWeatherConfig(config)
        _state.update { it.copy(config = config, providerProfiles = profiles, message = "已切换预警供应商：${profile.name}") }
        refresh(force = true)
    }

    fun requestAiAdvice() {
        val weather = _state.value.weather ?: run {
            _state.update { it.copy(message = "请先获取天气数据") }
            return
        }
        if (_state.value.aiLoading) return
        val profile = _state.value.aiProfiles.selected ?: run {
            _state.update { it.copy(message = "请先添加一个 AI 模型") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(aiLoading = true) }
            runCatching { aiAdviceRepository.request(profile, weather) }
                .onSuccess { advice -> _state.update { it.copy(aiLoading = false, aiAdvice = advice) } }
                .onFailure { error -> _state.update { it.copy(aiLoading = false, message = error.message ?: "AI 建议生成失败") } }
        }
    }

    fun clearAiAdvice() {
        _state.update { it.copy(aiAdvice = null) }
    }

    fun deleteCity(id: String) {
        _state.update { it.copy(savedCities = cityStore.delete(id)) }
    }

    fun locateCurrentCity() {
        if (!locationService.hasPermission()) {
            _state.update { it.copy(message = "需要位置权限才能定位城市") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(locating = true) }
            runCatching { locationService.locate() }
                .onSuccess { city ->
                    _state.update { it.copy(locating = false) }
                    selectCity(city)
                }
                .onFailure { error ->
                    _state.update { it.copy(locating = false, message = error.message ?: "定位失败，请检查系统定位服务") }
                }
        }
    }

    /** 引导页只获取一次位置，不写入城市列表或当前天气配置。 */
    fun locateForOnboarding(onLocated: (CityLocation) -> Unit) {
        if (!locationService.hasPermission()) {
            _state.update { it.copy(message = "需要位置权限才能定位城市") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(locating = true) }
            runCatching { locationService.locate() }
                .onSuccess { city ->
                    _state.update { it.copy(locating = false) }
                    onLocated(city)
                }
                .onFailure { error ->
                    _state.update { it.copy(locating = false, message = error.message ?: "定位失败，请检查系统定位服务") }
                }
        }
    }

    fun locationPermissionDenied() {
        _state.update { it.copy(message = "位置权限被拒绝，仍可搜索并选择城市") }
    }

    fun updateVoiceSettings(settings: VoiceSettings) {
        store.saveVoiceSettings(settings)
        _state.update { it.copy(voiceSettings = settings) }
    }

    fun updateNotificationSettings(settings: NotificationSettings) {
        store.saveNotificationSettings(settings)
        WeatherNotificationScheduler.sync(getApplication())
        _state.update { it.copy(notificationSettings = settings) }
    }

    fun checkUpdate() {
        if (_state.value.checkingUpdate) return
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(checkingUpdate = true) }
            val current = getApplication<Application>().packageManager
                .getPackageInfo(getApplication<Application>().packageName, 0).versionName ?: "0.0.0"
            runCatching { updateRepository.check(current) }
                .onSuccess { info -> _state.update { it.copy(checkingUpdate = false, updateInfo = info) } }
                .onFailure { error -> _state.update { it.copy(checkingUpdate = false, message = error.message ?: "检查更新失败") } }
        }
    }

    fun dismissUpdate() = _state.update { it.copy(updateInfo = null) }

    fun importCustomization(uri: android.net.Uri, kind: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { customizationManager.importFile(uri, kind) }
                .onSuccess { value -> _state.update { it.copy(customization = value, message = "外观资源已应用") } }
                .onFailure { error -> _state.update { it.copy(message = error.message ?: "资源导入失败") } }
        }
    }

    fun importEditedImage(bitmap: android.graphics.Bitmap, kind: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { customizationManager.importBitmap(bitmap, kind) }
                .onSuccess { value ->
                    bitmap.recycle()
                    _state.update { it.copy(customization = value, message = "图片已裁切并应用") }
                }
                .onFailure { error ->
                    bitmap.recycle()
                    _state.update { it.copy(message = error.message ?: "图片保存失败") }
                }
        }
    }

    fun resetCustomizationResource(kind: String) {
        runCatching { customizationManager.resetResource(kind) }
            .onSuccess { value -> _state.update { it.copy(customization = value, message = "已恢复默认资源") } }
            .onFailure { error -> _state.update { it.copy(message = error.message ?: "恢复默认失败") } }
    }

    fun createDesktopShortcut() {
        val context = getApplication<Application>()
        val path = _state.value.customization.appIconPath
        val bitmap = BitmapFactory.decodeFile(path)
        val manager = context.getSystemService(ShortcutManager::class.java)
        if (bitmap == null || manager == null || !manager.isRequestPinShortcutSupported) {
            _state.update { it.copy(message = "当前桌面不支持自定义快捷图标") }
            return
        }
        val shortcut = ShortcutInfo.Builder(context, "neko-weather-custom")
            .setShortLabel(LauncherLabelManager.labelFor(_state.value.displaySettings.language))
            .setIcon(Icon.createWithBitmap(bitmap))
            .setIntent(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN))
            .build()
        manager.requestPinShortcut(shortcut, null)
        _state.update { it.copy(message = "已向桌面请求添加自定义图标") }
    }

    fun backup(uri: android.net.Uri, categories: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { backupManager.backup(uri, categories) }
                .onSuccess { _state.update { it.copy(message = "备份已完成") } }
                .onFailure { error -> _state.update { it.copy(message = error.message ?: "备份失败") } }
        }
    }

    fun restore(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { backupManager.restore(uri) }.onSuccess {
                val config = store.loadWeatherConfig()
                _state.update {
                    it.copy(
                        config = config,
                        voiceSettings = store.loadVoiceSettings(),
                        notificationSettings = store.loadNotificationSettings(),
                        customization = customizationManager.load(),
                        refreshSettings = store.loadWeatherRefreshSettings(),
                        aiProfiles = aiProfileStore.load(store.loadAiSettings()),
                        displaySettings = store.loadDisplaySettings(),
                        themeSettings = store.loadThemeSettings(),
                        appearancePresets = appearancePresetStore.load(),
                        savedCities = cityStore.load(),
                        providerProfiles = providerProfileStore.load(config),
                        message = "还原完成",
                    )
                }
                syncLauncherLabel(_state.value.displaySettings.language)
                WeatherNotificationScheduler.sync(getApplication())
            }.onFailure { error -> _state.update { it.copy(message = error.message ?: "还原失败") } }
        }
    }

    fun importLive2d(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { customizationManager.importLive2d(uri) }
                .onSuccess { value -> _state.update { it.copy(customization = value, message = "Live2D 模型已导入") } }
                .onFailure { error -> _state.update { it.copy(message = error.message ?: "Live2D 导入失败") } }
        }
    }

    fun selectLive2d(id: String) {
        runCatching { customizationManager.selectLive2d(id) }
            .onSuccess { value -> _state.update { it.copy(customization = value, message = "已切换 Live2D 模型") } }
            .onFailure { error -> _state.update { it.copy(message = error.message ?: "模型切换失败") } }
    }

    fun deleteLive2d(id: String) {
        _state.update { it.copy(customization = customizationManager.deleteLive2d(id), message = "模型已删除") }
    }

    fun updateLive2dPlacement(id: String, placement: Live2dPlacement) {
        _state.update { it.copy(customization = customizationManager.updateLive2dPlacement(id, placement), message = "模型布局已保存") }
    }

    fun setLive2dHidden(hidden: Boolean) {
        _state.update { it.copy(customization = customizationManager.setLive2dHidden(hidden)) }
    }

    fun setLive2dEnabled(enabled: Boolean) {
        _state.update { it.copy(customization = customizationManager.setLive2dEnabled(enabled)) }
    }

    fun resetCustomization() {
        _state.update {
            it.copy(
                customization = customizationManager.reset(),
                appearancePresets = appearancePresetStore.clearSelection(),
                message = "已恢复默认外观",
            )
        }
    }

    fun updateThemeSettings(settings: ThemeSettings) {
        store.saveThemeSettings(settings)
        _state.update { it.copy(themeSettings = settings) }
    }

    fun applyAppearancePreset(id: String) {
        val preset = _state.value.appearancePresets.presets.firstOrNull { it.id == id } ?: return
        runCatching { appearancePresetStore.apply(preset) }
            .onSuccess { presets ->
                _state.update {
                    it.copy(
                        appearancePresets = presets,
                        customization = customizationManager.load(),
                        message = "已切换外观预设：${preset.name}",
                    )
                }
            }
            .onFailure { error -> _state.update { it.copy(message = error.message ?: "切换预设失败") } }
    }

    /** 以当前生效外观为内容新建预设，返回新预设 ID 供跳转编辑。 */
    fun createAppearancePreset(): String? {
        return runCatching { appearancePresetStore.createFromCurrent("新预设") }
            .onSuccess { presets -> _state.update { it.copy(appearancePresets = presets) } }
            .map { it.presets.lastOrNull()?.id }
            .getOrNull()
    }

    fun renameAppearancePreset(id: String, name: String) {
        _state.update {
            it.copy(appearancePresets = appearancePresetStore.rename(id, name), message = "预设已重命名")
        }
    }

    fun deleteAppearancePresets(ids: Set<String>) {
        if (ids.isEmpty()) return
        val before = _state.value.appearancePresets.presets.size
        runCatching { appearancePresetStore.delete(ids) }
            .onSuccess { presets ->
                _state.update {
                    it.copy(
                        appearancePresets = presets,
                        customization = customizationManager.load(),
                        message = "已删除 ${before - presets.presets.size} 个预设",
                    )
                }
            }
            .onFailure { error -> _state.update { it.copy(message = error.message ?: "删除预设失败") } }
    }

    fun importAppearancePreset(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { appearancePresetStore.importPreset(uri) }
                .onSuccess { presets -> _state.update { it.copy(appearancePresets = presets, message = "预设已导入") } }
                .onFailure { error -> _state.update { it.copy(message = error.message ?: "预设导入失败") } }
        }
    }

    fun exportAppearancePreset(id: String, uri: android.net.Uri) {
        val preset = _state.value.appearancePresets.presets.firstOrNull { it.id == id } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { appearancePresetStore.exportPreset(preset, uri) }
                .onSuccess { _state.update { it.copy(message = "预设已导出") } }
                .onFailure { error -> _state.update { it.copy(message = error.message ?: "预设导出失败") } }
        }
    }

    fun importPresetResource(presetId: String, uri: android.net.Uri, kind: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { appearancePresetStore.importResource(presetId, uri, kind) }
                .onSuccess { presets ->
                    _state.update {
                        it.copy(appearancePresets = presets, customization = customizationManager.load(), message = "外观资源已应用")
                    }
                }
                .onFailure { error -> _state.update { it.copy(message = error.message ?: "资源导入失败") } }
        }
    }

    fun importPresetEditedImage(bitmap: android.graphics.Bitmap, presetId: String, kind: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { appearancePresetStore.importBitmap(presetId, bitmap, kind) }
            result.onSuccess { presets ->
                bitmap.recycle()
                _state.update {
                    it.copy(appearancePresets = presets, customization = customizationManager.load(), message = "图片已裁切并应用")
                }
            }.onFailure { error ->
                bitmap.recycle()
                _state.update { it.copy(message = error.message ?: "图片保存失败") }
            }
        }
    }

    fun resetPresetResource(presetId: String, kind: String) {
        runCatching { appearancePresetStore.resetResource(presetId, kind) }
            .onSuccess { presets ->
                _state.update {
                    it.copy(appearancePresets = presets, customization = customizationManager.load(), message = "已恢复默认资源")
                }
            }
            .onFailure { error -> _state.update { it.copy(message = error.message ?: "恢复默认失败") } }
    }

    fun speakWeather() {
        val weather = _state.value.weather
        if (weather == null) {
            _state.update { it.copy(message = "请先获取天气") }
            return
        }
        val current = weather.current
        val unit = _state.value.displaySettings.temperatureUnit
        val spokenTemperature = when (unit) {
            TemperatureUnit.CELSIUS -> "${current.temperature.roundToInt()}摄氏度"
            TemperatureUnit.FAHRENHEIT -> "${(current.temperature * 9 / 5 + 32).roundToInt()}华氏度"
            TemperatureUnit.KELVIN -> "${(current.temperature + 273.15).roundToInt()}开尔文"
        }
        val text = buildString {
            append(current.location).append("当前").append(current.condition)
            append("，气温").append(spokenTemperature)
            current.humidity?.let { append("，相对湿度百分之").append(it.roundToInt()) }
            current.windSpeed?.let { append("，风速每小时").append(it.roundToInt()).append("公里") }
            append("。")
        }
        if (!systemTts.speak(text, _state.value.voiceSettings)) {
            val failureMessage = when {
                systemTts.isEngineMissing() -> "未检测到系统 TTS 引擎，请在系统设置的语言和输入中启用文字转语音输出"
                else -> "语音播报失败，请检查 TTS 引擎与媒体音量"
            }
            _state.update { it.copy(message = failureMessage) }
        }
    }

    fun importVoiceModel(uri: android.net.Uri) {
        viewModelScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { voiceModels.import(uri) } }
            result.onSuccess { model ->
                _state.update {
                    it.copy(importedModels = voiceModels.list(), message = "已导入音色：${model.name}")
                }
            }.onFailure { error ->
                _state.update { it.copy(message = error.message ?: "模型导入失败") }
            }
        }
    }

    fun deleteVoiceModel(id: String) {
        voiceModels.delete(id)
        _state.update { it.copy(importedModels = voiceModels.list(), message = "模型已删除") }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    override fun onCleared() {
        systemTts.stop()
        systemTts.shutdown()
    }
}

class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(application) as T
}
