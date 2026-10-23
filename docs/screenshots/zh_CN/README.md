# 简体中文界面截图

这组图片用于根目录的[简体中文 README](../../../README.zh_CN.md)，拍摄于 2026 年 10 月 23 日。

## 拍摄环境

- Android Emulator：Medium Phone，API 37，x86_64。
- 原始屏幕：1080 × 2400 像素，420 dpi，竖屏，浅色主题。
- 应用：从当前源码运行 `./gradlew --offline assembleDebug` 构建的 DayTiles 0.2.0 调试版。
- 应用语言：简体中文，Android 语言标签为 `zh-CN`，图片目录使用 `zh_CN`。
- 模拟器以 `-read-only -no-snapshot-load -no-snapshot-save` 启动，本次示例数据随会话丢弃。

## 截图与裁剪

所有图片均为模拟器内实际运行的应用界面，未使用实体手机截图、界面合成或后期文字替换。日记、任务、目标、课程和贴纸均为本次准备的中性示例内容，不包含个人记录。

处理过程完全在模拟器内完成：

1. 使用 Android `screencap` 保存原始 PNG。
2. 根据运行时窗口信息确认系统状态栏为顶部 63 像素，导航栏为底部 63 像素。
3. 通过模拟器内的 `app_process` 运行裁剪工具，使用 Android `Bitmap.createBitmap` 保留矩形 `(x=0, y=63, width=1080, height=2274)`，再编码为 PNG。
4. 最后通过 ADB 将已裁剪的图片复制到本目录。图片保持原始比例与像素，没有缩放。

裁剪只移除系统栏，保留应用自身的工具栏、底部导航和全部可见内容。每张成品尺寸均为 **1080 × 2274** 像素。

## 图片索引

| 图片 | 内容 |
| --- | --- |
| [month-view.png](month-view.png) | 月历、农历、任务、日记摘要与贴纸 |
| [quick-task-entry.png](quick-task-entry.png) | 任务快速输入面板 |
| [navigation-drawer.png](navigation-drawer.png) | 中文侧边导航与规划工具 |
| [task-targets.png](task-targets.png) | 每日任务目标与完成进度 |
| [widget-catalogue.png](widget-catalogue.png) | 桌面小组件目录 |
| [planning-tools.png](planning-tools.png) | 「我的」页面中的规划工具 |
| [course-timetable.png](course-timetable.png) | 含示例课程的每周课程表 |
| [calendar-settings.png](calendar-settings.png) | 农历显示设置 |
| [course-editor.png](course-editor.png) | 课程编辑与提醒选项 |
| [focus-clock.png](focus-clock.png) | 专注时钟与时长设置 |
