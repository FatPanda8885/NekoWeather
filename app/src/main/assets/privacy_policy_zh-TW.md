# 隱私權政策

**生效日期：2026年7月18日**

## 重要提示

本《隱私權政策》（以下簡稱"本政策"）適用於 **NekoWeather（貓咪風雨）**（以下簡稱"本應用"）。 **一旦您安裝、開啟或以任何方式使用本應用，即視為您已充分閱讀、瞭解並同意本政策的全部內容。 **

如果您不同意本政策，請立即停止使用本應用程式並解除安裝。

---

## 一、核心原則

**本應用程式本體不會收集、不儲存、不上傳您的任何個人資訊到開發者伺服器。 **

本應用程式沒有後台伺服器，不會將您的資料傳送給開發者。所有資料（包括設定、城市清單、備份檔案等）僅儲存在您的裝置本地。

本應用為開源項目，依據 **GNU General Public License v3.0（GPLv3）** 授權。任何人都可以查看本應用程式的完整原始程式碼，以驗證本隱私權政策所述的資料處理實務。原始碼託管於 [GitHub](https://github.com/FatPanda8885/NekoWeather)。

---

## 二、本應用本體的資訊處理

### 2.1 我們不收集的資訊

本應用本體**不會**收集以下任何資訊：

- 個人識別資料（姓名、信箱、電話號碼、地址等）
- 裝置識別碼（IMEI、MAC 位址、Android ID、廣告 ID 等）
- 通訊錄、通話記錄、簡訊
- 已安裝應用程式列表
- 照片、媒體、檔案（除使用者主動匯入的備份檔案外）
- 任何形式的生物識別訊息
- 任何形式的健康訊息

### 2.2 本地儲存的信息

以下資訊**僅在您的裝置本機儲存**，不會上傳到任何伺服器：

- 應用設定（語言、溫度單位、風力單位、更新間隔等）
- 已新增的城市列表
- API 配置（天氣供應商、API 金鑰等）
- AI 服務配置（API 位址、API 金鑰、模型名稱等）
- 通知偏好設定
- 本地備份文件
- API 使用次數統計（僅用於本機配額管理）

### 2.3 位置訊息

本應用程式僅申請 **粗略位置權限**（`ACCESS_COARSE_LOCATION`），用於取得目前城市的氣象資料。取得位置時：

- 位置資訊直接傳送至您選擇的天氣資料供應商（預設 Open-Meteo），**不會經過開發者的任何伺服器**；
- 位置僅用於單次天氣查詢，不會被長期儲存或追蹤；
- 您可以隨時在設備系統設定中撤回位置權限。

---

## 三、第三方內容提供者的隱私和資料收集

**重要：本應用程式不保證第三方內容提供者不會收集您的資訊。 **

### 3.1 第三方可能收集的信息

當本應用程式向第三方內容提供者要求資料時，您的裝置將直接與第三方伺服器通訊。這些第三方可能會收集以下資訊：

- **網路資訊**：IP 位址、請求時間、User-Agent（含應用程式版本號）
- **位置資訊**：天氣查詢時發送的經緯度座標或城市名稱
- **設備資訊**：HTTP 請求中自動包含的元數據
- **API 使用記錄**：請求頻率、請求內容、API 金鑰關聯的使用量

### 3.2 第三方隱私權政策

使用本應用程式即視為您同意以下第三方內容提供者的隱私權政策：

| 第三方 | 用途 | 隱私權政策 |
|---|---|---|
| Open-Meteo | 天氣/空氣品質/地理編碼 | [Privacy Policy](https://open-meteo.com/en/privacy) |
| 和風天氣 (QWeather) | 天氣資料（可選） | [隱私權政策](https://dev.qweather.com/terms/privacy/) |
| RainViewer | 雷達降雨雲圖 | [Privacy Policy](https://www.rainviewer.com/privacy.html) |
| OpenStreetMap | 地圖底圖 | [隱私權政策](https://osmfoundation.org/wiki/Privacy_Policy) |
| 高德地圖 (Amap) | 地圖瓦片（可選） | [隱私權政策](https://lbs.amap.com/pages/privacy/) |
| 百度地圖 (Baidu) | 地圖瓦片（可選） | [隱私權政策](https://lbsyun.baidu.com/index.php?title=open/privacy) |
| 花瓣地圖 (Petal) | 地圖瓦片（可選） | [隱私權聲明](https://consumer.huawei.com/cn/privacy/) |
| Anthropic | AI 天氣建議（可選） | [Privacy Policy](https://www.anthropic.com/legal/privacy) |
| DeepSeek | AI 天氣建議（可選） | [隱私權政策](https://www.deepseek.com/privacy) |
| GitHub | 版本更新檢查與分發 | [隱私權聲明](https://docs.github.com/site-policy/privacy-policies/github-general-privacy-statement) |
| Gitee | 版本更新檢查與分發（中國境內鏡像） | [隱私權聲明](https://gitee.com/terms) |
| NWS (美國) | 氣象預警 | [Privacy Policy](https://www.weather.gov/privacy/) |
| DWD (德國) | 氣象預警 | [Privacy Policy](https://www.dwd.de/EN/service/privacy-policy/privacy-policy_node.html) |
| Live2D Cubism SDK | 動態角色模型 | [隱私權政策](https://www.live2d.com/en/legal/) |

### 3.3 開發者對第三方資料收集免責

**開發者無法控制、審核或介入上述第三方如何收集、使用、儲存或分享您的資訊。對於第三方內容提供者的隱私實務或資料收集行為，開發者在適用法律允許的最大範圍內不承擔任何責任。 **

使用本應用程式即視為您確認已同意上述第三方內容提供者的隱私權政策。

---

## 四、資料安全

### 4.1 本地資料安全

本應用程式的所有資料儲存在您的裝置上，資料安全性取決於您裝置本身的安全措施（鎖定螢幕密碼、加密儲存等）。建議您將裝置的作業系統和本應用程式保持為最新版本。

### 4.2 第三方傳輸安全

本應用程式透過 HTTPS 加密協定與第三方 API 通訊。但開發者不保證第三方伺服器端的資料安全措施。

### 4.3 備份與還原

本應用程式的備份功能產生的檔案包含您的所有本機設定和 API 配置。這些備份檔案**不加密**儲存於您裝置的外部儲存。請妥善保管備份文件，開發者對備份文件外洩導致的任何問題不承擔責任。

---

## 五、兒童隱私

本應用程式未針對 13 歲以下兒童設計或定向。本應用程式不會故意收集 13 歲以下兒童的個人資訊。如果您是父母或監護人，發現您的孩子向我們提供了個人資訊，請聯絡我們。

---

## 六、隱私權政策更新

開發者保留隨時修改本政策的權利。修改後的隱私權政策將在應用程式內發布。繼續使用本應用程式即視為接受修改後的隱私權政策。

---

## 七、您的權利

由於本應用程式本體不收集您的個人信息，因此不存在需要您要求存取、更正或刪除的開發者持有的個人資料。

關於第三方內容提供者持有的您的數據，請直接參考該第三方的隱私權政策並聯絡該第三方行使您的權利。

---

## 八、聯絡方式

如您對本政策有任何疑問、意見或建議，請透過以下方式聯絡：

- **GitHub Issues**: [https://github.com/FatPanda8885/NekoWeather/issues](https://github.com/FatPanda8885/NekoWeather/issues)
- **Gitee Issues**: [https://gitee.com/pasky/NekoWeather/issues](https://gitee.com/pasky/NekoWeather/issues)
- **應用官網**: [https://nekoweather.12am.moe](https://nekoweather.12am.moe)

---

**使用本應用程式即代表您確認已閱讀、瞭解並同意本隱私權政策及所有第三方內容提供者的隱私權政策。 **
