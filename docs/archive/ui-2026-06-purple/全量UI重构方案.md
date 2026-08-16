# NextThing 全量 UI 重构方案

> 建立日期：2026-06-12
>
> 当前目标：先完成 UI 工程化和全页面视觉统一，再进入性能专项。

## 1. 扫描结论

### 页面规模

- 导航实际覆盖约 23 个页面/子页面。
- `presentation/screens` 下共有 26 个 `*Screen.kt`，合计约 24,900 行。
- 超大页面：
  - `StatsScreen.kt`：约 5,232 行
  - `CreateTaskScreen.kt`：约 2,742 行
  - `TaskDetailScreen.kt`：约 1,770 行
  - `TasksScreen.kt`：约 1,223 行
  - `TodayScreen.kt`：约 1,144 行
  - `SettingsScreen.kt`：约 1,124 行

### 当前主要问题

1. **设计语言分裂**
   - `MaterialTheme` 仍保留旧天蓝色。
   - `AppColors` 使用 AI 紫蓝色。
   - 新创建任务页单独硬编码蓝色。

2. **Design Token 没有真正落地**
   - UI 层约有 413 处硬编码颜色。
   - 约有 896 处直接设置 `fontSize`。
   - 约有 372 处直接创建 `RoundedCornerShape`。

3. **共享组件不足**
   - 不同页面各自实现 TopBar、卡片、按钮、输入框、空状态和错误状态。
   - BottomNavigationBar 仍使用 Android 系统旧图标。
   - 阴影、间距和点击反馈缺乏统一规范。

4. **页面文件过大**
   - 页面结构、图表、弹窗和业务交互混在同一文件。
   - 全量换肤时容易误伤业务逻辑，代码审查和回归成本高。

5. **设计文档覆盖不完整**
   - 现有文档覆盖 12 个主页面。
   - 分类编辑、重复设置、通知策略、同步、地图选择等子页面缺少统一设计规范。

## 2. 重构原则

1. 不修改 ViewModel、Repository、数据库和网络行为，除非 UI 接入必须。
2. 每次只改一个页面族，编译和真机验收后再进入下一批。
3. 新页面禁止新增硬编码颜色、字号、圆角和间距。
4. 创建任务页作为交互基准，但颜色迁移到统一 AI 紫蓝 Token。
5. 先建立组件，再改页面；不允许在每个页面复制一套“现代化”组件。
6. UI 全量完成前，暂停 ASR 内存和启动性能专项。

## 3. 目标设计系统

### Theme

建议整理为：

```text
presentation/theme/
├── Color.kt
├── Theme.kt
├── Type.kt
├── Shape.kt
├── Spacing.kt
├── Elevation.kt
└── Motion.kt
```

统一规则：

- 品牌主色：AI 紫蓝 `#6C5CE7`
- 页面背景、卡片、输入框、边框全部使用语义 Token。
- 天气主题只替换页面氛围色，不替换 AI 相关组件的品牌色。
- 深色模式由同一套语义 Token 派生。

### Foundation Components

第一阶段必须先完成：

```text
presentation/components/design/
├── AppScreen.kt
├── AppTopBar.kt
├── AppBottomNavigation.kt
├── AppCard.kt
├── AppButton.kt
├── AppTextField.kt
├── AppChip.kt
├── AppListItem.kt
├── AppSectionHeader.kt
├── AppDialog.kt
└── AppStateView.kt
```

组件职责：

- `AppScreen`：背景、安全区、页面边距和滚动约束。
- `AppTopBar`：标题、返回、右侧操作统一。
- `AppCard`：圆角、边框、阴影、内边距统一。
- `AppButton`：Primary、Secondary、Ghost、Danger 四种类型。
- `AppStateView`：Loading、Empty、Error、PermissionDenied 四种状态。
- `AppBottomNavigation`：统一图标、选中态和中央创建入口。

## 4. 页面分批

### Phase 0：UI 基座

目标：建立 Design Token、共享组件和 Preview 展示页。

验收：

