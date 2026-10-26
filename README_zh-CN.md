<p align="center">
  <img src="docs/icon.png" width="120" alt="NekoWeather">
</p>

<h1 align="center">猫猫风雨 <sub>NekoWeather</sub></h1>

<p align="center">
  <b>把天气数据、动态降雨雷达、AI 建议和 Live2D 角色塞进同一屏的开源 Android 天气应用</b><br>
  <sub>Kotlin · Jetpack Compose · Material 3 &nbsp;|&nbsp; 无账号 · 无广告 · 无追踪</sub>
</p>

<p align="center">
  <a href="https://github.com/FatPanda8885/NekoWeather/releases"><img src="https://img.shields.io/github/v/release/FatPanda8885/NekoWeather?label=release&color=blue" alt="release"></a>
  <img src="https://img.shields.io/badge/license-GPL--3.0-orange" alt="license">
  <img src="https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-3DDC84?logo=android&logoColor=white" alt="android">
  <img src="https://img.shields.io/badge/Compose-Material%203-4285F4" alt="compose">
  <img src="https://img.shields.io/badge/i18n-4%20%E8%AF%AD%E8%A8%80-blueviolet" alt="i18n">
  <a href="https://github.com/FatPanda8885/NekoWeather/actions/workflows/ci.yml"><img src="https://github.com/FatPanda8885/NekoWeather/actions/workflows/ci.yml/badge.svg" alt="ci"></a>
</p>

<p align="center">
  <b>简体中文</b> ·
  <a href="README_zh-TW.md">繁體中文</a> ·
  <a href="README_ja.md">日本語</a> ·
  <a href="README.md">English</a>
</p>

<p align="center">
  <img src="docs/screenshots/dashboard.webp" width="240" alt="首页">
  &nbsp;
  <img src="docs/screenshots/cards-alerts.webp" width="240" alt="预警与雷达">
  &nbsp;
  <img src="docs/screenshots/ai-advice.webp" width="240" alt="AI 建议">
</p>

---

## ✨ 一眼看懂

| 你最关心的 | 猫猫风雨怎么做的 |
| --- | --- |
| **隐私** | 无账号、无广告、无埋点。设置、城市、Key 全部存本机；不向任何自有服务器上报数据 |
| **信息密度** | 一屏给全：实时天气 + 24 小时 + 15 日 + 空气质量 + 日出日落 + 太阳高度弧线 + 降水概率图 |
| **数据源自由** | Open-Meteo 免 Key 开箱即用；可切和风天气；也可自填 endpoint + JSON 路径模板接任意接口 |
| **沉浸感** | Cubism SDK 5 驱动的 Live2D 角色：动作、表情、物理、呼吸、眨眼、视线跟随、点击反馈，外加可视化布局编辑器 |
| **可玩性** | 自定义字体 / LOGO / 静态或视频背景 / 按天气切换背景 / 启动图标 / 离线 VITS-Piper 语音模型 |
| **不锁死** | 4 语言界面、ZIP 备份还原、API 用量统计与配额、GitHub + Gitee 双通道分发 |

## 🚀 30 秒上手

