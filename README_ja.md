<p align="center">
  <img src="app/src/main/res/drawable/neko1.jpeg" width="140" alt="NekoWeather">
</p>

<p align="center">
  <strong style="font-size: 2em;">NekoWeather</strong>
  <br>
  <span style="font-size: 1.3em;">猫猫風雨</span>
</p>

<p align="center">
  <em>🚧 正式アプリアイコンは現在制作中です。上記は仮のマスコット画像です。</em>
</p>

---

<p align="center">
  <a href="README.md">English</a> ·
  <a href="README_zh-CN.md">简体中文</a> ·
  <a href="README_zh-TW.md">繁體中文</a>
</p>

---

**NekoWeather（猫猫風雨）** は、Kotlin と Jetpack Compose で構築された多機能なオープンソースの Android 天気アプリケーションです。詳細な天気データ、インタラクティブな地図、AI による天気アドバイス、Live2D アニメーションキャラクターを、一つの没入型ダッシュボードに統合しています。**アカウント不要。広告なし。追跡なし。**

---

## 機能

### 🌤 天気ダッシュボード
- 動的な天気シーン付きのリアルタイム天気表示（雲、雨、雪、太陽光）
- 24 時間の時間別予報（気温、降水確率、風力）
- 15 日間の日別予報（最高/最低気温、天気、日の出/日の入り）
- 大気質指数（PM2.5、PM10、NO₂、O₃、SO₂、米国/欧州 AQI 規格対応）
- 風速、風向、湿度、気圧、視程、紫外線指数
- インタラクティブな太陽高度アークチャート
- インタラクティブな降水確率・降水量チャート

### 🗺 リアルタイム降雨レーダー
- アニメーション降雨レーダーオーバーレイ（RainViewer）
- 複数のベースマップ：OpenStreetMap、高徳地図、Petal Maps（Huawei）、百度地図
- 自動モード：中国本土では高徳地図、その他の地域では OSM を使用

### 🤖 AI 天気アドバイス
- Anthropic Messages API 互換（Claude、DeepSeek、互換ゲートウェイ対応）
- エンドポイント、モデル、システムプロンプト、出力長を設定可能
- タイプライターアニメーションによる AI 応答表示、Markdown レンダリング対応
- 現在の天気、時間別/日別予報、大気質、警報をコンテキストとして活用

### 🎭 Live2D アニメーションキャラクター
- Cubism SDK 5 フルランタイム統合
- 複数の Live2D モデルを ZIP でインポート可能
- ビジュアルレイアウトエディター（位置、スケール、回転）
- モーション、表情、物理演算、呼吸、まばたき、視線追跡、タップフィードバック対応

### 🎨 豊富なカスタマイズ
- カスタムフォント — システムフォントを完全に置き換え
- カスタムホーム画面ロゴ
- カスタム静止画または動画背景
- 天気別背景切り替え（雷、雪、雨、霧、曇り、晴れ）
- カスタムランチャーショートカットアイコン

### 🔊 TTS と音声
- システム TTS エンジンによる天気の読み上げ
- カスタムオフライン VITS/Piper ONNX 音声モデルのインポート

### 🌍 マルチプロバイダーアーキテクチャ
- **Open-Meteo** — デフォルト、API キー不要
- **和風天気 (QWeather)** — オプション、API キー必要
- **カスタムテンプレート** — 独自のエンドポイントと JSON パス抽出を定義可能
- 警報：自動（地域別 NWS/DWD）、QWeather、カスタム GeoJSON/CAP、または無効

### 📢 通知
- 毎朝の天気
- 日曜の週間予報
- 降水確率アラート
- 気象災害警報
- 通知履歴
- 再起動後も動作継続

### 🌐 多言語対応
全 UI が **4 言語**に完全対応：
- 简体中文（簡体字中国語）
- 繁體中文（繁体字中国語）
- English（英語）
- 日本語

### 💾 バックアップと復元
- カテゴリを選択して ZIP にエクスポート
- バックアップファイルから復元
- 設定、都市、プロバイダー設定、AI 設定、音声モデルをカバー

### 📊 API 使用統計
- プロバイダー別、ドメイン別のリクエスト数カウント
- 日別、週別、月別の使用統計
- オプションのクォータ制限

### 🏙 都市管理
- Open-Meteo Geocoding によるグローバル都市検索
- GPS 自動位置検出
- 複数の保存都市、ホーム画面でスワイプ切り替え

---

## 📜 法務とライセンス

本プロジェクトは **GNU General Public License v3.0（GPLv3）** に基づいて配布されています。詳細は [LICENSE](LICENSE) をご覧ください。

**NekoWeather を使用することにより、以下の文書に同意したことになります：**

| 文書 | 説明 |
|---|---|
| [ユーザー同意書](app/src/main/assets/terms_of_use_ja.md) | 免責事項、ユーザーの責任、サードパーティコンテンツ |
| [プライバシーポリシー](app/src/main/assets/privacy_policy_ja.md) | データの取り扱い — 本アプリは個人情報を一切収集しません |
| [サードパーティコンテンツ利用規約](app/src/main/assets/third_party_notice_ja.md) | すべてのサードパーティコンポーネントのライセンスと帰属 |

