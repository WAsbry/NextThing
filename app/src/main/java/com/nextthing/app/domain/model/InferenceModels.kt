package com.nextthing.app.domain.model

/**
 * 端侧 AI 推理相关数据类
 */

/**
 * 加速器类型，按优先级排列：NPU → GPU → CPU
 * 降级链逻辑通过 ordinal 遍历实现
 */
enum class Accelerator {
    NPU,    // 通过 NNAPI Delegate 走 NPU
    GPU,    // 通过 GPU Delegate 走 GPU
    CPU     // 纯 CPU 推理
}

/**
 * 模型配置
 * @param modelFileName assets/models/ 下的模型文件名
 * @param accelerator 指定使用的加速器
 * @param numThreads CPU 推理时的线程数
 */
data class ModelConfig(
    val modelFileName: String,
    val accelerator: Accelerator = Accelerator.NPU,
    val numThreads: Int = 4
)

/**
 * 单次推理结果
 * @param data 推理输出数据（如分类概率数组）
 * @param latencyMs 推理耗时（毫秒）
 * @param accelerator 实际使用的加速器
 */
data class InferenceResult(
    val data: Any,
    val latencyMs: Long,
    val accelerator: Accelerator
)

/**
 * 单个加速器的 Benchmark（基准测试）结果
 *
 * @DESC: 记录某一种加速器（NPU/GPU/CPU）跑多次推理的耗时
 * 1. 存储属性：accelerator（加速器类型）、latencyMsList（每次推理耗时列表）
 * 2. 计算属性：medianLatencyMs（中位数延迟，由 latencyMsList 排序取中间值得出）
 *
 * @Parma: accelerator — 加速器类型（NPU/GPU/CPU）
 * @Parma: latencyMsList — 多次推理的耗时记录（毫秒）
 *
 * 补充：用中位数不用平均值，因为平均值容易被极端值（如 GC 暂停导致的 200ms）拉偏
 *       例：49 次 5ms + 1 次 200ms → 平均 8.9ms（失真），中位数 5ms（准确）
 */
data class SingleAcceleratorBenchmark(
    val accelerator: Accelerator,
    val latencyMsList: List<Long>
) {
    val medianLatencyMs: Long
        get() = latencyMsList.sorted().let { it[it.size / 2] }                                     // 计算属性：排序后取中间值，即中位数
}

/**
 * 完整 Benchmark 结果，包含三种加速器的对比
 */
data class BenchmarkResult(
    val results: List<SingleAcceleratorBenchmark>
)
