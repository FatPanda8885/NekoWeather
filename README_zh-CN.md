<p align="center">
  <img src="app/src/main/res/drawable/neko1.jpeg" width="140" alt="NekoWeather">
</p>

<p align="center">
  <strong style="font-size: 2em;">NekoWeather</strong>
  <br>
  <span style="font-size: 1.3em;">猫猫风雨</span>
</p>

<p align="center">
  <em>🚧 正式应用图标正在制作中，上方为占位吉祥物图片。</em>
</p>

---

<p align="center">
  <a href="README.md">English</a> ·
  <a href="README_zh-TW.md">繁體中文</a> ·
  <a href="README_ja.md">日本語</a>
</p>

---

**NekoWeather（猫猫风雨）** 是一款功能丰富的开源 Android 天气应用，使用 Kotlin 和 Jetpack Compose 构建。它将详尽的天气数据、交互式地图、AI 驱动的天气建议和 Live2D 动态角色模型融为一体，呈现为沉浸式单一仪表盘界面。**无需注册账号，无广告，无追踪。**

---

## 功能特性

### 🌤 天气仪表盘
- 实时天气状况，配合动态天气场景（云飘、雨落、雪飘、阳光光晕）
- 24 小时逐小时预报，包含温度、降雨概率和风力
- 15 天每日预报，包含最高/最低温度、天气状况和日出/日落
- 空气质量指数，包含 PM2.5、PM10、NO₂、O₃、SO₂（支持美标/欧标 AQI）
- 风力、风向、湿度、气压、能见度和紫外线指数
- 交互式太阳高度角弧线图
- 交互式降雨概率/预计降雨量图表

### 🗺 实时降雨雷达
- 动画降雨雷达叠加层（RainViewer）
- 多种底图供应商：OpenStreetMap、高德地图、花瓣地图（华为）、百度地图
- 自动模式：中国大陆使用高德地图，其他地区使用 OSM

### 🤖 AI 天气建议
- 兼容 Anthropic Messages API（支持 Claude、DeepSeek 及兼容网关）
- 可配置端点、模型、系统提示词和输出长度
- 打字机动画效果显示 AI 回复，支持 Markdown 渲染
- 上下文感知：综合当前天气、逐小时/每日预报、空气质量和预警

### 🎭 Live2D 动态角色
- 完整 Cubism SDK 5 运行时集成
- 支持通过 ZIP 导入多个 Live2D 模型
- 可视化布局编辑器（位置、大小、旋转角度）
- 支持动作、表情、物理、呼吸、眨眼、触摸视线和点击反馈

### 🎨 深度自定义
- 自定义字体 — 完全替换系统字体
- 自定义主页 Logo
- 自定义静态图片或视频动态背景
- 按天气状况切换背景（雷、雪、雨、雾、云、晴）
- 自定义桌面快捷方式图标

### 🔊 TTS 与音色
- 系统 TTS 引擎朗读天气
- 导入自定义离线 VITS/Piper ONNX 语音模型

### 🌍 多供应商架构
- **Open-Meteo** — 默认，无需 API Key
- **和风天气 (QWeather)** — 可选，需 API Key
- **自定义模板** — 定义自己的端点和 JSON 路径提取
- 预警：自动（按地区选择 NWS/DWD）、和风天气、自定义 GeoJSON/CAP、或关闭

### 📢 通知提醒
- 每日早间天气
- 周日下周速览
- 降雨概率提醒
- 气象灾害预警
- 通知发送历史
- 重启后持久运行

### 🌐 多语言
完整 UI 本地化，支持 **4 种语言**：
- 简体中文
- 繁體中文
- English
- 日本語

### 💾 备份与还原
- 选择配置类别并导出为 ZIP
- 从备份文件还原
- 涵盖设置、城市、供应商配置、AI 配置和语音模型

### 📊 API 请求统计
- 按供应商、域名分别统计
- 每日、每周、每月使用统计
- 可选配额限制

### 🏙 城市管理
- 全球城市搜索（Open-Meteo Geocoding）
- GPS 自动定位
- 多个已保存城市，主页左右滑动切换

---

## 📜 法律与许可

本项目依据 **GNU General Public License v3.0（GPLv3）** 分发。详见 [LICENSE](LICENSE)。

**使用 NekoWeather 即代表您同意以下文件：**

| 文件 | 说明 |
|---|---|
| [用户使用协议](app/src/main/assets/terms_of_use.md) | 免责声明、用户责任、第三方内容 |
| [隐私政策](app/src/main/assets/privacy_policy.md) | 数据处理方式 — 本应用不收集任何个人信息 |
| [第三方内容使用说明](app/src/main/assets/third_party_notice.md) | 所有第三方组件的许可与归属 |

