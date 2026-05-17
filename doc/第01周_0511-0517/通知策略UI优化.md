# 通知策略 UI 优化

## 问题背景

创建任务页面通知策略和地理围栏各占一整行，空间浪费；新建策略页震动/声音/通知方式每个选项独占一行带描述文字，信息密度极低需要大量滚动；首次使用无预置策略需要自己创建；试听按钮在勿扰模式下崩溃、静音模式下无反馈。

## 问题根源

1. **布局浪费**：`CollapsibleConfigSection` 第四行通知策略 `fillMaxWidth()` 独占一行，地理围栏在外面独立 `Box`，两行 80dp 只展示两个按钮
2. **信息密度低**：VibrationSection/SoundSection/NotificationModeSection 用 `forEach` 遍历枚举，每个选项是一整行 Row（icon + name + description + checkmark），4 个震动选项 × ~56dp ≈ 224dp，加标题和间距 ≈ 280dp，整个页面需要大量滚动
3. **无预置策略**：`NotificationStrategyRepository` 没有初始化逻辑，首次使用列表为空
4. **弹窗固定高度**：`ModalBottomSheet` 内部用 `fillMaxHeight(0.6f)` 固定 60% 屏幕高度，策略少时大面积空白
5. **试听崩溃**：`playSoundWithUri()` 调用 `AudioManager.setStreamVolume()` 修改系统音量，勿扰模式下抛 `SecurityException: Not allowed to change Do Not Disturb state`；静音模式下 `ringtone.play()` 不发声但无用户提示

## 修改思路

### 1. 通知策略+地理围栏并排

把 `TaskGeofenceCard` 移进 `CollapsibleConfigSection` 第四行，和 `NotificationStrategyConfigCard` 并排放在 `Row(weight(1f))` 中。删掉外部独立的 `Box` 包裹。给 `TaskGeofenceCard` 加 `height(80.dp)` 和通知策略卡片等高。

### 2. 新建策略页紧凑布局

新增 `CompactSelectionSection` 通用组件：标题 + 一行 chip 横排选择。替代原来的列表式选择。震动 4 选项、声音 3 选项、通知方式 3 选项各占一行 chip。音量条从独立大卡片改为内嵌单行 `音量 [slider] 50% [▶]`。

### 3. 预置默认通知策略

`NotificationStrategyRepositoryImpl` 新增 `ensurePresetStrategies()`：查 count=0 时插入 3 条预置策略（`preset_silent`/`preset_standard`/`preset_important`）。在 `CreateTaskViewModel.loadNotificationStrategies()` 里先调用 ensure 再 collect。

### 4. 弹窗自适应高度

`fillMaxHeight(0.6f)` → `wrapContentHeight(Alignment.Top).heightIn(max = 500.dp)`。

### 5. 试听按钮修复

- 去掉 `AudioManager.setStreamVolume()` 调用（试听不应改系统音量）
- 播放前检测 `audioManager.ringerMode`，静音/震动模式弹 Toast 提示

### 涉及文件

| 文件 | 改动 |
|------|------|
| `CreateTaskScreen.kt` | 第四行并排 + 地理围栏参数传入 CollapsibleConfigSection |
| `TaskGeofenceCard.kt` | 加 `height(80.dp)` |
| `CreateNotificationStrategyScreen.kt` | 全面重写为紧凑布局 |
| `NotificationStrategyRepositoryImpl.kt` | 新增 `ensurePresetStrategies()` |
| `NotificationStrategyRepository.kt` | 接口新增方法 |
| `NotificationStrategyDao.kt` | 新增 `insertAll()` |
| `CreateTaskViewModel.kt` | loadNotificationStrategies 加 ensure 调用 |
| `CreateNotificationStrategyViewModel.kt` | 试听逻辑修复 + 静音检测 |

## 经验总结

1. **UI 信息密度**：ToC 应用（滴答清单/Todoist/Google Tasks）的设置页面通常把选项做成 chip 横排而非竖向列表。选项少于 5 个时，横排比列表节省 60%+ 空间，用户一屏看完不需要滚动。

2. **AudioManager 勿扰模式**：Android 勿扰模式（DND）下 `setStreamVolume()` 会抛 `SecurityException`。需要 `ACCESS_NOTIFICATION_POLICY` 权限才能绕过。试听/预览类功能不应修改系统音量，只播放声音即可。

3. **静音检测**：`AudioManager.getRingerMode()` 返回三种值：`RINGER_MODE_SILENT`(0)、`RINGER_MODE_VIBRATE`(1)、`RINGER_MODE_NORMAL`(2)。试听前检查并给用户反馈。

4. **预置数据策略**：首次启动时检查数据表是否为空（count=0），为空才插入预置数据。用 `OnConflictStrategy.IGNORE` 防止重复插入。预置数据的 ID 用固定前缀（如 `preset_`）便于和用户自建数据区分。

5. **面试话术角度**：可以讲"通知系统的设计"——预置策略降低使用门槛、紧凑布局提升信息密度、音量试听的边界情况处理（勿扰/静音/权限），体现对用户体验细节的关注。
