# 随机闹钟（RandomAlarm）

一个 Android 本地音乐闹钟：铃声从手机音乐库（或指定文件夹）里**随机选一首**，并**从歌曲的随机位置开始播放**；支持指定单曲、音量渐强、多计时器、可配置的四向手势、封面背景和静音响铃。

- 纯 Java + 系统控件，**无第三方依赖**
- **不联网**：没有 INTERNET 权限，不收集任何数据
- minSdk 26（Android 8.0），targetSdk 34

> 定位：不是“又一个闹钟”，而是“**随机起播闹钟**”。适合把整张专辑/整个歌单当铃声池的人。

## 功能

### 闹钟

- 重复星期（不选＝只响一次）、标签、启停开关
- **随机起播**：全局开关 + 起播区间（占歌曲时长的百分比，如 15%～90%），避开前奏和尾声
- **指定歌曲**：每个闹钟可固定一首（系统文件选择器选音频），也可在默认设置里指定“默认歌曲”供新建闹钟继承
- 音量：渐强开关、渐强时长、初始音量、音量上限（均可自定义数值）
- 响铃时长：一直响 / 自定义秒数；**最小响铃时长**（默认 5 秒）内禁止任何操作，防睡梦中误触
- 贪睡：短按按默认时长，**长按可选本次贪睡时长**（5/10/15/20/30 分钟）；到点后可自动贪睡 N 次
- 静音播放：只震动、亮屏、显示界面，不发出声音
- 响铃后删除：可选

### 响铃界面

- 全屏显示时间、歌名、歌手、**播放进度条（可拖动）**、`当前/总长` 时间
- **左右滑切歌**（带滑出/滑入动画），**上滑/下滑/左滑/右滑**动作可在设置里分别配置为：关闭 / 贪睡 / 切歌 / 禁止
- 上下滑动时只移动内容层，保留封面背景，不黑屏
- 封面背景：读取歌曲内嵌封面，亮度可调
- 长按“贪睡”选择本次贪睡时长

### 主页

- 「下一个闹钟」卡片：时间、星期、倒计时、**跳过本次 / 恢复本次 / 取消贪睡** 按钮
- 响铃前 10 分钟通知提醒
- 长按闹钟删除（二次确认）；编辑页删除同样二次确认
- 多个计时器：按响铃时间排序，可单独取消，支持时/分/秒和快捷时间
- 桌面 2×1 时钟小组件

### 设置

- 四大可折叠分组：**默认 / 全局 / 历史 / 计时**
- 默认组：新建闹钟的默认值 + 「应用到所有闹钟」
- 全局组：随机起播、四向滑动、封面亮度、屏幕方向（自由/强制竖屏/强制横屏）
- 所有数字类设置都支持 **「自定义…」** 输入任意值（带 `X~Y` 范围提示）

## 安装

从 [Releases](https://github.com/your-name/RandomAlarm/releases) 下载 APK：

```bash
adb install -r RandomAlarm-debug.apk
```

或把 APK 传到手机用文件管理器安装。装好后打开一次，按首页提示补齐权限。

## 从源码构建

项目提供两种构建方式：**Gradle**（推荐，便于二次开发）和**无 Gradle 的脚本**（离线、无需下载依赖）。

### 方式一：Android Studio / Gradle（推荐）

环境：JDK 17、Android SDK（compileSdk 35）。

1. 用 Android Studio 打开项目根目录，等待 Gradle 同步（首次会下载 AGP 8.6.1）；
2. 直接 Run，或命令行：

```bash
gradle assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

> 仓库未附带 Gradle Wrapper 的 jar，用 Android Studio 打开会自动生成；命令行请使用 Gradle 8.7+。

### 方式二：无 Gradle 脚本（离线构建）

环境：JDK 17、Android SDK（build-tools + platform 35）、Python 3。

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk
bash build.sh
# 产物：RandomAlarm-debug.apk
```

脚本会自动定位 SDK（`ANDROID_HOME` / `ANDROID_SDK_ROOT` / 常见路径），把资源与源码复制到临时目录，使用 `aapt2 + javac + d8 + zipalign + apksigner` 打包，首次构建会生成 `debug.keystore`（已被 `.gitignore` 排除，不要提交）。

## 权限

| 权限 | 用途 |
|---|---|
| `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` | 读取本地音乐（Android 13+ 为 `READ_MEDIA_AUDIO`） |
| `POST_NOTIFICATIONS` | 响铃通知、10 分钟预告 |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | 精确闹钟 |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 响铃前台服务 |
| `VIBRATE`、`WAKE_LOCK`、`RECEIVE_BOOT_COMPLETED` | 震动、唤醒、开机恢复 |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | 引导用户关闭电池优化 |

**没有网络权限**，App 不会上传任何数据。

## 国产 ROM 注意事项

- **自启动 / 后台运行**：在系统设置里允许“自启动”“后台运行”“无限制”，否则杀后台或重启后可能不响。
- **精确闹钟**：首页“去授予”跳转到“闹钟与提醒”后允许。
- **通知**：必须允许通知，否则锁屏可能没有界面。
- **锁屏显示**：部分 ROM 限制第三方 App 锁屏全屏，需允许“锁屏显示/后台弹出界面”。
- **开机广播**：少数 ROM（如红魔）会拦截第三方开机广播，重启后可能需要手动打开一次 App 恢复调度。

## 已知限制

- 文件夹模式只扫描所选目录及其子目录，不支持多文件夹拼歌单；
- 媒体库模式只认 `IS_MUSIC!=0` 的音频；
- 封面只支持歌曲文件内嵌封面，没有内嵌封面时背景为黑色；
- 指定歌曲依赖 SAF 读取权限，卸载重装或清除数据后需要重新选择；
- 未做单元测试，欢迎提交 issue / PR。

## 目录结构

```
app/
  AndroidManifest.xml
  build.gradle                 Gradle 模块配置（namespace / 版本号）
  java/com/dsh/randomalarm/
    Alarm.java                 闹钟数据模型 + JSON
    AlarmStore.java            闹钟存储（device-protected）
    AlarmScheduler.java        下次触发、setAlarmClock、贪睡、预告
    AlarmReceiver.java         响铃广播 → 前台服务 + 响铃界面
    AlarmService.java          播放、渐强、随机起播、震动、封面、通知
    MusicLibrary.java          MediaStore / SAF 文件夹随机选歌
    MainActivity.java          主页、列表、下一个闹钟、多计时器
    EditActivity.java          新建/编辑闹钟
    RingActivity.java          响铃界面（进度条、手势、封面）
    SettingsActivity.java      设置（默认/全局/历史/计时）
    BootReceiver.java          开机恢复
    PreAlertReceiver.java      响铃前 10 分钟通知
    TimerStore.java            多计时器存储
    NumberSpinner.java         数值 Spinner + 自定义输入
    ClockWidgetProvider.java   2×1 桌面时钟小组件
  res/                         布局、图标、字符串
build.sh                       无 Gradle 的离线构建脚本
```

## 许可证

[MIT](LICENSE)
