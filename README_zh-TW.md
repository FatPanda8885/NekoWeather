<p align="center">
  <img src="app/src/main/res/drawable/neko1.jpeg" width="140" alt="NekoWeather">
</p>

<p align="center">
  <strong style="font-size: 2em;">NekoWeather</strong>
  <br>
  <span style="font-size: 1.3em;">貓貓風雨</span>
</p>

<p align="center">
  <em>🚧 正式應用程式圖示正在製作中，上方為暫用吉祥物圖片。</em>
</p>

---

<p align="center">
  <a href="README.md">English</a> ·
  <a href="README_zh-CN.md">简体中文</a> ·
  <a href="README_ja.md">日本語</a>
</p>

---

**NekoWeather（貓貓風雨）** 是一款功能豐富的開放原始碼 Android 天氣應用程式，使用 Kotlin 和 Jetpack Compose 構建。它將詳盡的天氣資料、互動式地圖、AI 驅動的天氣建議和 Live2D 動態角色模型融為一體，呈現為沉浸式單一儀表板介面。**無需註冊帳號，無廣告，無追蹤。**

---

## 功能特性

### 🌤 天氣儀表板
- 即時天氣狀況，配合動態天氣場景（雲飄、雨落、雪飄、陽光光暈）
- 24 小時每小時預報，包含溫度、降雨機率和風力
- 15 天每日預報，包含最高/最低溫度、天氣狀況和日出/日落
- 空氣品質指數，包含 PM2.5、PM10、NO₂、O₃、SO₂（支援美標/歐標 AQI）
- 風力、風向、濕度、氣壓、能見度和紫外線指數
- 互動式太陽高度角弧線圖
- 互動式降雨機率/預計降雨量圖表

### 🗺 即時降雨雷達
- 動畫降雨雷達疊加層（RainViewer）
- 多種底圖供應商：OpenStreetMap、高德地圖、花瓣地圖（華為）、百度地圖
- 自動模式：中國大陸使用高德地圖，其他地區使用 OSM

### 🤖 AI 天氣建議
- 相容 Anthropic Messages API（支援 Claude、DeepSeek 及相容閘道）
- 可設定端點、模型、系統提示詞和輸出長度
- 打字機動畫效果顯示 AI 回覆，支援 Markdown 渲染
- 情境感知：綜合當前天氣、每小時/每日預報、空氣品質和警報

### 🎭 Live2D 動態角色
- 完整 Cubism SDK 5 執行時期整合
- 支援透過 ZIP 匯入多個 Live2D 模型
- 視覺化版面配置編輯器（位置、大小、旋轉角度）
- 支援動作、表情、物理、呼吸、眨眼、觸控視線和點擊回饋

### 🎨 深度自訂
- 自訂字型 — 完全替換系統字型
- 自訂首頁 Logo
- 自訂靜態圖片或影片動態背景
- 按天氣狀況切換背景（雷、雪、雨、霧、雲、晴）
- 自訂桌面捷徑圖示

### 🔊 TTS 與音色
- 系統 TTS 引擎朗讀天氣
- 匯入自訂離線 VITS/Piper ONNX 語音模型

### 🌍 多供應商架構
- **Open-Meteo** — 預設，無需 API Key
- **和風天氣 (QWeather)** — 可選，需 API Key
- **自訂範本** — 定義自己的端點和 JSON 路徑擷取
- 警報：自動（按地區選擇 NWS/DWD）、和風天氣、自訂 GeoJSON/CAP、或關閉

### 📢 通知提醒
- 每日早晨天氣
- 週日下週預覽
- 降雨機率提醒
- 氣象災害警報
- 通知發送歷史
- 重新啟動後持續執行

### 🌐 多語言
完整 UI 在地化，支援 **4 種語言**：
- 简体中文
- 繁體中文
- English
- 日本語

### 💾 備份與還原
- 選擇設定類別並匯出為 ZIP
- 從備份檔案還原
- 涵蓋設定、城市、供應商設定、AI 設定和語音模型

### 📊 API 請求統計
- 按供應商、網域分別統計
- 每日、每週、每月使用統計
- 可選配額限制

### 🏙 城市管理
- 全球城市搜尋（Open-Meteo Geocoding）
- GPS 自動定位
- 多個已儲存城市，首頁左右滑動切換

---

## 📜 法律與授權

本專案依據 **GNU General Public License v3.0（GPLv3）** 分發。詳見 [LICENSE](LICENSE)。

**使用 NekoWeather 即代表您同意以下檔案：**

| 檔案 | 說明 |
|---|---|
| [使用者使用協議](app/src/main/assets/terms_of_use_zh-TW.md) | 免責聲明、使用者責任、第三方內容 |
| [隱私權政策](app/src/main/assets/privacy_policy_zh-TW.md) | 資料處理方式 — 本應用程式不收集任何個人資訊 |
| [第三方內容使用說明](app/src/main/assets/third_party_notice_zh-TW.md) | 所有第三方元件的授權與歸屬 |

