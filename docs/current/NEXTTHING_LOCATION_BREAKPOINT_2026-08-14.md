# NextThing 地点与围栏断点（2026-08-14）

## 本次完成

- 地图已由高德 2D `map2d:6.0.0` 迁移到 3D 合包 SDK `navi-3dmap-location-search:11.2.000_3dmap11.2.000_loc11.2.000_sea9.8.0`；旧的独立 2D 地图、定位、搜索依赖已移除。
- 选点页已切换至 `com.amap.api.maps`，并补齐 `MapView` 的前后台生命周期转发；应用启动时会在构造地图前调用 3D 地图隐私状态 API。
- 新建地点保存会写入 `LocationInfo` 与 `GeofenceLocation`。
- 从既有任务的地理围栏入口进入时，保存后立即创建 `TaskGeofence`，并向任务编辑态回传已选围栏地点。
- 地图选点页优先使用新建地点页已成功定位的坐标作为初始镜头；只有点击“回到当前位置”才再次定位。
- 高德定位 Manifest 已声明 `com.amap.api.location.APSService`；搜索 SDK 初始化前会设置隐私状态。
- Google Play Services 系统围栏注册已停用；当前有效提醒链路为高德定位与 Worker 距离检查。

## 已验证

- `:app:compileDebugKotlin --no-daemon --console=plain -Pksp.incremental=false` 于 2026-08-14（迁移后）编译通过。

## 2026-08-14 地图白底修复

- 已在真机复现：定位与逆地理编码成功，但地图底图白屏。
- 根因：`MapPickerScreen` 中 `MapView` 创建后触发 Compose 重组，旧 `DisposableEffect` 的清理 lambda 读取到新实例并提前调用 `onPause()` / `onDestroy()`，使刚创建的地图被销毁。
- 修复：稳定持有单个 `MapView` 实例；只在页面真正离开时销毁，并由生命周期观察者转发前后台事件。
- 已排除：Manifest 已有网络权限；独立复现日志中没有高德 Key 鉴权、地图网络或 GL 崩溃错误。
- 编译：`:app:compileDebugKotlin --no-daemon --console=plain -Pksp.incremental=false` 通过。

## 2026-08-14 选点页视觉收口

- 地图不再进入状态栏、标题和搜索区域；顶部改为固定白色导航与搜索区。
- 右侧“回到当前位置”由图钉改为准星图标，避免与中心选点图钉混淆。
- 底部确认卡新增拖拽把手，保留“已选位置 / 地址 / 确认此位置”的确认链路。
- 最新布局：顶部只保留一行“返回按钮 + 搜索框”，已移除独立“选择地点”标题；地图从该行下方开始。
- 编译：`:app:compileDebugKotlin --no-daemon --console=plain -Pksp.incremental=false` 通过。

## 待真机回归

1. 任务属性 → 地理围栏 → 新建地点 → 使用当前位置 → 保存：提示“已创建并绑定”，返回后新地点被选中。
2. 新建地点拿到实时位置后进入选择地点：地图中心应是该实时坐标，而不是北京兜底坐标。
3. 搜索、拖动地图、确认位置、保存地点的回填与持久化。

## 仍不能主张

- 项目尚未实现“展示并记录用户同意隐私政策”的产品流程；现有 SDK 初始化参数不能替代该流程。
- 当前日志未包含 `AmapLocationService` 输出，无法据此证明 Key 的 SHA1/包名配置是否覆盖所有签名包。
