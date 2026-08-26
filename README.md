# 淨機 Tidy

給 OPPO（ColorOS，Android 14）的儲存與電池整理工具。設計原則是**只做系統真的允許做的事，並且說清楚哪些做不到**。

---

## 一、產出 APK（不需要安裝 Android Studio）

1. 到 GitHub 建立一個新的 repository，名稱隨意，可設為 Private。
2. 把這個資料夾裡的**全部檔案**（含 `.github` 資料夾）上傳到該 repository 的 `main` 分支。
   - 網頁上傳：進入 repo → `Add file` → `Upload files` → 把整個資料夾拖進去 → `Commit changes`。
   - 注意：GitHub 網頁上傳有時會忽略 `.github` 這種以點開頭的資料夾。若上傳後在 repo 看不到它，請改用 `Add file` → `Create new file`，檔名直接輸入 `.github/workflows/build-apk.yml`，再把該檔內容貼上。
3. 上傳完成後，打開 repo 的 **Actions** 分頁。第一次可能需要按一下 `I understand my workflows, go ahead and enable them`。
4. 選左側的 **Build APK** → 右側 `Run workflow` → 綠色按鈕 `Run workflow`。
5. 等待約 4～8 分鐘（第一次較久，之後有快取會快很多）。跑完後點進該次執行，頁面最下方 **Artifacts** 區塊會有 `tidy-apk`，下載它。
6. 解壓縮後得到 `app-release.apk`，傳到手機安裝。

### 手機端安裝
ColorOS 會擋來路不明的安裝檔。用檔案管理員點開 APK 時，依提示允許「安裝未知應用程式」即可。

---

## 二、第一次開啟要做的三件事

| 項目 | 用途 | 沒開會怎樣 |
|---|---|---|
| 所有檔案存取權 | 掃描共用儲存空間 | 無法掃描，其他功能正常 |
| 使用權限存取 | 電池分頁的耗電推估 | 電池分頁只剩基本讀數 |
| Shizuku（選配） | 真正清除全機快取、結束背景程序 | 專家模式收起，其餘不受影響 |

### Shizuku 設定（選配）
1. 安裝 Shizuku（Play 商店或 GitHub）。
2. 手機開啟「開發人員選項」→「無線偵錯」。
3. 在 Shizuku App 裡用無線偵錯配對並啟動。
4. 回到淨機，首頁「專家模式」按「向 Shizuku 請求授權」。
5. **手機重開機後 Shizuku 需要重新啟動**，這是它的運作方式，不是故障。

---

## 三、這個 App 明確做不到的事

寫在這裡是因為市面上很多同類 App 不會說：

- **無法終止其他 App 的程序**。Android 8 起就封了。首頁的記憶體數字是真的，但「一鍵加速」在一般權限下只是動畫。要真的做到，只能走 Shizuku。
- **無法讀取 `/Android/data` 與 `/Android/obb`**。Android 11 起對所有第三方 App 關閉，拿到「所有檔案存取權」也一樣。多數 App 的快取放在這裡，所以清那部分要靠系統對話框或 Shizuku 的 `pm trim-caches`。
- **無法讀取各 App 的實際耗電 mAh**。電池分頁用前景使用時間推估，並在畫面上註明。

---

## 四、安全設計

- 刪除的檔案不會立刻消失，而是移到 App 私有目錄的回收桶，保留 **30 天**，隨時可全部還原。
- 空資料夾沒有資料，直接移除，不進回收桶。
- 大型檔案、殘留安裝檔、備份殘留三類**預設不勾選**，需要手動確認。
- 每個類別都可以展開看到實際路徑再決定。

---

## 五、專案結構

```
app/src/main/java/tw/vic/tidy/
├── MainActivity.kt          三分頁外框與 Shizuku 授權回呼
├── TidyViewModel.kt         全部狀態與非同步流程
├── core/
│   ├── JunkScanner.kt       檔案掃描與分類、重複檔比對
│   ├── RecycleBin.kt        30 天回收桶與還原
│   ├── SystemStats.kt       儲存／記憶體／電池讀數
│   ├── UsageRepo.kt         前景使用時間統計
│   ├── Perms.kt             權限檢查與系統設定深連結
│   ├── ShizukuBridge.kt     選配的 shell 執行層
│   └── Fmt.kt               容量與時間格式
└── ui/
    ├── HomeScreen.kt        剖面圖、機況、系統交辦、專家模式
    ├── CleanScreen.kt       分類勾選與確認
    ├── BatteryScreen.kt     電池讀數與耗電推估
    └── components/          剖面圖元件、共用版面元件、自繪圖示
```

## 六、要改東西的話

- **調整判定規則**：`core/JunkScanner.kt` 最上方的 `TEMP_SUFFIXES`、`THUMB_NAMES`、`BACKUP_HINTS`、`BIG_FILE_THRESHOLD`。
- **調整配色**：`ui/theme/Theme.kt` 的 `T` 物件。
- **調整回收桶保留天數**：`core/RecycleBin.kt` 的 `RETENTION_DAYS`。
- **改版本號**：`app/build.gradle.kts` 的 `versionCode` / `versionName`。

APK 用 Gradle 自動產生的 debug 金鑰簽章，方便直接安裝。若日後要能覆蓋升級或上架，需要換成自己的 keystore。

---

## 七、建置失敗時的排查

- **Actions 分頁沒有 Build APK**：`.github/workflows/build-apk.yml` 沒有上傳成功，改用 `Create new file` 手動建立。
- **Set up Gradle 步驟失敗**：把 workflow 裡的 `gradle-version: '8.7'` 改成 `gradle-version: 'release'`。
- **相依套件下載失敗**：多半是暫時性網路問題，重跑一次 workflow 即可。
- **Shizuku 相依失敗**：若 `dev.rikka.shizuku` 抓不到，可先把 `app/build.gradle.kts` 裡那兩行 `implementation("dev.rikka.shizuku:...")` 註解掉，並移除 `core/ShizukuBridge.kt` 與 `AndroidManifest.xml` 中的 provider 區塊；其餘功能不受影響。
- **看不到 Artifacts**：只有在建置成功（綠色勾）時才會出現，失敗的執行不會產生檔案。
