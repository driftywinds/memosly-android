# Release Guide

## 发布新版本

### 1. 更新版本号与发布元数据

编辑 `app/build.gradle.kts`：

```kotlin
defaultConfig {
    applicationId = "com.whtis.memosly"
    versionCode = 8          // 每次发布 +1
    versionName = "1.3.0"    // 语义化版本号
}
```

版本号规则：
- **主版本号** (1.x.x)：大功能改版、不兼容变更
- **次版本号** (x.1.x)：新功能
- **修订号** (x.x.1)：Bug 修复

同时更新四处元数据（缺一处 `CI` 会报错）：

- `fastlane/metadata/android/{en-US,zh-CN}/changelogs/<versionCode>.txt`（≤500 字符）
- `version.json`（App 内检查更新读取，必须与 versionName 一致）
- `CHANGELOG.md`（新增 `## [x.y.z] - 日期` 小节和底部链接引用）

本地自检：

```bash
python3 scripts/check_release_metadata.py
```

### 2. 合并到 main 并确认 CI 全绿

Release workflow **只在手动触发时运行**（这是刻意的：发布必须来自 CI 全绿的提交，
而不是碰巧被 push 的 tag）。先确认 CI 通过，再进入下一步。

### 2.5 配置 Actions secrets（每个仓库单独配置，fork 不继承）

Release workflow 依赖以下 secrets（仓库 Settings → Secrets and variables → Actions）：

| Secret | 必需 | 内容 |
|--------|------|------|
| `KEYSTORE_BASE64` | ✅（发版必需） | `release.keystore` 文件的 base64 |
| `KEYSTORE_PASSWORD` | ✅（发版必需） | keystore 密码 |
| `KEY_ALIAS` | ✅（发版必需） | keystore 里的 key 别名 |
| `KEY_PASSWORD` | ✅（发版必需） | key 密码 |
| `GOOGLE_SERVICES_JSON_BASE64` | 建议 | `google-services.json` 的 base64（缺失时 CI 用占位文件，构建能过但 Firebase 不可用） |

生成 base64：

```powershell
# Windows PowerShell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\path\to\release.keystore"))
```

```bash
# Linux / macOS（-w0 避免换行）
base64 -w0 release.keystore
base64 -w0 google-services.json
```

> 没有 `KEYSTORE_BASE64` 时 workflow 仍然会构建，但产物是**未签名 APK**，且会拒绝
> 创建 GitHub Release；artifact 可正常下载用于测试。

### 3. 触发 Release workflow

```bash
gh workflow run release.yml \
  --repo whtis/memosly-android \
  -f tag=v1.3.0 \
  -f create_release=true
```

- `tag`：要创建的 release tag（例如 `v1.3.0`）；留空则只构建并上传 artifact
- `create_release`：是否创建 GitHub Release 并附上 APK（默认 true）

产物：`Memosly-v<versionName>-release.apk`，同时作为 workflow artifact 保留 90 天。

### 4. 验证

发布后检查：
- https://github.com/whtis/memosly-android/releases/latest
- 确认 APK 能正常下载安装
- App 内「检查更新」应提示已是最新（version.json 已随发布更新）

### 本地构建 Release APK（不发版时）

```bash
./gradlew assembleRelease
```

产物路径：`app/build/outputs/apk/release/app-release.apk`

> 签名配置读取自 `keystore.properties`（已 gitignore），keystore 文件在 `keystore/release.keystore`。
