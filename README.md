<p align="center">
  <img src="docs/icon.png" width="116" alt="轻译图标" />
</p>

<h1 align="center">轻译 QingYi</h1>

---

<p align="center"><strong>把看不懂的文字，变成看得懂的译文。</strong></p>

<p align="center">文字翻译 · 图片框选 · 拍照识别 · 可追问的翻译助手</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?logo=kotlin&logoColor=white&labelColor=2B2B2B" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white&labelColor=2B2B2B" alt="Android" />
  <img src="https://img.shields.io/badge/Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white&labelColor=2B2B2B" alt="Compose" />
  <img src="https://img.shields.io/badge/CameraX-1.3.4-3DDC84?logo=android&logoColor=white&labelColor=2B2B2B" alt="CameraX" />
  <img src="https://img.shields.io/badge/DeepSeek-V4.1%20Flash-4D6BFE?labelColor=2B2B2B" alt="DeepSeek" />
  <img src="https://img.shields.io/badge/License-MIT-3DA639?labelColor=2B2B2B" alt="License" />
</p>

<p align="center"><strong>简体中文</strong> ｜ <a href="README.en.md">English</a></p>

---

## 这是什么

轻译是一个只做翻译的 Android 小工具。没有信息流、没有账号、没有广告，打开就是翻译。

界面参考 Google 翻译的布局，但使用自己的名称、配色和图标。翻译走 **DeepSeek V4.1 Flash（`deepseek-flash`）**，它原生支持图片理解，所以图片不需要本地 OCR，直接把画面交给模型。

## 功能

| 能力 | 说明 |
| --- | --- |
| **文字翻译** | 输入后点“翻译”才请求，支持 20 种语言、粘贴、复制、语言互换 |
| **图片翻译** | 相册选图，框选要翻译的区域；译文以半透明浮层盖在选框位置 |
| **拍照翻译** | CameraX 取景，拍完直接进入框选，不用先存相册 |
| **翻译助手** | 译文下方可继续追问：语气、语境、换个说法、为什么这么翻 |
| **视觉理解** | 局部翻译会同时发送完整画面作为语境；整图翻译只发一张 |
| **隐私** | API Key 只保存在本机，没有账号、统计或后台服务 |

## 安装

前往 [Releases](https://github.com/boluo140716/QingYi/releases/latest) 下载 `QingYi-release.apk`。

- Android 7.0（API 24）及以上
- 安装包约 **1.8 MB**
- 首次打开：右上角头像 → 填入自己的 DeepSeek API Key

也可以命令行安装：

```bash
adb install -r QingYi-release.apk
```

API Key 在 [platform.deepseek.com](https://platform.deepseek.com) 创建。默认模型 `deepseek-flash`，Base URL `https://api.deepseek.com/v1`，一般不用改。

## 使用

1. **文字**：输入或粘贴文字 → 点“翻译”
2. **图片**：点底部“图片” → 选图 → 拖动/拉伸选框 → “翻译选中区域”
3. **拍照**：点底部“相机” → 对准文字 → 快门 → 框选 → 翻译
4. **追问**：在译文下方点“解释 / 更口语”，或点对话图标继续问

框选的四个角改大小，四条边单独推拉，只有框中间才整体移动；拖动时有触感反馈。

## 技术栈

| 模块 | 选型 |
| --- | --- |
| UI | Jetpack Compose + Material 3 |
| 架构 | 单 Activity + `AppViewModel`（`mutableStateOf`） |
| 相机 | CameraX 1.3.4 |
| 图片处理 | BitmapFactory + ExifInterface（处理拍照旋转） |
| 翻译 / 识图 | DeepSeek `deepseek-flash`，OpenAI 兼容 Chat Completions |
| 网络 | `HttpURLConnection`（不引 OkHttp / Retrofit） |
| 配置 | SharedPreferences（不引 DataStore / Room） |

图片以 base64 `data:` URL 放在 `image_url` 内容块里发送，`detail=original`。每张图最多约 1024 token。

## 构建

环境：JDK 17、Android SDK Platform 34、Build-Tools 34。

```bash
./gradlew :app:assembleRelease
# 产物：app/build/outputs/apk/release/app-release-unsigned.apk（约 1.8 MB）
```

release 已开启 R8 与资源压缩，需要签名后才能安装：

```bash
apksigner sign --ks your.keystore --out QingYi-release.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk
```

> Windows 下如果项目路径包含 `&`，`gradlew.bat` 会因为 `%~dp0` 未加引号而报错。把项目放到不含 `&` 的路径，或用目录联接：
>
> ```powershell
> cmd /c mklink /J C:\qy "D:\path\to\QingYi"
> cd C:\qy
> .\gradlew.bat :app:assembleDebug
> ```

## 项目结构

```
app/src/main/java/com/qingyi/translate/
├── MainActivity.kt              # 单 Activity、底部栏、页面切换
├── data/
│   ├── Language.kt              # 语言列表
│   ├── DeepSeekSettings.kt      # 配置与翻译结果
│   ├── ChatTurn.kt              # 助手对话消息
│   ├── DeepSeekClient.kt        # 文字 / 图片 / 追问请求
│   └── SettingsStore.kt         # SharedPreferences
├── util/BitmapUtils.kt          # 解码、EXIF 旋转、裁剪
└── ui/
    ├── AppUiState.kt
    ├── AppViewModel.kt
    ├── translate/               # 文字翻译与结果卡片
    ├── image/                   # 选框、就地译文浮层
    ├── assistant/               # 追问面板
    ├── camera/                  # CameraX 取景
    ├── settings/                # API Key 与模型配置
    ├── language/                # 语言选择
    └── theme/                   # 薄荷绿配色
```

## 常见问题

**为什么需要自己填 API Key？**
翻译和识图都直接调用 DeepSeek，没有中间服务器。Key 只存在本机，费用由你自己的账户承担。


**数据会上传到哪？**
只有 DeepSeek：文字、图片、追问内容会发送到 `api.deepseek.com`。没有其他后端，也没有统计 SDK。


## License

[MIT](LICENSE)
