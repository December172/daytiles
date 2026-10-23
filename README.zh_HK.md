# DayTiles

[English](README.md) | [简体中文](README.zh_CN.md) | [繁體中文（香港）](README.zh_HK.md) | [繁體中文（台灣）](README.zh_TW.md)

DayTiles 是一款免費、開源、以離線使用為核心的 Android 日曆、日記與可視化規劃應用。你可以在月曆中查看每天的日記摘要、貼紙和待辦事項，也可以使用課程表、任務目標和專注時鐘安排日常生活。

應用使用 Kotlin、Jetpack Compose、Room 和 Material 3 構建。無需註冊帳號，沒有廣告、訂閲、統計追蹤或後端服務。

## 界面預覽

以下圖片均來自運行繁體中文（香港）界面的 Android 模擬器。截圖已在模擬器內裁去系統狀態欄和導覽列；日記、任務與課程內容均為專門準備的示例數據。

<table>
  <tr>
    <td align="center"><strong>月曆與農曆</strong><br><img src="docs/screenshots/zh_HK/month-view.png" width="240" alt="DayTiles 繁體中文（香港）月曆，顯示農曆、任務、日記摘要和貼紙"></td>
    <td align="center"><strong>快速新增任務</strong><br><img src="docs/screenshots/zh_HK/quick-task-entry.png" width="240" alt="DayTiles 中文任務快速輸入面板"></td>
  </tr>
  <tr>
    <td align="center"><strong>側邊導覽</strong><br><img src="docs/screenshots/zh_HK/navigation-drawer.png" width="240" alt="DayTiles 中文側邊欄，包含週檢視和規劃工具"></td>
    <td align="center"><strong>任務目標</strong><br><img src="docs/screenshots/zh_HK/task-targets.png" width="240" alt="DayTiles 每日任務目標與完成進度"></td>
  </tr>
  <tr>
    <td align="center"><strong>主畫面小工具</strong><br><img src="docs/screenshots/zh_HK/widget-catalogue.png" width="240" alt="DayTiles 中文小工具目錄與新增至主畫面按鈕"></td>
    <td align="center"><strong>規劃工具</strong><br><img src="docs/screenshots/zh_HK/planning-tools.png" width="240" alt="DayTiles 我的頁面，集中提供本地規劃工具"></td>
  </tr>
  <tr>
    <td align="center"><strong>課程表</strong><br><img src="docs/screenshots/zh_HK/course-timetable.png" width="240" alt="DayTiles 中文每週課程表，顯示課程時間和地點"></td>
    <td align="center"><strong>日曆設定</strong><br><img src="docs/screenshots/zh_HK/calendar-settings.png" width="240" alt="DayTiles 設定頁面中的顯示中國農曆開關"></td>
  </tr>
  <tr>
    <td align="center"><strong>編輯課程</strong><br><img src="docs/screenshots/zh_HK/course-editor.png" width="240" alt="DayTiles 中文課程編輯頁面，包含星期、時間、地點和提醒選項"></td>
    <td align="center"><strong>專注時鐘</strong><br><img src="docs/screenshots/zh_HK/focus-clock.png" width="240" alt="DayTiles 中文專注時鐘與專注、休息時長設定"></td>
  </tr>
</table>

截圖的設備、語言和裁剪方式見[截圖説明](docs/screenshots/zh_HK/README.md)。界面改版過程與驗證記錄見[外觀改版里程碑](docs/redesign.md)（英文）。

## 主要功能

- **月曆**：充分利用屏幕高度，以週日為每週第一天；支持左右滑動切換月份、年月選擇、返回今天和相鄰月份日期顯示。
- **農曆顯示**：離線顯示中國農曆日期，可在設定中隨時開啓或關閉；偏好會儲存在本機。
- **每日日記**：按日期記錄文字並自動儲存，在月曆中預覽摘要。日記與任務、貼紙分別存儲。
- **任務與子任務**：快速新增任務，或在完整編輯頁設定標題、詳情、標籤、可勾選子任務和可選提醒。
- **任務整理**：支持完成與刪除、按日期分組、標籤篩選、逾期提示，以及按重要性和緊急程度分類的四象限優先次序矩陣。
- **任務目標**：設定每日或每週完成數量，查看進度、週日期欄和目標打卡次數。
- **貼紙**：內置 Emoji、自訂 Emoji、本地 PNG/WebP 匯入，以及可選的 HTTPS 圖片下載；貼紙庫可重複使用，貼紙位置與具體日期關聯。
- **週檢視**：集中查看一週內的任務與日記摘要。
- **課程表**：在同一個每週網格中查看、新增和管理課程，支持上課時間、地點、每週重複和可選本地提醒。
- **專注時鐘**：設定專注與休息時長，支持暫停、繼續、自動進入休息階段，以及關聯任務。
- **主畫面小工具**：提供待辦清單、每日筆記、月曆、週檢視和專注時鐘五類可調整尺寸的小工具。
- **中文界面**：默認語言為英語，並提供簡體中文、繁體中文（香港）和繁體中文（台灣）本地化；應用語言隨系統語言切換。
- **離線使用**：計劃數據儲存在本機，無需帳號、雲同步或後端連接。

