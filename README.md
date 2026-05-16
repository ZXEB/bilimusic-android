# BiliMusic

一个基于 Bilibili 的第三方 Android 音乐播放器。

## 功能

- 通过 Bilibili 登录（SESSDATA）获取收藏夹内容
- 收藏夹作为播放列表，支持顺序/随机/单曲循环
- 搜索 Bilibili 视频并播放音频
- 播放队列管理（添加/移除）
- 后台播放 + 通知栏控制
- 迷你播放器
- 封面取色动态背景
- 倍速/音调/均衡器/响度增强
- 睡眠定时

## 登录方式

-在 https://www.bilibili.com/ 登录账号后F12获取哔bilibili SESSDATA 填写到软件内即可

## 截图

![](https://dl.xp6.top/view.php/7e7b1c539582ab3c8484b7d230dd0768.jpg)

![](https://dl.xp6.top/view.php/5aa4673904ad08719d91b0523ee89891.jpg)

！[](https://dl.xp6.top/view.php/b8e11a316b0c092cf10b7adb4ea8bacf.jpg)

![](https://dl.xp6.top/view.php/bc70b9fc2773249e4fbe9f37dab4eaa3.jpg)

## 构建环境

| 项目 | 版本 |
|------|------|
| Android Studio | Koala / Ladybug 均可 |
| Gradle | 8.5 (Wrapper 内置) |
| Java | 17 |
| Android SDK | 34 (compileSdk) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 |
| Kotlin | 1.9.22 |
| Compose BOM | 2024.02.00 |
| Compose Compiler | 1.5.10 |

## 构建方法

### 方式一：Android Studio（推荐）

1. 用 Android Studio **File → Open** 打开项目根目录
2. 等待 Gradle Sync 完成（第一次需要下载依赖，请保持网络畅通）
3. 点击顶部工具栏的 **Build → Make Project**（或按 `Ctrl+F9`）
4. 连接设备或启动模拟器，点击 **Run**（或按 `Shift+F10`）

### 方式二：命令行

```bash
# Windows（双击运行）
build.bat

# 或手动执行
./gradlew assembleDebug
```

APK 生成位置：`app/build/outputs/apk/debug/app-debug.apk`

### 方式三：直接使用 build.bat

项目根目录下提供了 `build.bat`，双击即可自动检测环境并构建。

> 注意：构建前请确保已安装 JDK 17+ 和 Android SDK 34+，并配置好 `JAVA_HOME` 和 `ANDROID_HOME` 环境变量。

## 使用准备

1. 打开 Bilibili 网页端，登录后按 `F12` → **Application** → **Cookies** → 复制 `SESSDATA` 的值
2. 在 App 中点击侧边栏 → **设置** → **登录**，粘贴 SESSDATA
3. 登录后在设置中添加要显示的收藏夹
4. 开始播放收藏夹内的音乐

## 技术栈

- **UI**: Jetpack Compose + Material 3
- **播放器**: ExoPlayer (Media3)
- **网络**: OkHttp
- **图片**: Coil
- **状态管理**: ViewModel + StateFlow
- **数据持久化**: DataStore
- **取色**: Android Palette