- 浅色、深色、天气主题均能预览。
- 组件不依赖具体业务 ViewModel。
- 新页面不再直接使用 `Color(0x...)`。

### Phase 1：五个主导航页面

顺序：

1. 今日页
2. 任务列表
3. 创建任务页收口
4. 统计页
5. 我的页
6. 底部导航

原因：这是用户最高频路径，先统一它们才能形成完整产品观感。

重点：

- 今日页：问候区、统计卡片、任务卡片、空状态。
- 任务列表：搜索、筛选、分组、批量操作。
- 创建任务：移除独立蓝色，接入统一 Token 和共享组件。
- 统计页：先拆文件，再统一卡片和图表容器。
- 我的页：用户卡片、设置分组和入口列表。

### Phase 2：核心任务闭环

1. 启动页
2. 登录页
3. 任务详情
4. 日历
5. 专注模式

验收链路：

`启动 -> 登录 -> 今日 -> 创建 -> 任务详情 -> 完成任务`

### Phase 3：设置与表单页面

1. 分类管理 / 分类编辑
2. 通知策略
3. 自定义重复
4. 视图偏好
5. 主题设置
6. 用户信息
7. 数据同步
8. 成就

统一重点：

- 表单行、选择器、开关、分组标题、确认按钮和危险操作。

### Phase 4：地理围栏页面族

1. 地理围栏配置
2. 新增地点
3. 地点详情
4. 关联任务
5. 地图选择
6. 创建地点

原因：页面数量多、状态复杂、地图依赖重，放在稳定设计系统之后处理。

## 5. 单页标准流程

每个页面严格执行：

1. 核对设计文档与当前功能。
2. 列出页面状态：正常、加载、空、错误、权限拒绝。
3. 将纯 UI 提取为 `XxxContent(uiState, callbacks)`。
4. 接入共享组件和 Design Token。
5. 保持原 ViewModel 与业务回调不变。
6. 添加至少一个 Compose Preview。
7. 编译并安装真机。
8. 验证导航、返回、键盘、滚动、深色模式和核心操作。
9. 截图留档并更新本方案进度。

## 6. 文件拆分要求

- 单个 Screen 文件建议控制在 500 行以内。
- 超过 800 行必须拆分。
- 图表、弹窗、表单区块和复杂卡片进入独立文件。
- 不为了行数制造无意义抽象；按可独立预览、可复用或复杂状态边界拆分。

`StatsScreen.kt` 建议拆为：

```text
stats/
├── StatsScreen.kt
├── StatsOverviewContent.kt
├── StatsSummaryCards.kt
├── StatsCharts.kt
├── StatsAIInsightCard.kt
└── StatsModels.kt
```

## 7. 验收标准

### 视觉

- 页面使用同一品牌色、圆角、间距、字体层级和阴影。
- 不再出现 Android 系统旧图标。
- 浅色、深色和天气主题文字对比度正常。

### 交互

- 点击区域不小于 48dp。
- Loading、Empty、Error 状态完整。
- 输入页面支持键盘和 IME 遮挡处理。
- 页面返回和底部导航行为一致。

### 工程

- `compileDebugKotlin` 通过。
- 每批改造完成后 APK 可安装。
- 核心业务链路真机通过。
- 不引入新的硬编码视觉值。
- UI 改造与业务逻辑修改分开提交。

## 8. 推荐执行节奏

### 第 1 批

- Design Token
- 共享组件
- 底部导航
- 今日页

### 第 2 批

- 任务列表
- 创建任务页收口
- 任务详情

### 第 3 批

- 统计页拆分与重构
- 我的页
- 启动与登录

### 第 4 批

- 日历、专注、成就
- 设置与表单页面族

### 第 5 批

- 地理围栏与地图页面族
- 全量视觉回归

## 9. 当前下一步

下一次开发从 **Phase 0 + 今日页** 开始：

1. 建立 Shape、Spacing、Elevation、Motion Token。
2. 实现第一批共享组件。
3. 重做 BottomNavigationBar。
4. 使用新基座重构 TodayScreen。

创建任务页暂时作为视觉参考，不继续独立堆叠样式。