> ⚠️ **重要提示：** NekoWeather 按"现状"提供，不附带任何形式的担保。开发者对天气数据的准确性、地图边界、AI 生成内容或任何第三方服务不承担任何责任。使用前请仔细阅读用户使用协议。

---

## 🌐 官方网站

官方网站正在 **开发中**，暂时无法访问。

- 网址：[https://nekoweather.12am.moe](https://nekoweather.12am.moe) *（即将上线）*

请关注 GitHub 仓库获取最新动态。

---

## 📦 下载

**唯一官方分发渠道：**

| 平台 | 地址 |
|---|---|
| **GitHub Releases** | [github.com/FatPanda8885/NekoWeather/releases](https://github.com/FatPanda8885/NekoWeather/releases) |
| **Gitee Releases** | [gitee.com/pasky/NekoWeather/releases](https://gitee.com/pasky/NekoWeather/releases)（中国境内镜像） |

> ⚠️ 开发者仅对以上两个渠道发布的版本负责。通过任何其他渠道（第三方应用商店、下载站、论坛等）获取的 APK 可能遭到篡改，开发者**不保证**其安全性和真实性。

---

## 🛠 从源码构建

### 环境要求

| 工具 | 版本 |
|---|---|
| **Android Studio** | 最新稳定版 |
| **JDK** | 17 |
| **Android SDK** | 35 (compileSdk) |
| **Kotlin** | 2.1.20 |
| **Gradle** | 8.7+（已包含 wrapper） |

### 构建步骤

```bash
# 1. 克隆仓库
git clone https://github.com/FatPanda8885/NekoWeather.git
cd NekoWeather

# 2. （可选）配置 Live2D Cubism SDK
# 从 https://www.live2d.com/ 下载 CubismSdkForJava-5-r.5.zip
# 解压到项目根目录的 .cubism-sdk/ 文件夹
# 该 SDK 非必须 — 如未配置，构建时会自动跳过

# 3. 构建 Debug APK
./gradlew assembleDebug

# 4. 或构建 Release APK（需要签名配置）
# ./gradlew assembleRelease
```

APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

### 最低要求
- **Android 8.0 (API 26)** 或更高
- **ABI：** arm64-v8a、x86、x86_64

---

## 🏗 架构

- **语言：** Kotlin
- **UI 框架：** Jetpack Compose + Material 3
- **架构模式：** MVVM（ViewModel + StateFlow）
- **HTTP 客户端：** OkHttp 4
- **序列化：** 手动 JSON 解析（org.json）
- **本地存储：** SharedPreferences（通过 ConfigStore）
- **Live2D 集成：** Cubism SDK for Java 5（可选，条件编译）
- **代码压缩：** ProGuard（Release 构建）

---

## 📄 开源许可

```
NekoWeather（猫猫风雨）— Android 天气应用
Copyright (C) 2026  NekoWeather 贡献者

本程序是自由软件：您可以依据自由软件基金会发布的
GNU 通用公共许可证（第三版或任意更新版本）的条款，
对其进行再发布和/或修改。

本程序的分发是基于"有用"的目的，但不附带任何担保；
甚至不包含对适销性或特定用途适用性的默示担保。
详情请参阅 GNU 通用公共许可证。
```

详见 [LICENSE](LICENSE) 获取 GPLv3 完整文本。

---

## 🤖 AI 辅助声明

本项目在开发过程中使用了 AI/LLM 工具辅助，包括但不限于：

- **OpenAI Codex**
- **DeepSeek**

上述工具用于代码生成、翻译、文档起草及开发流程支持。所有 AI 生成的内容均已经过人工审核与验证。

---

## 🙏 致谢

NekoWeather 建立在众多优秀开源项目和服务之上：

- [Open-Meteo](https://open-meteo.com/) — 免费天气与空气质量 API
- [RainViewer](https://www.rainviewer.com/) — 免费降雨雷达数据
- [OpenStreetMap](https://www.openstreetmap.org/) — 免费地图数据
- [Jetpack Compose](https://developer.android.com/compose) — 现代 Android UI 工具包
- [OkHttp](https://square.github.io/okhttp/) — HTTP 客户端
- [Live2D Cubism SDK](https://www.live2d.com/) — 动态角色运行时

完整列表请参阅 [第三方内容使用说明](app/src/main/assets/third_party_notice.md)。

---

<p align="center">
  <sub>用 ❤️ 制作 · <a href="https://github.com/FatPanda8885/NekoWeather">GitHub</a> · <a href="https://gitee.com/pasky/NekoWeather">Gitee</a></sub>
</p>
