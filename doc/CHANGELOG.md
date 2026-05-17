# NextThing 更新日志

简要记录每次代码变更，详细分析见对应周文件夹。

---

## 第01周 (05.11 - 05.17)

- [fix] 定位系统无法获取位置：权限检查逻辑 + 高德Key认证 + Google回退超时 → `doc/第01周_0511-0517/定位系统修复.md`
- [fix] 退出登录按钮无效：Token未清除、导航栈未清空 → `doc/第01周_0511-0517/登录登出及头像修复.md`
- [fix] 登录失败只显示"网络错误"：未解析HTTP 400响应体 → `doc/第01周_0511-0517/登录登出及头像修复.md`
- [fix] 设置页与用户信息页头像不一致：emoji与drawable混用 → `doc/第01周_0511-0517/登录登出及头像修复.md`
- [fix] Hilt编译错误：kapt→KSP迁移遗留问题（@ApplicationContext、@Inject constructor、BroadcastReceiver @EntryPoint） → `doc/第01周_0511-0517/Hilt编译错误修复.md`
- [chore] Gradle代理配置 + settings.gradle.kts仓库顺序调整
- [refactor] 通知策略+地理围栏并排展示：创建任务页两卡片各占一半宽度 → `doc/第01周_0511-0517/通知策略UI优化.md`
- [refactor] 新建策略页紧凑布局：震动/声音/通知方式从列表式改为chip横排 → `doc/第01周_0511-0517/通知策略UI优化.md`
- [feat] 预置默认通知策略：首次启动插入无声提醒/标准提醒/重要提醒 → `doc/第01周_0511-0517/通知策略UI优化.md`
- [fix] 通知策略弹窗自适应高度：去掉固定60%高度 → `doc/第01周_0511-0517/通知策略UI优化.md`
- [fix] 试听按钮静音模式崩溃：勿扰模式setStreamVolume抛SecurityException + 静音检测Toast → `doc/第01周_0511-0517/通知策略UI优化.md`
- [chore] 删除预置分类中的"字节"图标映射

---

## 第01周补充 — 三大 Feature (05.17)

- [feat] 云同步客户端+后端：SyncUseCases去stub + CRUD标记PENDING + SyncPreferences持久化 + 后端SyncController完整实现 → `doc/第01周_0511-0517/三大Feature实现.md`
- [feat] 今日任务桌面Widget：Glance App Widget，显示今日待办前5条，30分钟自动刷新 → `doc/第01周_0511-0517/三大Feature实现.md`
- [feat] 日历月视图：月历网格+日期标点+任务列表，从Today页日历图标进入 → `doc/第01周_0511-0517/三大Feature实现.md`
- [refactor] DB迁移v10→v11：TaskEntity/CategoryEntity新增deleted软删字段，所有SELECT查询加deleted=0过滤
- [fix] Glance Widget编译失败：1.1.0降级到1.0.0（Aliyun镜像不完整）+ 补全clickable/actionStartActivity子包导入 → `doc/第01周_0511-0517/Glance和ANR问题修复.md`
- [fix] 启动ANR：SyncRepositoryImpl.init用runBlocking+collect读DataStore无限Flow，主线程永久阻塞 → `doc/第01周_0511-0517/Glance和ANR问题修复.md`
- [refactor] Widget改直查数据库：去掉Preferences GlanceState，provideGlance内直接查Room，避免StateDefinition配置问题
