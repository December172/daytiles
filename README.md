# DayTiles

DayTiles 是一个完全离线、免费且开源的 Android 视觉日历、日记与每日计划应用。项目以占满屏幕主体的月历为中心，让贴纸、日记预览和可勾选任务直接出现在日期格中。

## 当前状态

里程碑 1 已建立单 `app` 模块的 Jetpack Compose 工程，并提供：

- 支持 4–6 周的自适应月历布局
- 上月、下月、返回今天与日期选择
- 根据可用高度裁剪信息密度的大尺寸日期格
- 独立的日记、任务、贴纸预览模型
- 与生产数据层隔离的里程碑 1 示例内容
- 浅色与深色 Material 3 主题

当前示例内容不会持久化；Room、真实编辑器和贴纸资源将在后续里程碑实现。

## 环境

- JDK 17 或更高版本（源码目标为 Java 17）
- Android SDK 37
- Android SDK Build Tools 36.0.0
- 使用仓库内 Gradle Wrapper，无需系统 Gradle

主要构建版本由 `gradle/libs.versions.toml` 统一管理：AGP 9.4.0、Kotlin/Compose Compiler 2.4.10、Compose BOM 2026.09.00。

## 构建与测试

```shell
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

调试 APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。

## 结构

```text
app/src/main/java/com/december172/daytiles/
├── data/sample/       # 仅用于里程碑 1 的演示内容
├── domain/model/      # 日记、任务与贴纸的独立模型
└── ui/
    ├── calendar/      # 月历状态、日期算法和 Compose UI
    └── theme/         # Material 3 主题
```

## 隐私与联网

核心功能按完全离线设计。项目不包含账号、广告、分析、追踪、云服务或后端依赖。

