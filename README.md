<p align="center">
  <img src="app/src/main/res/drawable/neko1.jpeg" width="140" alt="NekoWeather">
</p>

<p align="center">
  <strong style="font-size: 2em;">NekoWeather</strong>
  <br>
  <span style="font-size: 1.3em;">猫猫风雨</span>
</p>

<p align="center">
  <em>🚧 The official app icon is currently in production. The image above is a placeholder mascot.</em>
</p>

---

<p align="center">
  <a href="README_zh-CN.md">简体中文</a> ·
  <a href="README_zh-TW.md">繁體中文</a> ·
  <a href="README_ja.md">日本語</a>
</p>

---

**NekoWeather** is a feature-rich, open-source Android weather application built with Kotlin and Jetpack Compose. It combines detailed weather data, interactive maps, AI-powered advice, and Live2D animated characters into a single immersive dashboard. **No account required. No ads. No tracking.**

---

## Features

### 🌤 Weather Dashboard
- Real-time current conditions with animated weather scenes (clouds, rain, snow, sun glow)
- 24-hour hourly forecast with temperature, precipitation probability, and wind
- 15-day daily forecast with highs, lows, conditions, and sunrise/sunset
- Air quality index with PM2.5, PM10, NO₂, O₃, SO₂ (US & EU AQI standards)
- Wind speed, direction, humidity, pressure, visibility, and UV index
- Interactive solar elevation arc chart
- Interactive precipitation probability & rainfall amount charts

### 🗺 Live Rain Radar
- Animated rainfall radar overlay (RainViewer)
- Multiple basemap providers: OpenStreetMap, Amap, Petal Maps (Huawei), Baidu Maps
- Auto-mode: uses Amap in mainland China, OSM elsewhere

### 🤖 AI Weather Advice
- Anthropic Messages API compatible (supports Claude, DeepSeek, and compatible gateways)
- Configurable endpoint, model, system prompt, and output length
- Typewriter-animated AI response with Markdown rendering
- Context-aware: current weather, hourly/daily forecast, air quality, and alerts

### 🎭 Live2D Animated Characters
- Full Cubism SDK 5 runtime integration
- Import multiple Live2D models via ZIP
- Visual layout editor (position, scale, rotation)
- Supports motions, expressions, physics, breathing, blinking, gaze tracking, and tap feedback

### 🎨 Deep Customization
- Custom fonts — replace the system font entirely
- Custom home-screen logo
- Custom static or video backgrounds
- Per-weather-condition backgrounds (thunder, snow, rain, fog, cloudy, clear)
- Custom launcher shortcut icon

### 🔊 TTS & Voice
- System TTS engine for reading weather aloud
- Import custom offline VITS/Piper ONNX voice models

### 🌍 Multi-Provider Architecture
- **Open-Meteo** — default, no API key required
- **QWeather (和风天气)** — optional, with API key
- **Custom templates** — define your own endpoints and JSON path extraction
- Alerts: Auto (NWS/DWD by region), QWeather, custom GeoJSON/CAP, or off

### 📢 Notifications
- Morning daily weather
- Sunday weekly outlook
- Rain probability alerts
- Hazard warning alerts
- Notification history
- Persists across reboots

### 🌐 Multi-Language
Full UI localization in **4 languages**:
- Simplified Chinese (简体中文)
- Traditional Chinese (繁體中文)
- English
- Japanese (日本語)

### 💾 Backup & Restore
- Select categories and export to ZIP
- Restore from backup file
- Covers settings, cities, provider config, AI config, and voice models

### 📊 API Usage Tracking
- Per-provider, per-domain request counting
- Daily, weekly, and monthly usage statistics
- Optional quota limits

### 🏙 City Management
- Global city search powered by Open-Meteo Geocoding
- GPS auto-location
- Multiple saved cities with swipe-to-switch on the home screen

---

## 📜 Legal & License

This project is distributed under the **GNU General Public License v3.0 (GPLv3)**. See [LICENSE](LICENSE) for the full text.

**By using NekoWeather, you agree to the following documents:**

| Document | Description |
|---|---|
| [Terms of Use](app/src/main/assets/terms_of_use.md) | User agreement — disclaimers, user responsibilities, third-party content |
| [Privacy Policy](app/src/main/assets/privacy_policy.md) | How your data is handled — the app collects no personal information |
| [Third-Party Notice](app/src/main/assets/third_party_notice.md) | Licenses and attributions for all third-party components |

> ⚠️ **Important:** NekoWeather is provided **"AS IS"** without warranty of any kind. The developer assumes no liability for the accuracy of weather data, map boundaries, AI-generated content, or any third-party service. Please read the Terms of Use carefully before using the app.

---

## 🌐 Official Website

The official website is currently **under development** and temporarily unavailable.