> ⚠️ **重要提示：** NekoWeather 按「現狀」提供，不附帶任何形式的擔保。開發者對天氣資料的準確性、地圖邊界、AI 生成內容或任何第三方服務不承擔任何責任。使用前請仔細閱讀使用者使用協議。

---

## 🌐 官方網站

官方網站正在 **開發中**，暫時無法存取。

- 網址：[https://nekoweather.12am.moe](https://nekoweather.12am.moe) *（即將上線）*

請關注 GitHub 倉庫取得最新動態。

---

## 📦 下載

**唯一官方分發管道：**

| 平台 | 網址 |
|---|---|
| **GitHub Releases** | [github.com/FatPanda8885/NekoWeather/releases](https://github.com/FatPanda8885/NekoWeather/releases) |
| **Gitee Releases** | [gitee.com/pasky/NekoWeather/releases](https://gitee.com/pasky/NekoWeather/releases)（中國境內鏡像） |

> ⚠️ 開發者僅對以上兩個管道發布的版本負責。透過任何其他管道（第三方應用程式商店、下載網站、論壇等）取得的 APK 可能遭到竄改，開發者**不保證**其安全性和真實性。

---

## 🛠 從原始碼構建

### 環境需求

| 工具 | 版本 |
|---|---|
| **Android Studio** | 最新穩定版 |
| **JDK** | 17 |
| **Android SDK** | 35 (compileSdk) |
| **Kotlin** | 2.1.20 |
| **Gradle** | 8.7+（已包含 wrapper） |

### 構建步驟

```bash
# 1. 複製倉庫
git clone https://github.com/FatPanda8885/NekoWeather.git
cd NekoWeather

# 2. （可選）設定 Live2D Cubism SDK
# 從 https://www.live2d.com/ 下載 CubismSdkForJava-5-r.5.zip
# 解壓縮到專案根目錄的 .cubism-sdk/ 資料夾
# 該 SDK 非必須 — 如未設定，構建時會自動跳過

# 3. 構建 Debug APK
./gradlew assembleDebug

# 4. 或構建 Release APK（需要簽章設定）
# ./gradlew assembleRelease
```

APK 位於 `app/build/outputs/apk/debug/app-debug.apk`。

### 最低需求
- **Android 8.0 (API 26)** 或更高
- **ABI：** arm64-v8a、x86、x86_64

---

## 🏗 架構

- **語言：** Kotlin
- **UI 框架：** Jetpack Compose + Material 3
- **架構模式：** MVVM（ViewModel + StateFlow）
- **HTTP 客戶端：** OkHttp 4
- **序列化：** 手動 JSON 解析（org.json）
- **本機儲存：** SharedPreferences（透過 ConfigStore）
- **Live2D 整合：** Cubism SDK for Java 5（可選，條件編譯）
- **程式碼壓縮：** ProGuard（Release 構建）

---

## 📄 開放原始碼授權

```
NekoWeather（貓貓風雨）— Android 天氣應用程式
Copyright (C) 2026  NekoWeather 貢獻者

本程式是自由軟體：您可以依據自由軟體基金會發布的
GNU 通用公眾授權條款（第三版或任意更新版本）的條款，
對其進行再發布和/或修改。

本程式的分發是基於「有用」的目的，但不附帶任何擔保；
甚至不包含對適售性或特定用途適用性的默示擔保。
詳情請參閱 GNU 通用公眾授權條款。
```

詳見 [LICENSE](LICENSE) 取得 GPLv3 完整文字。

---

## 🤖 AI 輔助聲明

本專案在開發過程中使用了 AI/LLM 工具輔助，包括但不限於：

- **OpenAI Codex**
- **DeepSeek**

上述工具用於程式碼生成、翻譯、文檔起草及開發流程支援。所有 AI 生成的內容均已經過人工審核與驗證。

---

## 🙏 致謝

NekoWeather 建立在眾多優秀開放原始碼專案和服務之上：

- [Open-Meteo](https://open-meteo.com/) — 免費天氣與空氣品質 API
- [RainViewer](https://www.rainviewer.com/) — 免費降雨雷達資料
- [OpenStreetMap](https://www.openstreetmap.org/) — 免費地圖資料
- [Jetpack Compose](https://developer.android.com/compose) — 現代 Android UI 工具包
- [OkHttp](https://square.github.io/okhttp/) — HTTP 客戶端
- [Live2D Cubism SDK](https://www.live2d.com/) — 動態角色執行時期

完整清單請參閱 [第三方內容使用說明](app/src/main/assets/third_party_notice_zh-TW.md)。

---

<p align="center">
  <sub>用 ❤️ 製作 · <a href="https://github.com/FatPanda8885/NekoWeather">GitHub</a> · <a href="https://gitee.com/pasky/NekoWeather">Gitee</a></sub>
</p>