1. **装**：从 [GitHub Releases](https://github.com/FatPanda8885/NekoWeather/releases)（或 [Gitee 镜像](https://gitee.com/pasky/NekoWeather/releases)）下载对应架构的 APK。
2. **开**：首次启动选择界面语言 → 允许定位（也可以拒绝，手动搜索城市一样能用）。
3. **用**：首页下拉刷新；右上角依次是 **刷新 / 语音播报 / 设置**。

> 默认数据源 Open-Meteo **不需要任何 API Key**，装完即用。

## 🎯 核心能力

### 🌤 一屏到底的天气仪表盘

实时天气 + 动态天气场景（云、雨、雪、日光）· 24 小时逐小时预报（温度 / 降水概率 / 风力）· 15 日预报（高低温、天气、日出日落）· 空气质量（PM2.5 / PM10 / NO₂ / O₃ / SO₂，US & EU 双标准）· 风速风向、湿度、气压、能见度、紫外线 · 可交互的太阳高度弧线与降水概率/雨量图。

<img src="docs/screenshots/dashboard.webp" width="300" alt="首页">

### 🗺 动态降雨雷达与预警卡片

RainViewer 动画降雨图层；底图可切换 **OpenStreetMap / 高德 / 花瓣地图（华为）/ 百度**，并提供「中国大陆用高德、其他地区用 OSM」的自动模式。危害预警卡片支持 **自动（按地区选 NWS / DWD）/ 和风 / 自定义 GeoJSON·CAP / 关闭**。

<img src="docs/screenshots/cards-alerts.webp" width="300" alt="预警与雷达">

### 🤖 AI 天气建议

兼容 **Anthropic Messages API** 的服务都能接（Claude、DeepSeek 及各类兼容网关）：端点、模型、系统提示词、输出长度全部可配；回答带打字机动画与 Markdown 渲染，并把当前天气、逐小时/15 日预报、空气质量、预警一起作为上下文喂给模型。

<img src="docs/screenshots/ai-advice.webp" width="300" alt="AI 建议">

### 🎭 Live2D 角色

完整的 **Cubism SDK for Java 5** 运行时：支持 ZIP 导入多个模型、可视化布局编辑器（位置 / 缩放 / 旋转），动作、表情、物理、呼吸、眨眼、视线跟随与点击反馈全部可用。

### 🌍 数据源与预警

| 类别 | 可选 |
| --- | --- |
| 天气数据 | Open-Meteo（默认，免 Key）· 和风天气（需 Key）· 自定义 endpoint + JSON 路径提取 |
| 危害预警 | 自动（NWS / DWD 按地区）· 和风 · 自定义 GeoJSON / CAP · 关闭 |

<img src="docs/screenshots/providers.webp" width="300" alt="数据源">

### 🎨 深度自定义

整套系统字体替换 · 自定义首页 LOGO · 静态或视频背景 · 按天气条件（雷/雪/雨/雾/多云/晴）分别设置背景 · 自定义桌面快捷方式图标。

<img src="docs/screenshots/theme-color.webp" width="300" alt="主题">

### 🔊 语音与 TTS

系统 TTS 朗读天气；可导入自定义离线 **VITS / Piper ONNX** 语音模型。

### 📢 通知与提醒

每日早间天气、周日周报、降水概率提醒、危害预警提醒，附带通知历史，重启后仍生效。

<img src="docs/screenshots/notifications.webp" width="300" alt="通知">

### 💾 备份 · 统计 · 多城市

ZIP 分类备份还原（设置 / 城市 / 数据源 / AI / 语音模型）· 按数据源与域名统计 API 调用量（日/周/月，可设配额）· Open-Meteo 全球城市搜索、GPS 定位、多城市首页滑动切换。

<img src="docs/screenshots/settings.webp" width="300" alt="设置">

## 📦 下载

| 渠道 | 地址 |
| --- | --- |
| **GitHub Releases** | <https://github.com/FatPanda8885/NekoWeather/releases> |
| **Gitee Releases**（国内镜像） | <https://gitee.com/pasky/NekoWeather/releases> |

> ⚠️ 以上是**仅有的两个官方分发渠道**。第三方应用商店、下载站、论坛提供的 APK 可能被二次打包，不保证安全与正版。

## 🛠 从源码构建

| 依赖 | 版本 |
| --- | --- |
| Android Studio | 最新稳定版 |
| JDK | 17 |
| Android SDK | 35（compileSdk） |
| Kotlin | 2.1.20 |
| Gradle | 8.11（wrapper 自带） |

```bash
git clone https://github.com/FatPanda8885/NekoWeather.git
cd NekoWeather

# 可选：Live2D Cubism SDK（缺失时自动回退到无 Live2D 的 lite 源集，仍可正常编译）
# 从 https://www.live2d.com/ 下载 CubismSdkForJava-5-r.5.zip，解压到 .cubism-sdk/

./gradlew assembleDebug     # 产物：app/build/outputs/apk/debug/
./gradlew assembleRelease   # 需要 signing/keystore.properties
```

**运行要求**：Android 8.0（API 26）及以上；ABI 支持 arm64-v8a / x86 / x86_64（release 按 ABI 拆分并提供通用包）。

## 🏗 技术栈

| 维度 | 选型 |
| --- | --- |
| 语言 / UI | Kotlin 2.1.20 · Jetpack Compose（Material 3） |
| 架构 | MVVM：ViewModel + StateFlow 单一状态源 |
| 网络 | OkHttp 4 |
| 序列化 | 手写 JSON 解析（`org.json`）+ JSON 路径提取器 |
| 存储 | SharedPreferences（`ConfigStore` / `WeatherCacheStore` 等） |
| 渲染 | Live2D Cubism SDK for Java 5（可选，条件编译） |
| 混淆 | ProGuard（release） |

<details>
<summary>项目结构（点击展开）</summary>

```
app/src/main/java/com/neko/weather/
├── MainActivity.kt              # 入口
├── model/Models.kt              # 数据模型、枚举、DTO
├── data/
│   ├── WeatherRepository.kt     # 天气拉取与缓存
│   ├── AlertRepository.kt       # 危害预警
│   ├── AiAdviceRepository.kt    # AI 建议调用
│   ├── CityRepository.kt        # 城市搜索与管理
│   ├── ConfigStore.kt           # 配置持久化
│   ├── ApiUsageTracker.kt       # 用量统计与配额
│   ├── BackupManager.kt         # ZIP 备份还原
│   └── UpdateRepository.kt      # GitHub / Gitee 版本检查
├── ui/
│   ├── NekoWeatherApp.kt        # 全部界面（首页 / 设置 / 二级页）
│   ├── MainViewModel.kt         # 中枢 ViewModel
│   ├── Localization.kt          # 4 语言运行期文案
│   ├── Onboarding.kt            # 首启向导
│   └── LegalDocuments.kt        # 法律文档查看
├── notification/WeatherNotificationService.kt
└── tts/VoiceModelManager.kt
```

</details>

## ❓ 常见问题

**需要注册账号吗？** 不需要，也没有账号体系。

**会收集我的数据吗？** 不会。应用只在你要看天气时向**你自己配置的**数据源和 AI 端点发请求，其余数据留在本机。详见[隐私政策](app/src/main/assets/privacy_policy.md)。

**AI 建议的 Key 存在哪？** 明文存在本机 SharedPreferences（设置页内有提示）。不想用就不配，功能会自动隐藏。

**没有 Live2D Cubism SDK 能编译吗？** 能。缺 SDK 时构建自动走 `lite` 源集（无 Live2D 界面），不会报错。

**为什么 release 有多个 APK？** 按 ABI 拆分（arm64-v8a / x86 / x86_64）以减小体积，另有一个通用包。

**天气数据准不准？** 取决于你选的数据源；应用不做二次订正，请以数据源与官方预警为准（见下方免责声明）。

**开源协议允许二次分发吗？** GPLv3 允许，但必须同样开源并保留协议。修改版请勿使用官方名称与图标。

## 📜 许可证与法律文件

本项目以 **GNU General Public License v3.0（GPLv3）** 发布，全文见 [LICENSE](LICENSE)。

| 文档 | 说明 |
| --- | --- |
| [用户协议](app/src/main/assets/terms_of_use.md) | 免责、用户责任、第三方内容 |
| [隐私政策](app/src/main/assets/privacy_policy.md) | 数据处理说明：不收集个人信息 |
| [第三方声明](app/src/main/assets/third_party_notice.md) | 第三方组件许可与署名 |

> ⚠️ **猫猫风雨按「现状」提供，不附带任何担保。** 开发者不对天气数据准确性、地图边界、AI 生成内容或任何第三方服务承担责任。使用前请阅读用户协议。

## 🌐 官方网站

<https://nekoweather.12am.moe>（建设中，暂不可用）——最新动态请以本仓库为准。

## 🤖 AI 使用披露

本项目在开发过程中使用了 AI / LLM 工具辅助，包括但不限于 **OpenAI Codex** 与 **DeepSeek**，用于代码生成、翻译、文档撰写与工作流支持。所有 AI 生成的产出均已由人类开发者审查与验证。

## 🙏 致谢

- [Open-Meteo](https://open-meteo.com/) — 免费天气与空气质量 API
- [RainViewer](https://www.rainviewer.com/) — 免费降雨雷达
- [OpenStreetMap](https://www.openstreetmap.org/) — 免费地图数据
- [Jetpack Compose](https://developer.android.com/compose) — Android 现代 UI 工具包
- [OkHttp](https://square.github.io/okhttp/) — HTTP 客户端
- [Live2D Cubism SDK](https://www.live2d.com/) — 角色动画运行时

完整清单见[第三方声明](app/src/main/assets/third_party_notice.md)。

---

<p align="center">
  <sub>Made with ❤️ · <a href="https://github.com/FatPanda8885/NekoWeather">GitHub</a> · <a href="https://gitee.com/pasky/NekoWeather">Gitee</a></sub>
</p>
