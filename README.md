# 轻译（QingYi）

一个只做翻译的 Android 小工具：**文字翻译 + 相册图片翻译 + 拍照翻译**，并带一个可以追问的**翻译助手**。
界面布局参考 Google 翻译手机版（大输入区、粘贴按钮、底部语言胶囊、中间相机大按钮），但使用自己的名称、配色和图标。

翻译走 **DeepSeek V4.1 Flash（`deepseek-flash`）**，它原生支持图片理解，所以图片不需要本地 OCR，直接把选中的区域发给模型。Key 由用户自己填写，只保存在本机。

## 功能

### 文字

- 输入文字后，**点“翻译”按钮**才发送请求（不会边打字边翻译，省 token）
- 支持粘贴、清空、复制译文、语言互换
- 键盘上的“前往/Go”也可以直接触发翻译

### 图片

- 相册选图或拍照后进入图片编辑器
- 图片上有一个**可拖动、可拉伸的选框**：四角改大小，四条边可以单独推拉，只有框中间才整体移动；拖动时带触感反馈
- 点“翻译选中区域”时：**选框小于整图**会发两张（完整图片做语境 + 选框做翻译目标）；**选框就是整图**只发一张，由模型自己找出需要翻译的文字
- 译文以**半透明浮层盖在选框下沿**（类似豆包/Google Lens 的就地翻译效果），不是把所有文字抽出来堆在下面
- 浮层可滚动，支持复制、重新框选

### 翻译助手

- 翻译结果下方有“解释 / 更口语 / 问助手”
- 打开底部对话面板，可以继续问：这句话的语气、某个词在语境里的意思、换个说法……
- 图片场景会把**选中的图片 + 译文**作为上下文；**只有第一轮带图**，后续追问只带译文和对话记录，避免每轮重复计图片 token

### 其他

- 20 种常用语言，源语言支持“检测语言”
- 设置里填 DeepSeek API Key / Base URL / 模型名，只存本机 SharedPreferences
- 没有历史记录、没有账号、没有广告、没有多余 SDK

## 技术栈

| 模块 | 选型 |
|---|---|
| UI | Jetpack Compose + Material 3（薄荷绿主题） |
| 架构 | 单 Activity + 一个 `AppViewModel`（`mutableStateOf` 状态） |
| 相机 | CameraX 1.3.4 |
| 图片解码 | BitmapFactory + ExifInterface（处理拍照旋转） |
| 翻译 / 识图 | DeepSeek `deepseek-flash`，OpenAI 兼容 Chat Completions |
| 网络 | `HttpURLConnection`（只有一个 POST，不引 OkHttp/Retrofit） |
| 配置 | SharedPreferences（不引 DataStore/Room） |
| 最低版本 | Android 7.0（API 24），targetSdk 34 |

图片以 base64 `data:` URL 放在 `image_url` 内容块里发送，`detail` 用 `original`；
模型对每张图最多计 ~1024 token，所以裁得越小越便宜、也越快。

## 构建

环境：JDK 17、Android SDK Platform 34、Build-Tools 34。

```bash
./gradlew :app:assembleDebug
# 调试包：app/build/outputs/apk/debug/app-debug.apk

./gradlew :app:assembleRelease
# 发布包（R8 + 资源压缩，约 1.8 MB）：app/build/outputs/apk/release/app-release-unsigned.apk
```

发布包需要签名后才能安装：

```powershell
# 个人测试可以先用本机的 debug keystore
& "$env:ANDROID_HOME\build-tools\34.0.0\apksigner.bat" sign `
  --ks "$env:USERPROFILE\.android\debug.keystore" `
  --ks-pass pass:android --key-pass pass:android `
  --ks-key-alias androiddebugkey `
  --out QingYi-release.apk app\build\outputs\apk\release\app-release-unsigned.apk
```

正式上架请换成自己的 keystore。

> 注意：Windows 下如果项目路径包含 `&`（例如 `D:\Q&A chat\...`），`gradlew.bat` 会因为 `%~dp0` 未加引号而报错。
> 解决办法：把项目放到不含 `&` 的路径，或用目录联接（junction）再构建：
> ```powershell
> cmd /c mklink /J C:\qy "D:\Q&A chat\QingYi"
> cd C:\qy
> .\gradlew.bat :app:assembleDebug
> ```

## 首次使用

1. 打开 App → 右上角圆形“译”或底部“设置”
2. 填入在 <https://platform.deepseek.com> 创建的 API Key
3. Base URL 默认 `https://api.deepseek.com/v1`，模型默认 `deepseek-flash`
4. 保存后回首页：输入文字点“翻译”，或点底部“图片 / 相机”

Key 只写入本机 `SharedPreferences`；文字、图片和问题会发送给 DeepSeek 用于翻译。

## 代码结构

```
app/src/main/java/com/qingyi/translate/
├── MainActivity.kt              # 单 Activity、底部栏、图片选择器、页面切换
├── data/
│   ├── Language.kt              # 语言列表（显示名 + prompt 名）
│   ├── DeepSeekSettings.kt      # 配置数据类 + 翻译结果
│   ├── ChatTurn.kt              # 助手对话消息
│   ├── DeepSeekClient.kt        # 文字 / 图片 / 追问三类请求与错误映射
│   └── SettingsStore.kt         # SharedPreferences 持久化
├── util/BitmapUtils.kt          # 解码、EXIF 旋转、按选框裁剪
└── ui/
    ├── AppUiState.kt            # Screen / TranslationUi / ImageUi / AssistantUi
    ├── AppViewModel.kt          # 翻译、选框翻译、助手对话、设置
    ├── translate/TranslationScreen.kt
    ├── image/ImageTranslateScreen.kt   # 选框 + 译文浮层
    ├── assistant/AssistantSheet.kt     # 追问面板
    ├── camera/CameraScreen.kt
    ├── settings/SettingsScreen.kt
    ├── language/LanguageListOverlay.kt
    └── theme/                   # 薄荷绿配色 + 字体
```

## 想继续改的话

- **加语言**：`data/Language.kt` 追加枚举项即可
- **换模型**：设置页直接改；只要兼容 OpenAI Chat Completions 即可
- **换翻译服务**：只改 `DeepSeekClient` 的三个方法，保持 `TranslateResult` 返回类型
- **换配色 / 图标**：`ui/theme/Color.kt`、`res/mipmap-*`、`res/drawable-nodpi/ic_launcher_foreground.png`
- **想让译文逐行贴合原文**：需要每个文本行的坐标。可以再加一个本地 OCR（ML Kit）拿行框，把行框和模型译文按顺序对应后逐行绘制，工程量会明显上升

## 许可与合规

- 布局灵感来自 Google 翻译，但**没有使用** Google 的名称、Logo、四色 G 图标或商业外观；发布前请换成自己的应用名、图标和签名。
- DeepSeek API 的使用须遵守其服务条款，费用由用户自己的账户承担。
- CameraX / ExifInterface 遵循 Apache-2.0；Material Icons 遵循 Apache-2.0。
