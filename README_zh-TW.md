<p align="center">
  <img src="docs/icon.png" width="120" alt="NekoWeather">
</p>

<h1 align="center">貓貓風雨 <sub>NekoWeather</sub></h1>

<p align="center">
  <b>把天氣資料、動態降雨雷達、AI 建議與 Live2D 角色放進同一屏的開源 Android 天氣 App</b><br>
  <sub>Kotlin · Jetpack Compose · Material 3 &nbsp;|&nbsp; 免帳號 · 無廣告 · 無追蹤</sub>
</p>

<p align="center">
  <a href="https://github.com/FatPanda8885/NekoWeather/releases"><img src="https://img.shields.io/github/v/release/FatPanda8885/NekoWeather?label=release&color=blue" alt="release"></a>
  <img src="https://img.shields.io/badge/license-GPL--3.0-orange" alt="license">
  <img src="https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-3DDC84?logo=android&logoColor=white" alt="android">
  <img src="https://img.shields.io/badge/Compose-Material%203-4285F4" alt="compose">
  <img src="https://img.shields.io/badge/i18n-4%20languages-blueviolet" alt="i18n">
  <a href="https://github.com/FatPanda8885/NekoWeather/actions/workflows/ci.yml"><img src="https://github.com/FatPanda8885/NekoWeather/actions/workflows/ci.yml/badge.svg" alt="ci"></a>
</p>

<p align="center">
  <a href="README_zh-CN.md">简体中文</a> ·
  <b>繁體中文</b> ·
  <a href="README_ja.md">日本語</a> ·
  <a href="README.md">English</a>
</p>

<p align="center">
  <img src="docs/screenshots/dashboard.webp" width="240" alt="首頁">
  &nbsp;
  <img src="docs/screenshots/cards-alerts.webp" width="240" alt="警報與雷達">
  &nbsp;
  <img src="docs/screenshots/ai-advice.webp" width="240" alt="AI 建議">
</p>

---

## ✨ 一眼看懂

| 你關心的 | 貓貓風雨怎麼做 |
| --- | --- |
| **隱私** | 免帳號、無廣告、無埋點。設定、城市、金鑰全部存在本機，不向任何自有伺服器回報 |
| **資訊密度** | 一屏給齊：即時天氣 + 24 小時 + 15 日 + 空氣品質 + 日出日落 + 太陽高度弧線 + 降雨機率圖 |
| **資料源自由** | Open-Meteo 免金鑰即可用；可切換和風天氣；也能自填 endpoint + JSON 路徑範本接任意 API |
| **沉浸感** | 以 Cubism SDK 5 驅動的 Live2D 角色：動作、表情、物理、呼吸、眨眼、視線跟隨、點擊回饋，外加可視化佈局編輯器 |
| **可玩性** | 自訂字型 / LOGO / 靜態或影片背景 / 依天氣切換背景 / 啟動圖示 / 離線 VITS-Piper 語音模型 |
| **不綁死** | 4 語言介面、ZIP 備份還原、API 用量統計與配額、GitHub + Gitee 雙通道發佈 |

## 🚀 30 秒上手

