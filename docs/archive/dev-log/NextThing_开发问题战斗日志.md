# NextThing 开发问题战斗日志

## 2026-07-12 SER 真机闭环与 Hexagon NPU 落地

问题现象：
SER 最初因 MFCC `AudioEvent.floatBuffer` 为空而失败；改用 NNAPI 后只能证明 Delegate 创建成功，不能证明模型实际运行在物理 NPU。

解决：
MFCC 改为通过 `setFloatBuffer()` 注入音频帧；推理后端切换为 Qualcomm QNN LiteRT Delegate 2.34.0，指定 HTP、INT8、V73 Unsigned PD、HMX 和 Burst，并在 Manifest 声明 `libcdsprpc.so`。

真机证据：
`libcdsprpc.so loaded`；成功打开 `libQnnHtpV73Skel.so`；CDSP domain 3 remote handle 建立；`TfLiteQnnDelegate` 接管 22/24 个节点。

性能：
NPU median 4050us、avg 4117us；CPU median 约 144us。42KB 小模型的 NPU 调度成本高于 CPU，当前价值主要是完成真实 NPU 工程链路，而不是性能收益。

工程结论：
硬件执行路径从 CPU 升级到了 Hexagon NPU，但端到端决策属于负优化：纯推理延迟约从 0.14ms 上升到 4.05ms，约慢 28 倍。是否使用 NPU 必须同时看模型规模、算力密度、数据搬运和调度成本，不能只看硬件级别更高。

剩余问题：
中性语音仍可能输出 `SAD 100%`，需要继续验证数据预处理一致性、量化校准和模型泛化能力。

## 2026-07-05 SER 情绪识别不显示

问题现象：
语音创建任务后，确认页没有展示任何情绪感知内容。

关键证据：
真机 logcat 显示 `ser_model_float16.tflite` 加载失败：`CONV_2D failed to prepare`。替换 float32 后又发现旧 BiLSTM TFLite 虽能加载，但本地 LiteRT `invoke()` 失败。

根因：
原 float16 模型在当前 LiteRT/NNAPI 路径下不可用；旧 BiLSTM 转 TFLite 后的 `BIDIRECTIONAL_SEQUENCE_LSTM` 图不稳定，不适合继续作为端侧 NPU/int8 主路线。

解决：
先用 float32 模型验证加载链路，再训练 NPU 更友好的轻量 CNN SER，导出 int8 TFLite，模型从 543KB 降到约 42KB。

验证：
42KB CNN int8 TFLite 本地 LiteRT `load + invoke` 成功；项目编译通过，等待真机 AS Run 后抓 `SER-Flow` 验证 NPU 与推理结果。

面试价值：
端侧 AI 模型部署、TFLite 量化、NNAPI/NPU 验证、模型结构取舍、日志定位。

## 2026-07-05 SER 输入形状不匹配

问题现象：
SER 模型加载后，真实推理链路仍可能失败或无法正确利用语音时序信息。

关键证据：
模型输入详情显示 `inputShape=[1, 1, 39]`，`shape_signature=[1, -1, 39]`；代码最初直接送 `[39, frames]`，后来临时池化成 `[1,1,39]` 又会丢失时序。

根因：
Android 端 MFCC 输出格式是 `[39, frames]`，而模型期望的是 `[batch, frames, features]`。直接池化虽然能适配形状，但会损失语音情绪识别依赖的时序变化。

解决：
将 SER 输入转换为 `[1, frames, 39]`；在切换轻量 CNN int8 后固定为 `[1,80,39]`，不足补零，超出截断。

验证：
`compileDebugKotlin` 通过；后续真机抓日志确认 `SER 输入准备完成` 与 `端侧推理开始` 的输入形状。

面试价值：
音频特征工程、端侧模型输入适配、动态形状与固定形状取舍。

## 2026-07-05 NPU 不是口头能力，需要日志和 benchmark 证明

问题现象：
设备芯片有高通 NPU，但不能只说“模型跑在 NPU 上”，需要证据证明。