- Website: [https://nekoweather.12am.moe](https://nekoweather.12am.moe) *(coming soon)*

For the latest updates, please follow the GitHub repository.

---

## 📦 Download

The **only official distribution channels** are:

| Platform | URL |
|---|---|
| **GitHub Releases** | [github.com/FatPanda8885/NekoWeather/releases](https://github.com/FatPanda8885/NekoWeather/releases) |
| **Gitee Releases** | [gitee.com/pasky/NekoWeather/releases](https://gitee.com/pasky/NekoWeather/releases) (mirror for users in China) |

> ⚠️ The developer is only responsible for releases published on these two channels. APKs obtained from any other source (third-party app stores, download sites, forums, etc.) may be tampered with and are **not guaranteed** to be safe or authentic.

---

## 🛠 Build from Source

### Prerequisites

| Tool | Version |
|---|---|
| **Android Studio** | Latest stable |
| **JDK** | 17 |
| **Android SDK** | 35 (compileSdk) |
| **Kotlin** | 2.1.20 |
| **Gradle** | 8.7+ (wrapper included) |

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/FatPanda8885/NekoWeather.git
cd NekoWeather

# 2. (Optional) Set up Live2D Cubism SDK
# Download CubismSdkForJava-5-r.5.zip from https://www.live2d.com/
# Extract to .cubism-sdk/ in the project root
# The SDK is NOT required for basic compilation — the build will skip it if absent

# 3. Build the debug APK
./gradlew assembleDebug

# 4. Or build the release APK (requires signing configuration)
# ./gradlew assembleRelease
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Minimum Requirements
- **Android 8.0 (API 26)** or higher
- **ABI:** arm64-v8a, x86, x86_64

---

## 🏗 Architecture

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose with Material 3
- **Architecture Pattern:** MVVM (ViewModel + StateFlow)
- **HTTP Client:** OkHttp 4
- **Serialization:** Manual JSON parsing with `org.json`
- **Local Storage:** SharedPreferences (via `ConfigStore`)
- **Live2D Integration:** Cubism SDK for Java 5 (optional, conditional compilation)
- **Minification:** ProGuard (release builds)

### Project Structure

```
app/src/main/java/com/neko/weather/
├── MainActivity.kt              # Entry point
├── model/
│   └── Models.kt                # Data models, enums, DTOs
├── data/
│   ├── WeatherRepository.kt     # Weather data fetching & caching
│   ├── AlertRepository.kt       # Hazard alert data
│   ├── AiAdviceRepository.kt    # AI API integration
│   ├── CityRepository.kt        # City search & management
│   ├── ConfigStore.kt           # SharedPreferences persistence
│   ├── ApiUsageTracker.kt       # API call counting & quotas
│   ├── BackupManager.kt         # ZIP backup & restore
│   ├── UpdateRepository.kt      # GitHub/Gitee release check
│   └── ...
├── ui/
│   ├── NekoWeatherApp.kt        # All UI screens (dashboard, settings, etc.)
│   ├── MainViewModel.kt         # Central ViewModel & state management
│   ├── Localization.kt          # 4-language runtime string replacement
│   ├── LocalizedText.kt         # Custom Text() composable with auto-translation
│   ├── Onboarding.kt            # First-launch setup wizard
│   └── LegalDocuments.kt        # Legal document viewer
├── notification/
│   └── WeatherNotificationService.kt
└── tts/
    └── VoiceModelManager.kt     # Offline voice model management
```

---

## 📄 License

```
NekoWeather (猫猫风雨) — An Android weather application
Copyright (C) 2026  NekoWeather Contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
```

See [LICENSE](LICENSE) for the full GPLv3 text.

---

## 🤖 AI Assistance Disclosure

This project was developed with the assistance of AI/LLM tools, including but not limited to:

- **OpenAI Codex**
- **DeepSeek**

These tools were used for code generation, translation, documentation drafting, and development workflow support. All AI-generated content has been reviewed and verified by human developers.

---

## 🙏 Acknowledgments

NekoWeather is built on the shoulders of many excellent open-source projects and services:

- [Open-Meteo](https://open-meteo.com/) — free weather & air quality API
- [RainViewer](https://www.rainviewer.com/) — free rain radar data
- [OpenStreetMap](https://www.openstreetmap.org/) — free map data
- [Jetpack Compose](https://developer.android.com/compose) — modern Android UI toolkit
- [OkHttp](https://square.github.io/okhttp/) — HTTP client
- [Live2D Cubism SDK](https://www.live2d.com/) — animated character runtime

See the [Third-Party Notice](app/src/main/assets/third_party_notice.md) for a complete list.

---

<p align="center">
  <sub>Made with ❤️ · <a href="https://github.com/FatPanda8885/NekoWeather">GitHub</a> · <a href="https://gitee.com/pasky/NekoWeather">Gitee</a></sub>
</p>