1. **安裝**：從 [GitHub Releases](https://github.com/FatPanda8885/NekoWeather/releases)（或 [Gitee 鏡像](https://gitee.com/pasky/NekoWeather/releases)）下載對應 ABI 的 APK。
2. **開啟**：首次啟動選擇介面語言 → 允許定位（也可拒絕，手動搜尋城市同樣可用）。
3. **使用**：首頁下拉更新；右上角依序為 **更新 / 語音播報 / 設定**。

> 預設資料源 Open-Meteo **不需要任何 API Key**，安裝後即可使用。

## 🎯 核心能力

### 🌤 一屏到底的天氣儀表板

即時天氣 + 動態天氣場景（雲、雨、雪、日光）· 24 小時逐時預報（溫度 / 降雨機率 / 風力）· 15 日預報（高低溫、天氣、日出日落）· 空氣品質（PM2.5 / PM10 / NO₂ / O₃ / SO₂，US & EU 雙標準）· 風速風向、濕度、氣壓、能見度、紫外線 · 可互動的太陽高度弧線與降雨機率 / 雨量圖。

<img src="docs/screenshots/dashboard.webp" width="300" alt="首頁">

### 🗺 動態降雨雷達與警報卡片

RainViewer 動畫降雨圖層；底圖可切換 **OpenStreetMap / 高德 / 花瓣地圖（華為）/ 百度**，並提供「中國大陸用高德、其他地區用 OSM」的自動模式。危害警報卡片支援 **自動（依地區選 NWS / DWD）/ 和風 / 自訂 GeoJSON·CAP / 關閉**。

<img src="docs/screenshots/cards-alerts.webp" width="300" alt="警報與雷達">

### 🤖 AI 天氣建議

相容 **Anthropic Messages API** 的服務都能接（Claude、DeepSeek 及各式相容閘道）：端點、模型、系統提示詞、輸出長度皆可設定；回覆具打字機動畫與 Markdown 渲染，並把即時天氣、逐時 / 15 日預報、空氣品質、警報一併作為上下文。

<img src="docs/screenshots/ai-advice.webp" width="300" alt="AI 建議">

### 🎭 Live2D 角色

完整的 **Cubism SDK for Java 5** 執行環境：支援以 ZIP 匯入多個模型、可視化佈局編輯器（位置 / 縮放 / 旋轉），動作、表情、物理、呼吸、眨眼、視線跟隨與點擊回饋全部可用。

### 🌍 資料源與警報

| 類別 | 可選 |
| --- | --- |
| 天氣資料 | Open-Meteo（預設，免金鑰）· 和風天氣（需金鑰）· 自訂 endpoint + JSON 路徑擷取 |
| 危害警報 | 自動（NWS / DWD 依地區）· 和風 · 自訂 GeoJSON / CAP · 關閉 |

<img src="docs/screenshots/providers.webp" width="300" alt="資料源">

### 🎨 深度自訂

整套系統字型替換 · 自訂首頁 LOGO · 靜態或影片背景 · 依天氣條件（雷 / 雪 / 雨 / 霧 / 多雲 / 晴）分別設定背景 · 自訂桌面捷徑圖示。

<img src="docs/screenshots/theme-color.webp" width="300" alt="主題">

### 🔊 語音與 TTS

以系統 TTS 朗讀天氣；可匯入自訂離線 **VITS / Piper ONNX** 語音模型。

### 📢 通知與提醒

每日晨間天氣、週日週報、降雨機率提醒、危害警報提醒，附通知歷史，重新開機後仍有效。

<img src="docs/screenshots/notifications.webp" width="300" alt="通知">

### 💾 備份 · 統計 · 多城市

ZIP 分類備份還原（設定 / 城市 / 資料源 / AI / 語音模型）· 依資料源與網域統計 API 呼叫量（日 / 週 / 月，可設配額）· Open-Meteo 全球城市搜尋、GPS 定位、多城市首頁滑動切換。

<img src="docs/screenshots/settings.webp" width="300" alt="設定">

## 📦 下載

| 管道 | 位址 |
| --- | --- |
| **GitHub Releases** | <https://github.com/FatPanda8885/NekoWeather/releases> |
| **Gitee Releases**（中國大陸鏡像） | <https://gitee.com/pasky/NekoWeather/releases> |

> ⚠️ 以上是**唯二的官方發佈管道**。第三方商店、下載站、論壇提供的 APK 可能被重新打包，不保證安全與正版。

## 🛠 從原始碼建置

| 依賴 | 版本 |
| --- | --- |
| Android Studio | 最新穩定版 |
| JDK | 17 |
| Android SDK | 35（compileSdk） |
| Kotlin | 2.1.20 |
| Gradle | 8.11（wrapper 內附） |

```bash
git clone https://github.com/FatPanda8885/NekoWeather.git
cd NekoWeather

# 選用：Live2D Cubism SDK（缺少時自動退回無 Live2D 的 lite 源集，仍可正常編譯）
# 自 https://www.live2d.com/ 下載 CubismSdkForJava-5-r.5.zip，解壓到 .cubism-sdk/

./gradlew assembleDebug     # 產物：app/build/outputs/apk/debug/
./gradlew assembleRelease   # 需 signing/keystore.properties
```

**執行需求**：Android 8.0（API 26）以上；ABI 支援 arm64-v8a / x86 / x86_64（release 依 ABI 拆分並附通用包）。

## 🏗 技術棧

| 面向 | 選型 |
| --- | --- |
| 語言 / UI | Kotlin 2.1.20 · Jetpack Compose（Material 3） |
| 架構 | MVVM：ViewModel + StateFlow 單一狀態源 |
| 網路 | OkHttp 4 |
| 序列化 | 手寫 JSON 解析（`org.json`）+ JSON 路徑擷取器 |
| 儲存 | SharedPreferences（`ConfigStore` / `WeatherCacheStore` 等） |
| 渲染 | Live2D Cubism SDK for Java 5（選用，條件編譯） |
| 混淆 | ProGuard（release） |

<details>
<summary>專案結構（點擊展開）</summary>

```
app/src/main/java/com/neko/weather/
├── MainActivity.kt              # 入口
├── model/Models.kt              # 資料模型、列舉、DTO
├── data/                        # WeatherRepository / AlertRepository / AiAdviceRepository
│                                # CityRepository / ConfigStore / ApiUsageTracker
│                                # BackupManager / UpdateRepository
├── ui/                          # NekoWeatherApp（全部畫面）/ MainViewModel
│                                # Localization / Onboarding / LegalDocuments
├── notification/WeatherNotificationService.kt
└── tts/VoiceModelManager.kt
```

</details>

## ❓ 常見問題

**需要註冊帳號嗎？** 不需要，也沒有帳號系統。

**會蒐集我的資料嗎？** 不會。App 只在你查看天氣時，向你**自己設定的**資料源與 AI 端點發出請求，其餘資料留在本機。詳見[隱私政策](app/src/main/assets/privacy_policy.md)。

**AI 建議的金鑰存在哪？** 以明文存於本機 SharedPreferences（設定頁有提示）。不想用就不要設定，功能會自動隱藏。

**沒有 Live2D Cubism SDK 能編譯嗎？** 能。缺少 SDK 時建置會自動走 `lite` 源集（無 Live2D 畫面），不會報錯。

**為什麼 release 有多個 APK？** 依 ABI 拆分（arm64-v8a / x86 / x86_64）以縮小體積，另附一個通用包。

**天氣資料準嗎？** 取決於你選的資料源；App 不做二次修正，請以資料源與官方警報為準。

## 📜 授權與法律文件

本專案以 **GNU General Public License v3.0（GPLv3）** 發佈，全文見 [LICENSE](LICENSE)。

| 文件 | 說明 |
| --- | --- |
| [使用者條款](app/src/main/assets/terms_of_use.md) | 免責、使用者責任、第三方內容 |
| [隱私政策](app/src/main/assets/privacy_policy.md) | 資料處理說明：不蒐集個人資訊 |
| [第三方聲明](app/src/main/assets/third_party_notice.md) | 第三方元件授權與署名 |

> ⚠️ **貓貓風雨依「現狀」提供，不附任何擔保。** 開發者不對天氣資料準確性、地圖邊界、AI 生成內容或任何第三方服務負責。

## 🌐 官方網站

<https://nekoweather.12am.moe>（建置中，暫不可用）——最新動態請以本儲存庫為準。

## 🤖 AI 使用揭露

本專案在開發過程中使用 AI / LLM 工具輔助，包括但不限於 **OpenAI Codex** 與 **DeepSeek**，用於程式碼生成、翻譯、文件撰寫與工作流程支援。所有 AI 產出均已由人類開發者審查與驗證。

## 🙏 致謝

- [Open-Meteo](https://open-meteo.com/) — 免費天氣與空氣品質 API
- [RainViewer](https://www.rainviewer.com/) — 免費降雨雷達
- [OpenStreetMap](https://www.openstreetmap.org/) — 免費地圖資料
- [Jetpack Compose](https://developer.android.com/compose) — Android 現代 UI 工具包
- [OkHttp](https://square.github.io/okhttp/) — HTTP 客戶端
- [Live2D Cubism SDK](https://www.live2d.com/) — 角色動畫執行環境

完整清單見[第三方聲明](app/src/main/assets/third_party_notice.md)。

---

<p align="center">
  <sub>Made with ❤️ · <a href="https://github.com/FatPanda8885/NekoWeather">GitHub</a> · <a href="https://gitee.com/pasky/NekoWeather">Gitee</a></sub>
</p>
