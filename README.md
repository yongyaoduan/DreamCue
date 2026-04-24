# DreamCue

DreamCue 是一个本地备忘提醒 App。业务层使用 `Rust`，Android 宿主使用 `Kotlin` 和 `Compose`。

## 功能

- 写下一条短句备忘，内容按原文保存。
- 保存 `created_at_ms`、`updated_at_ms`、`last_reviewed_at_ms`、`cleared_at_ms`。
- 每天固定时间提醒还没有处理完的备忘。
- 已清除的备忘进入历史，不再参与每日提醒。
- 新增、编辑、继续提醒、清除、重新打开都会写入日志。
- 支持搜索当前备忘和历史备忘。

## 目录

- `crates/memo-core`: Rust 核心业务层，负责 SQLite 存储、日志、每日队列和搜索。
- `crates/memo-android-ffi`: Rust JNI 桥，向 Android 返回 JSON。
- `android/`: Android App，包含 Compose UI、通知权限、闹钟调度和开机重建提醒。
- `docs/architecture.md`: 架构说明。
- `scripts/build-rust-android.sh`: 构建 Android native 库。

## 构建前提

构建前准备以下工具：

1. Rust 工具链：`rustup`
2. Android Studio、Android SDK、Android NDK
3. `cargo-ndk`
4. Android Rust 目标：

```bash
rustup target add aarch64-linux-android x86_64-linux-android
```

## 构建

1. 构建 Rust 动态库：

```bash
./scripts/build-rust-android.sh
```

2. 用 Android Studio 打开 [android/settings.gradle.kts](android/settings.gradle.kts)。

3. 同步 Gradle，运行 `app`。

## 当前状态

- Rust 核心已覆盖备忘生命周期、日志、每日提醒队列和搜索。
- Android 端已覆盖基础 UI、通知权限、每日提醒调度和开机后重建提醒。
- 搜索目前是混合搜索，包含精确匹配、字符 n-gram、短语相似和同义词提示。
- 语义搜索可在 `crates/memo-core/src/search.rs` 中替换为本地 embedding 或服务端 embedding。