> ⚠️ **重要：** NekoWeather は「現状のまま」提供され、いかなる保証もありません。開発者は、天気データの正確性、地図の境界、AI 生成コンテンツ、またはサードパーティサービスについて一切の責任を負いません。ご使用前にユーザー同意書をよくお読みください。

---

## 🌐 公式ウェブサイト

公式ウェブサイトは現在 **開発中** で、一時的にアクセスできません。

- ウェブサイト：[https://nekoweather.12am.moe](https://nekoweather.12am.moe) *（近日公開）*

最新情報は GitHub リポジトリをご覧ください。

---

## 📦 ダウンロード

**唯一の公式配布チャンネル：**

| プラットフォーム | URL |
|---|---|
| **GitHub Releases** | [github.com/FatPanda8885/NekoWeather/releases](https://github.com/FatPanda8885/NekoWeather/releases) |
| **Gitee Releases** | [gitee.com/pasky/NekoWeather/releases](https://gitee.com/pasky/NekoWeather/releases)（中国向けミラー） |

> ⚠️ 開発者は、上記 2 つのチャンネルで公開されたリリースに対してのみ責任を負います。その他のソース（サードパーティのアプリストア、ダウンロードサイト、フォーラム等）から入手した APK は改ざんされている可能性があり、安全性と真正性は**保証されません**。

---

## 🛠 ソースからビルド

### 前提条件

| ツール | バージョン |
|---|---|
| **Android Studio** | 最新安定版 |
| **JDK** | 17 |
| **Android SDK** | 35 (compileSdk) |
| **Kotlin** | 2.1.20 |
| **Gradle** | 8.7+（ラッパー付属） |

### ビルド手順

```bash
# 1. リポジトリをクローン
git clone https://github.com/FatPanda8885/NekoWeather.git
cd NekoWeather

# 2. （オプション）Live2D Cubism SDK のセットアップ
# https://www.live2d.com/ から CubismSdkForJava-5-r.5.zip をダウンロード
# プロジェクトルートの .cubism-sdk/ に展開
# SDK は必須ではありません — 未設定の場合、ビルド時に自動スキップされます

# 3. Debug APK をビルド
./gradlew assembleDebug

# 4. または Release APK をビルド（署名設定が必要）
# ./gradlew assembleRelease
```

APK は `app/build/outputs/apk/debug/app-debug.apk` に生成されます。

### 最小要件
- **Android 8.0 (API 26)** 以上
- **ABI：** arm64-v8a、x86、x86_64

---

## 🏗 アーキテクチャ

- **言語：** Kotlin
- **UI フレームワーク：** Jetpack Compose + Material 3
- **アーキテクチャパターン：** MVVM（ViewModel + StateFlow）
- **HTTP クライアント：** OkHttp 4
- **シリアライゼーション：** 手動 JSON 解析（org.json）
- **ローカルストレージ：** SharedPreferences（ConfigStore 経由）
- **Live2D 統合：** Cubism SDK for Java 5（オプション、条件付きコンパイル）
- **コード圧縮：** ProGuard（リリースビルド）

---

## 📄 オープンソースライセンス

```
NekoWeather（猫猫風雨）— Android 天気アプリケーション
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

GPLv3 の全文は [LICENSE](LICENSE) をご覧ください。

---

## 🤖 AI 支援に関する開示

本プロジェクトの開発には、以下の AI/LLM ツールの支援を受けています（以下は一例であり、これらに限定されません）：

- **OpenAI Codex**
- **DeepSeek**

これらのツールは、コード生成、翻訳、ドキュメント作成、および開発ワークフローのサポートに使用されました。AI によって生成されたすべてのコンテンツは、人間の開発者によってレビューおよび検証されています。

---

## 🙏 謝辞

NekoWeather は多くの優れたオープンソースプロジェクトとサービスの上に成り立っています：

- [Open-Meteo](https://open-meteo.com/) — 無料の天気・大気質 API
- [RainViewer](https://www.rainviewer.com/) — 無料の降雨レーダーデータ
- [OpenStreetMap](https://www.openstreetmap.org/) — 無料の地図データ
- [Jetpack Compose](https://developer.android.com/compose) — モダンな Android UI ツールキット
- [OkHttp](https://square.github.io/okhttp/) — HTTP クライアント
- [Live2D Cubism SDK](https://www.live2d.com/) — アニメーションキャラクターランタイム

完全なリストは [サードパーティコンテンツ利用規約](app/src/main/assets/third_party_notice_ja.md) をご覧ください。

---

<p align="center">
  <sub>❤️ で作られました · <a href="https://github.com/FatPanda8885/NekoWeather">GitHub</a> · <a href="https://gitee.com/pasky/NekoWeather">Gitee</a></sub>
</p>
