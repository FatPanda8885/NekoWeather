# 隐私政策

**生效日期：2026年7月18日**

## 重要提示

本《隐私政策》（以下简称"本政策"）适用于 **NekoWeather（猫猫风雨）**（以下简称"本应用"）。**一旦您安装、打开或以任何方式使用本应用，即视为您已充分阅读、理解并同意本政策的全部内容。**

如果您不同意本政策，请立即停止使用本应用并卸载。

---

## 一、核心原则

**本应用本体不收集、不存储、不上传您的任何个人信息到开发者服务器。**

本应用没有后台服务器，不会将您的数据发送给开发者。所有数据（包括设置、城市列表、备份文件等）仅存储在您的设备本地。

本应用为开源项目，依据 **GNU General Public License v3.0（GPLv3）** 授权。任何人都可以查看本应用的完整源代码，以验证本隐私政策所述的数据处理实践。源代码托管于 [GitHub](https://github.com/FatPanda8885/NekoWeather)。

---

## 二、本应用本体的信息处理

### 2.1 我们不收集的信息

本应用本体**不会**收集以下任何信息：

- 个人身份信息（姓名、邮箱、电话号码、地址等）
- 设备标识符（IMEI、MAC 地址、Android ID、广告 ID 等）
- 通讯录、通话记录、短信
- 已安装应用列表
- 照片、媒体、文件（除用户主动导入的备份文件外）
- 任何形式的生物识别信息
- 任何形式的健康信息

### 2.2 本地存储的信息

以下信息**仅在您的设备本地存储**，不会上传到任何服务器：

- 应用设置（语言、温度单位、风力单位、更新间隔等）
- 已添加的城市列表
- API 配置（天气供应商、API 密钥等）
- AI 服务配置（API 地址、API 密钥、模型名称等）
- 通知偏好设置
- 本地备份文件
- API 使用次数统计（仅用于本地配额管理）

### 2.3 位置信息

本应用仅申请 **粗略位置权限**（`ACCESS_COARSE_LOCATION`），用于获取当前城市的气象数据。获取位置时：

- 位置信息直接发送至您选择的天气数据供应商（默认 Open-Meteo），**不会经过开发者的任何服务器**；
- 位置仅用于单次天气查询，不会被长期存储或追踪；
- 您可以随时在设备系统设置中撤回位置权限。

---

## 三、第三方内容提供商的隐私和数据收集

**重要：本应用不保证第三方内容提供商不会收集您的信息。**

### 3.1 第三方可能收集的信息

当本应用向第三方内容提供商请求数据时，您的设备将直接与第三方服务器通信。这些第三方可能收集以下信息：

- **网络信息**：IP 地址、请求时间、User-Agent（含应用版本号）
- **位置信息**：天气查询时发送的经纬度坐标或城市名称
- **设备信息**：HTTP 请求中自动包含的元数据
- **API 使用记录**：请求频率、请求内容、API 密钥关联的使用量

### 3.2 第三方隐私政策

使用本应用即视为您同意以下第三方内容提供商的隐私政策：

| 第三方 | 用途 | 隐私政策 |
|---|---|---|
| Open-Meteo | 天气/空气质量/地理编码 | [Privacy Policy](https://open-meteo.com/en/privacy) |
| 和风天气 (QWeather) | 天气数据（可选） | [隐私政策](https://dev.qweather.com/terms/privacy/) |
| RainViewer | 雷达降雨云图 | [Privacy Policy](https://www.rainviewer.com/privacy.html) |
| OpenStreetMap | 地图底图 | [隐私政策](https://osmfoundation.org/wiki/Privacy_Policy) |
| 高德地图 (Amap) | 地图瓦片（可选） | [隐私政策](https://lbs.amap.com/pages/privacy/) |
| 百度地图 (Baidu) | 地图瓦片（可选） | [隐私政策](https://lbsyun.baidu.com/index.php?title=open/privacy) |
| 花瓣地图 (Petal) | 地图瓦片（可选） | [隐私声明](https://consumer.huawei.com/cn/privacy/) |
| Anthropic | AI 天气建议（可选） | [Privacy Policy](https://www.anthropic.com/legal/privacy) |
| DeepSeek | AI 天气建议（可选） | [隐私政策](https://www.deepseek.com/privacy) |
| GitHub | 版本更新检查与分发 | [隐私声明](https://docs.github.com/site-policy/privacy-policies/github-general-privacy-statement) |
| Gitee | 版本更新检查与分发（中国境内镜像） | [隐私声明](https://gitee.com/terms) |
| NWS (美国) | 气象预警 | [Privacy Policy](https://www.weather.gov/privacy/) |
| DWD (德国) | 气象预警 | [Privacy Policy](https://www.dwd.de/EN/service/privacy-policy/privacy-policy_node.html) |
| Live2D Cubism SDK | 动态角色模型 | [隐私政策](https://www.live2d.com/en/legal/) |

### 3.3 开发者对第三方数据收集免责

**开发者无法控制、审核或干预上述第三方如何收集、使用、存储或分享您的信息。对于第三方内容提供商的隐私实践或数据收集行为，开发者在适用法律允许的最大范围内不承担任何责任。**

使用本应用即视为您确认已同意上述第三方内容提供商的隐私政策。

---

## 四、数据安全

### 4.1 本地数据安全

本应用的所有数据存储在您的设备上，数据安全性取决于您设备自身的安全措施（锁屏密码、加密存储等）。建议您保持设备的操作系统和本应用为最新版本。

### 4.2 第三方传输安全

本应用通过 HTTPS 加密协议与第三方 API 通信。但开发者不保证第三方服务器端的数据安全措施。

### 4.3 备份与还原

本应用的备份功能生成的文件包含您的所有本地设置和 API 配置。这些备份文件**不加密**存储于您设备的外部存储中。请妥善保管备份文件，开发者对备份文件泄露导致的任何问题不承担责任。

---

## 五、儿童隐私

本应用未针对 13 岁以下儿童设计或定向。本应用不会故意收集 13 岁以下儿童的个人信息。如果您是父母或监护人，发现您的孩子向我们提供了个人信息，请联系我们。

---

## 六、隐私政策更新

开发者保留随时修改本政策的权利。修改后的隐私政策将在应用内发布。继续使用本应用即视为接受修改后的隐私政策。

---

## 七、您的权利

由于本应用本体不收集您的个人信息，不存在需要您请求访问、更正或删除的开发者持有的个人数据。

关于第三方内容提供商持有的您的数据，请直接参考该第三方的隐私政策并联系该第三方行使您的权利。

---

## 八、联系方式

如您对本政策有任何疑问、意见或建议，请通过以下方式联系：

- **GitHub Issues**: [https://github.com/FatPanda8885/NekoWeather/issues](https://github.com/FatPanda8885/NekoWeather/issues)
- **Gitee Issues**: [https://gitee.com/pasky/NekoWeather/issues](https://gitee.com/pasky/NekoWeather/issues)
- **应用官网**: [https://nekoweather.12am.moe](https://nekoweather.12am.moe)

---

**使用本应用即代表您确认已阅读、理解并同意本隐私政策及所有第三方内容提供商的隐私政策。**