## 使用説明

### 日曆、日記與貼紙

底部提供「日程」「檢視」「目標」「小工具」「我的」五個主要入口。月曆中的日期格可以顯示任務、日記摘要和貼紙；選擇日期後，可進入當天的日記與貼紙編輯界面。

點選月曆頂部的年月可選擇月份，左右滑動可切換月份，點選「今天」可返回當前日期。通過「我的 → 設定 → 顯示中國農曆」可以控制農曆標籤是否顯示。

農曆日期由 Android 系統自帶的 ICU 日曆離線計算，目前不包含節氣或法定節假日標註。

### 任務與目標

點選日程或檢視頁面的 **+** 打開快速輸入面板，填寫任務標題，並按需補充詳情、標籤、優先次序和提醒。點選已有任務可進入完整編輯頁，繼續管理子任務和其他字段。

任務列表按日期分組，支持勾選完成和標籤篩選。「目標」頁面用於設定每日或每週完成任務數量；「優先次序矩陣」按已設定的重要性和緊急程度展示任務。未設定優先次序的任務不會自動歸入象限。

### 課程表與專注時鐘

通過側邊欄或「我的」打開「課程表」。點選空白時段或頂部 **+** 可新增課程，空白時段會預選對應星期和時間；點選課程塊可編輯或刪除課程。頂部還可切換周次或打開課程列表，重疊課程會分別顯示並可單獨選擇。

「專注時鐘」提供可設定的專注和休息時長、暫停與繼續、持續通知及可選任務關聯，也可以從主畫面專注小工具啓動。

### 新增主畫面小工具

在「小工具」頁面選擇類型，點選「新增至主畫面」，然後在主畫面啓動器的系統彈窗中確認。應用收到實際放置回調後才會顯示新增成功。

如果啓動器不支持自動新增，或沒有收到新增確認，可以使用頁面提供的重試與手動新增入口：返回主畫面，長按空白處，打開小工具列表，找到 DayTiles 並拖動所需小工具。請為小工具留出足夠的主畫面空間。

| 類型 | 內容與操作 |
| --- | --- |
| 待辦清單 | 查看未完成任務、日期與提醒時間，並直接勾選完成 |
| 每日筆記 | 預覽今天的日記，一鍵打開編輯 |
| 月曆 | 查看本地化緊湊月曆與今天的日期高亮 |
| 週檢視 | 查看本週按日期排列的任務和日記摘要 |
| 專注時鐘 | 啓動或查看專注計時 |

五類小工具均可調整大小，並根據可用空間展示緊湊或展開佈局。

## 下載與安裝

支持 Android 8.0（API 26）及以上版本。可從 [GitHub Releases](https://github.com/December172/daytiles/releases) 下載發佈的 APK，也可以按下文從源碼構建。

## 構建與檢查

環境要求：

- JDK 17 或更高版本
- Android SDK 37
- Android SDK Build Tools 36.0.0
- 倉庫內的 Gradle Wrapper，無需單獨安裝系統 Gradle

在項目根目錄運行：

```shell
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

調試 APK 輸出至 `app/build/outputs/apk/debug/app-debug.apk`。依賴版本集中定義在 `gradle/libs.versions.toml` 中。

## 項目結構

```text
app/src/main/java/com/december172/daytiles/
├── data/              # Room 數據持久化與倉庫
├── domain/model/      # 獨立的日記、任務與貼紙模型
├── widget/            # Android 主畫面小工具
└── ui/
    ├── calendar/      # 月曆狀態、日期算法與界面
    ├── components/    # 共用工具欄與主導覽
    ├── day/           # 每日日記、任務快速輸入與完整編輯
    ├── planner/       # 目標、課程表、小工具、專注時鐘與規劃工具
    └── theme/         # Material 3 主題
```

## 隱私與聯網

DayTiles 的日曆、日記、任務、課程表和專注功能均可離線使用。只有在你主動通過網址下載貼紙時才需要聯網；下載的貼紙儲存在應用私有的本地存儲空間。

應用不要求帳號，不包含廣告、統計分析、追蹤、雲服務或後端依賴。日記、任務、貼紙和目標使用獨立的數據模型，數據庫升級會保留已有數據。

## 致謝

- **December172**：項目創建與維護。
- **OpenAI Codex / ChatGPT**：開發協作、功能實現、測試與文檔。
- **Microsoft Fluent System Icons**：導覽圖標來自 [Fluent UI System Icons](https://github.com/microsoft/fluentui-system-icons)，詳見[第三方許可](THIRD_PARTY_LICENSES.md)。

貢獻説明見 [CONTRIBUTORS.md](CONTRIBUTORS.md)。
