# 繁體中文（台灣）介面截圖

這組圖片用於根目錄的[繁體中文（台灣） README](../../../README.zh_TW.md)，拍攝於 2026 年 10 月 23 日。

## 拍攝環境

- Android Emulator：Medium Phone，API 37，x86_64。
- 原始螢幕：1080 × 2400 畫素，420 dpi，豎屏，淺色主題。
- 應用：從當前原始碼執行 `./gradlew --offline assembleDebug` 建置的 DayTiles 0.2.0 除錯版。
- 應用程式語言：繁體中文（台灣），Android 語言標籤為 `zh-TW`，圖片目錄使用 `zh_TW`。
- 模擬器以 `-read-only -no-snapshot-load -no-snapshot-save` 啟動，本次範例資料隨會話丟棄。

## 截圖與裁剪

所有圖片均為模擬器內實際執行的應用介面，未使用實體手機截圖、介面合成或後期文字替換。日記、任務、目標、課程和貼紙均為本次準備的中性範例內容，不包含個人記錄。

處理過程完全在模擬器內完成：

1. 使用 Android `screencap` 儲存原始 PNG。
2. 根據執行時視窗資訊確認系統狀態列為頂部 63 畫素，導覽列為底部 63 畫素。
3. 通過模擬器內的 `app_process` 執行裁剪工具，使用 Android `Bitmap.createBitmap` 保留矩形 `(x=0, y=63, width=1080, height=2274)`，再編碼為 PNG。
4. 最後通過 ADB 將已裁剪的圖片複製到本目錄。圖片保持原始比例與畫素，沒有縮放。

裁剪只移除系統欄，保留應用自身的工具欄、底部導覽和全部可見內容。每張成品尺寸均為 **1080 × 2274** 畫素。

## 圖片索引

| 圖片 | 內容 |
| --- | --- |
| [month-view.png](month-view.png) | 月曆、農曆、任務、日記摘要與貼紙 |
| [quick-task-entry.png](quick-task-entry.png) | 任務快速輸入面板 |
| [navigation-drawer.png](navigation-drawer.png) | 中文側邊導覽與規劃工具 |
| [task-targets.png](task-targets.png) | 每日任務目標與完成進度 |
| [widget-catalogue.png](widget-catalogue.png) | 主畫面小工具目錄 |
| [planning-tools.png](planning-tools.png) | 「我的」頁面中的規劃工具 |
| [course-timetable.png](course-timetable.png) | 含範例課程的每週課表 |
| [calendar-settings.png](calendar-settings.png) | 農曆顯示設定 |
| [course-editor.png](course-editor.png) | 課程編輯與提醒選項 |
| [focus-clock.png](focus-clock.png) | 專注時鐘與時長設定 |
