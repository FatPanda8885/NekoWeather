# サードパーティコンテンツの使用手順

**NekoWeather（猫猫風雨）**は、以下の第三者コンテンツ（オープンソースプロジェクト、SDK、APIサービス、画像、フォント、モデルなど）を使用しています。本アプリを利用する前に、それぞれのライセンスをよくお読みください。

---

## このアプリケーションライセンスは、

このアプリケーションのソース コード (NekoWeather) は、**GNU General Public License v3.0 (GPLv3) ** に基づいてライセンスされています。このライセンスの全文は、プロジェクトのルート ディレクトリの [LICENSE](https://github.com/FatPanda8885/NekoWeather/blob/main/LICENSE) ファイル、または [https://www.gnu.org/licenses/gpl-3.0.html](https://www.gnu.org/licenses/gpl-3.0.html) にあります。 GPLv3 は、このアプリケーションを自由に実行、研究、変更、再配布する権利を保証し、変更された派生作品もオープンソースにし、GPLv3 と互換性のある方法で配布することを要求します。

** 注意してください: ** GPLv3 このアプリケーションの元のコードにのみ適用されます。以下にリストされているサードパーティのコンテンツには、それぞれ独自の使用許諾契約が適用され、このアプリケーションで使用されることによって GPLv3 が自動的に適用されるわけではありません。

---

## オープンソースライブラリ

| 名前 | ライセンス契約 | 著作権所有者 |
|---|---|---|
| AndroidX (コア、アクティビティ、ライフサイクル、構成) | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) | Android オープンソース プロジェクト/Google LLC |
| Jetpack Compose (UI、マテリアル 3、アイコン) | [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) | Android オープンソース プロジェクト/Google LLC |
| OkHttp | [Apache 2.0](https://square.github.io/okhttp/#license) | 株式会社スクエア |
| Kotlin Coroutines | [Apache 2.0](https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt) | JetBrains s.r.o. |
| Kotlin Standard Library | [Apache 2.0](https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt) | JetBrains s.r.o. |
| org.json (テスト) | [JSON ライセンス](https://www.json.org/license.html) | JSON.org |

---

## SDK

| 名前 | ライセンス契約 | 著作権所有者 |
|---|---|---|
| Live2D Cubism SDK for Java (5-r.5) | [Live2D 独自のライセンス](https://www.live2d.com/en/products/cubism-sdk/license/) | Live2D株式会社 |

> Live2D Cubism SDK を使用するには、別途ダウンロードしてライセンス契約に同意する必要があります。 SDK ファイルは、このプロジェクトのコード リポジトリでは配布されません。

---

## 天気およびデータ API

| サービス | 目的 | ライセンス/契約 |
|---|---|---|
| Open-Meteo | 天気、大気質、ジオコーディング | [用語](https://open-meteo.com/en/terms) · [CC-BY 4.0](https://creativecommons.org/licenses/by/4.0/) 属性付きデータ ソースを使用する |
| 和風天気 (QWeather) | 天気、大気質、警報、ジオコーディング (オプション) | [開発条件](https://dev.qweather.com/terms/) |
| RainViewer | レーダー雨量雲図 | [用語](https://www.rainviewer.com/terms.html) |
| 国立気象局 (NWS) | 米国の気象警報 | [免責事項](https://www.weather.gov/disclaimer/) |
| ドイツのウェッターディエンスト (DWD) | ドイツの気象警報 | [免責事項](https://www.dwd.de/EN/service/disclaimer/disclaimer_node.html) |

---

## 地図サービス

| サービス | 目的 | ライセンス/契約 |
|---|---|---|
| OpenStreetMap | マップのベースマップ タイル | [ODbL 1.0](https://www.openstreetmap.org/copyright) · © OpenStreetMap 寄稿者 |
| 高徳地図 (Amap) | 座標変換とマップ タイル (オプション) | [利用規約](https://lbs.amap.com/pages/terms/) |
| 百度地図 (Baidu) | マップタイル (オプション) | [利用規約](https://lbsyun.baidu.com/index.php?title=open/terms) |
| 花びらマップ (Petal Maps / Huawei) | マップタイル (オプション) | [利用規約](https://developer.huawei.com/consumer/cn/doc/petal-maps-TOS) |

> **⚠️ 重要な声明: ** 上記の地図サービスは、サードパーティのプロバイダーによって独自に運営されています。地図サービスが異なれば、それぞれのデータ ソースとポリシーに応じて、異なる国境、行政境界、地名ラベル、領土の所有権表現が表示される場合があります。 ** 開発者は、地図プロバ​​イダーによって表示される境界線、ラベル、または領土所属の表現について責任を負いません。 ** 地図の内容に異議がある場合は、対応する地図サービス プロバイダーに直接お問い合わせください。

---

## AIサービス

| サービス | 目的 | ライセンス/契約 |
|---|---|---|
| Anthropic (クロード) | AI天気アドバイス（オプション） | [消費者規約](https://www.anthropic.com/legal/consumer-terms) |
| DeepSeek | AI天気アドバイス（オプション） | [ユーザー同意書](https://www.deepseek.com/terms) |

---

## その他のネットワークサービス

| サービス | 目的 | ライセンス/契約 |
|---|---|---|
| GitHub | バージョン更新チェックと配布 | [利用規約](https://docs.github.com/site-policy/github-terms) |
| Gitee | バージョン更新チェックと配布（中国向けミラー） | [利用規約](https://gitee.com/terms) |
| Cloudflare | 中国本土の IP 検出 (Gitee ミラー フォールバック ロジック) | [利用規約](https://www.cloudflare.com/terms/) |

---

## 写真

| 名前 | ソース |
|---|---|
| neko1.jpeg | [@Nakkar7](https://x.com/@Nakkar7) |

---

## 重要なヒント

- 上記のサードパーティのコンテンツは、それぞれの著作権所有者が所有しており、それぞれのライセンス契約によって保護されています。
- このアプリケーションは、サードパーティのコンテンツを使用する場合、各使用許諾契約の条件に厳密に従います。
- サードパーティのコンテンツの使用について質問がある場合は、各著作権所有者の公式ライセンス契約を参照してください。
- このアプリがあなたが所有するコンテンツを不適切に使用していると思われる場合は、GitHub 問題までご連絡ください。
- ** 一つの中国原則に関する声明: ** このアプリケーションの開発者は一つの中国原則を遵守しており、台湾は中国領土の不可侵の一部です。地図サービスに表示される、「一つの中国」の原則と矛盾するラベル、境界表現、またはタイトルは、サードパーティの地図サービス プロバイダーの独立したデータ動作であり、開発者の立場を表すものではありません。開発者はこれについて一切の責任を負いません。
- ** 地図紛争に関する免責条項: ** 開発者は、サードパーティの地図プロバイダーの地図データ (国境、行政境界、地名の注釈、領土の所有権表示を含みますがこれらに限定されません) から生じる紛争、論争、または損失に対して一切の責任を負いません。特定の地図コンテンツに関する質問については、対応する地図サービス プロバイダーに直接お問い合わせください。
- **公式配布チャンネル免責事項：** 本アプリケーションの唯一の公式配布チャンネルは [GitHub](https://github.com/FatPanda8885/NekoWeather) と [Gitee](https://gitee.com/pasky/NekoWeather) です。その他のチャンネルを通じて入手された本アプリケーションについて、開発者はその完全性と安全性を保証できず、それに起因するいかなる問題についても責任を負いません。常に公式チャンネルから最新バージョンをダウンロードしてください。

---

※この説明文が更新された場合、アプリ内でも同時に公開されます。 *

*最終更新日: 2026 年 7 月 18 日*