关键证据：
logcat 可看到 `Created TensorFlow Lite delegate for NNAPI` 与 `模型加载完成: 加速器: NPU`，但这只能说明走了 NNAPI Delegate 路径，不完全等同于物理 Hexagon NPU 执行。

根因：
Android NNAPI 可能存在内部 fallback；Delegate 创建成功、模型加载成功、真实 NPU 执行是三个不同层次。

解决：
在 Debug 包中增加 SER 加速器 benchmark：NPU/GPU/CPU 分别跑多次推理，输出 median/avg/min/max，并保留真机日志证据。

验证：
代码已接入 benchmark 日志，等待用户 AS Run 后抓取 `SER 加速器基准` 日志。

面试价值：
端侧 AI 性能验证、NNAPI 与硬件 NPU 的边界、工程化 benchmark 方法。

补充记录：
benchmark 最初按请求的加速器记名，如果 NPU 加载失败后内部降级 CPU，日志可能误标为 NPU。已修正为同时记录 requested 与 actual，加速器结论以后以 actual 为准。

## 2026-07-05 DeepSeek 401 错误用户不可理解

问题现象：
创建任务时提示 `AI task parsing failed HTTP 401`，普通用户无法理解。

关键证据：
用户在真机创建任务时遇到 401，页面展示偏程序员风格的错误信息。

根因：
服务端 AI 错误直接透传技术异常，没有区分登录态过期、DeepSeek API Key 无效、额度不足、请求频繁等用户可处理场景。

解决：
在 AI 请求层做错误映射：401、402、429、5xx 转为用户能理解的中文提示；创建页提供去 AI 设置的入口。

验证：
创建页错误卡片改为完整展示，不再截断，并提供 `去 AI 设置` 操作。

面试价值：
AI 产品化错误处理、用户体验、服务端 AI 配置闭环。

## 2026-07-05 创建任务页 DeepSeek 状态语义不清

问题现象：
创建页里“AI 自动解析”像一个普通开关，用户不知道 DeepSeek 是否真的可用。

关键证据：
用户反馈“服务端 AI 启用状态一定表示可用，而不是简单开关”；配置后返回创建页需要状态自动刷新。

根因：
UI 没有区分“功能开关”和“服务端 AI 可用状态”，导致用户不清楚失败原因和下一步动作。

解决：
去掉右侧开关，改为右侧状态文案：`DeepSeek 配置已启用` / `DeepSeek 配置未启用`。未启用时显示 `去设置`，从配置页返回后自动刷新状态。

验证：
创建页状态栏已调整；生命周期 `ON_RESUME` 刷新 AI 路由状态。

面试价值：
AI 功能状态设计、配置闭环、Compose 状态刷新。

## 2026-07-05 统计页信息过载

问题现象：
统计页顶部存在多层 Tab，概览、结构、效率等信息堆叠，页面显得杂乱。

关键证据：
用户多次反馈不想要两个 Tab，认为统计页面功能太多，应拆成多个清晰页面。

根因：
把多种分析能力都压在一个页面内切换，导致入口层和内容层混杂，用户认知负担高。

解决：
统计首页改为概览 + 专项分析入口，拆出任务结构、趋势分析、效率诊断、AI 周报等独立页面。

验证：
统计详情页返回按钮统一现有样式；`compileDebugKotlin` 通过。

面试价值：
复杂页面信息架构、Compose 页面拆分、用户反馈驱动重构。

## 2026-07-05 我的页冻结头部交互

问题现象：
我的页面整体向下滑动时，头像区域和四个格子也跟着滑动，不符合用户想要的冻结表头效果。

关键证据：
用户明确要求“头像区域、下面四个格子不能滑动，AI 增强及以下列表滑动”，类似 Excel 冻结表头。

根因：
页面滚动容器层级设计不清，固定区和列表区没有分离。

解决：
将顶部个人信息和四格概览作为固定区域，下方 `AI 增强` 及以下内容独立滚动，并缩小底部多余留白。

验证：
页面交互按冻结头部模式调整。

面试价值：
Compose 滚动布局、复杂页面交互、移动端信息分层。
