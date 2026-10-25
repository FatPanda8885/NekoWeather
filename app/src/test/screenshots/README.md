# 像素基线（Roborazzi）

这个目录存放 `app` 模块 Compose 界面的像素基线 PNG，由 Roborazzi 在 JVM 上用 Robolectric 渲染生成。

## 命名

`<Screen>_<state>.png`，例如：

| 文件 | 对应界面 | 截图原因 |
| --- | --- | --- |
| `SettingsRows_light.png` / `SettingsRows_dark.png` | `SettingsPanel` + `Group`/`Entry`/`SwitchRow`/`NoteRow`/`SeparatorRow` | 设置页所有二级页共用的行组件，深浅两套配色 |
| `SettingsControls_default.png` | `SliderRow`/`ChoiceRow`/`TextFieldRow`/`ActionRow` | 滑杆、按钮组换行、输入框错误态 |
| `LegalLanguageDropdown_zh.png` / `LegalLanguageDropdown_en.png` | `LegalLanguageDropdown` | 法律文档页的语言标签 |

## 命令

```bash
# 采集/更新基线（会把 PNG 直接写回本目录，提交前必须人工看一遍 diff）
./gradlew :app:recordRoborazziDebug

# 与基线比较（CI 用这个）
./gradlew :app:verifyRoborazziDebug
```

普通的 `./gradlew :app:testDebugUnitTest` 不会读写本目录，只会把图写到
`app/build/outputs/roborazzi-scratch/`，避免日常单测就地改写基线。

## 首次没有基线时

AI QA（`.github/workflows/ai-review.yml`）在发现本目录没有 PNG 时不会判定失败，
而是执行 `recordRoborazziDebug` 并把候选图上传成 `qa/ui-shots/baseline-candidates/`，
由人工确认后提交到本目录。
