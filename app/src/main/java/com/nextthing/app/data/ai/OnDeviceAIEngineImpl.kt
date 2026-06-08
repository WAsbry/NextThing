package com.nextthing.app.data.ai

import android.content.Context
import android.os.Build
import com.nextthing.app.domain.model.Accelerator
import com.nextthing.app.domain.model.BenchmarkResult
import com.nextthing.app.domain.model.InferenceResult
import com.nextthing.app.domain.model.ModelConfig
import com.nextthing.app.domain.model.SingleAcceleratorBenchmark
import com.nextthing.app.domain.service.OnDeviceAIEngine
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import timber.log.Timber
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 端侧 AI 推理引擎实现
 *
 * 基于 LiteRT 1.4.1 Interpreter API
 * 降级链：NPU（NNAPI, API 31+）→ GPU → CPU（4 threads）
 */
@Singleton
class OnDeviceAIEngineImpl @Inject constructor(
    private val context: Context
) : OnDeviceAIEngine {

    private var interpreter: Interpreter? = null
    private var currentConfig: ModelConfig? = null

    // Delegate 引用，用于释放
    private var nnapiDelegate: NnApiDelegate? = null
    private var gpuDelegate: GpuDelegate? = null

    /**
     * @DESC: 加载 .tflite 模型文件并初始化 Interpreter
     * 1. 释放已有模型（防止重复加载导致内存泄漏）
     * 2. 从 assets 读取模型文件到 MappedByteBuffer
     * 3. 根据 config 创建 Interpreter.Options（配置 Delegate 和线程数）
     * 4. 创建 Interpreter 实例
     *
     * @Parma: config — 模型配置，包含文件名、加速器类型、线程数
     *
     * 补充：每次加载新模型前会先 release 旧的，保证同时只有一个模型在内存中
     */
    override suspend fun loadModel(config: ModelConfig) {
        release()                                                                                      // 先释放已有模型，防止内存泄漏
        val buffer = loadModelFile(config.modelFileName)                                               // 从 assets 读取 .tflite 到 MappedByteBuffer
        val options = createInterpreterOptions(config)                                                 // 根据加速器类型创建 Delegate 配置
        interpreter = Interpreter(buffer, options)                                                     // 创建解释器：加载模型 + 应用硬件配置
        currentConfig = config                                                                         // 保存当前配置，供 infer 等方法使用
        Timber.d("模型加载完成: ${config.modelFileName}, 加速器: ${config.accelerator}")                  // 日志：记录加载结果
    }

    /**
     * @DESC: 执行一次模型推理，返回结果和耗时
     * 1. 检查 interpreter 和 config 是否存在（未加载则抛异常）
     * 2. 创建输出容器（根据模型输出维度）
     * 3. 调用 interpreter.run() 执行推理并计时
     * 4. 封装结果（输出数据 + 耗时 + 使用的加速器）
     *
     * @Parma: input — 输入数据（如 float[1][224][224][3]：1=batch数量, 224=高, 224=宽, 3=RGB通道）
     *
     * 补充：run() 是同步阻塞的，suspend 标记提醒调用方应在协程中调用
     */
    override suspend fun infer(input: Any): InferenceResult {
        val interp = interpreter
            ?: throw IllegalStateException("模型未加载，请先调用 loadModel()")                         // Elvis 运算符：为 null 则抛异常
        val config = currentConfig
            ?: throw IllegalStateException("模型配置缺失")                                                // 同上
        val startTime = System.nanoTime()                                                               // 记录开始时间（纳秒级精度）
        val output = createOutputContainer(interp)                                                      // 根据模型输出维度创建对应大小的数组容器
        interp.run(input, output)                                                                       // 执行推理：input → 模型 → output
        val latencyMs = (System.nanoTime() - startTime) / 1_000_000                                    // 计算耗时，纳秒转毫秒
        return InferenceResult(
            data = output,                                                                              // 推理输出数据
            latencyMs = latencyMs,                                                                      // 推理耗时（毫秒）
            accelerator = config.accelerator                                                            // 实际使用的加速器
        )
    }

    /**
     * @DESC: 性能测试，用三种加速器（NPU/GPU/CPU）各跑若干次推理，对比延迟
     * 1. 遍历三种 Accelerator，依次加载模型
     * 2. 创建 dummy 输入（全 0.5f，只测速度不关心结果）
     * 3. 预热 5 次（排除首次初始化开销）
     * 4. 正式跑 iterations 次推理，记录每次耗时
     * 5. 汇总结果，某种加速器失败则跳过不影响其他
     *
     * @Parma: modelFileName — 模型文件名
     * @Parma: iterations — 每种加速器跑多少次，默认 50
     *
     * 补充：用中位数不用平均值，因为平均值容易被极端值（如 GC 暂停）拉偏
     */
    override suspend fun benchmark(
        modelFileName: String,
        iterations: Int
    ): BenchmarkResult {
        val results = mutableListOf<SingleAcceleratorBenchmark>()                                       // 收集三种加速器的测试结果

        for (accelerator in Accelerator.entries) {                                                      // 遍历 NPU → GPU → CPU
            val config = ModelConfig(
                modelFileName = modelFileName,
                accelerator = accelerator
            )

            try {
                loadModel(config)                                                                       // 加载模型（含 Delegate 配置）
                val interp = interpreter!!                                                              // 非空断言：刚加载完一定不为 null
                val input = createDummyInput(interp)                                                    // 创建 dummy 输入（全 0.5f）

                repeat(5) {                                                                             // 预热 5 次，排除首次初始化开销
                    val output = createOutputContainer(interp)
                    interp.run(input, output)
                }

                val latencies = mutableListOf<Long>()                                                   // 记录每次推理耗时
                repeat(iterations) {                                                                    // 正式跑 iterations 次推理
                    val startTime = System.nanoTime()
                    val output = createOutputContainer(interp)
                    interp.run(input, output)
                    latencies.add((System.nanoTime() - startTime) / 1_000_000)                         // 纳秒转毫秒
                }

                results.add(
                    SingleAcceleratorBenchmark(
                        accelerator = accelerator,
                        latencyMsList = latencies
                    )
                )

                Timber.d(
                    "Benchmark ${accelerator.name}: " +
                            "中位数=${latencies.sorted()[latencies.size / 2]}ms, " +
                            "平均=${latencies.average().toLong()}ms"
                )
            } catch (e: Exception) {
                Timber.w(e, "Benchmark ${accelerator.name} 失败，跳过")                                  // 某种加速器失败不影响其他
            } finally {
                release()                                                                               // 每种加速器测完释放资源
            }
        }

        return BenchmarkResult(results = results)
    }

    /**
     * @DESC: 检测当前设备可用的最高优先级加速器
     * 1. 按枚举顺序遍历：NPU → GPU → CPU
     * 2. 尝试创建对应的 Delegate，第一个成功的就是最佳加速器
     * 3. 全部失败则返回 CPU（CPU 不需要 Delegate，一定能跑）
     *
     * 补充：通过遍历枚举 ordinal 实现降级链，不需要写死 if-else
     */
    override fun detectBestAccelerator(): Accelerator {
        for (accelerator in Accelerator.entries) {                                                      // 按优先级遍历：NPU → GPU → CPU
            if (canCreateDelegate(accelerator)) {                                                       // 尝试创建 Delegate
                Timber.d("检测到最佳加速器: ${accelerator.name}")
                return accelerator                                                                      // 第一个成功的就是最佳
            }
        }
        return Accelerator.CPU                                                                          // 兜底：CPU 一定能跑
    }

    override fun release() {
        interpreter?.close()
        interpreter = null

        nnapiDelegate?.close()
        nnapiDelegate = null

        gpuDelegate?.close()
        gpuDelegate = null

        currentConfig = null
    }

    // ========== 私有方法 ==========

    /**
     * @DESC: 从 assets 加载模型文件到 MappedByteBuffer
     * 1. 打开 APK 中的模型文件，获取文件描述信息
     * 2. 通过 NIO FileChannel 进行 mmap 内存映射
     *
     * @Parma: modelFileName — assets/models/ 下的模型文件名
     *
     * 补充：使用 MappedByteBuffer 而非普通 ByteBuffer，因为 mmap 不占 JVM 堆、懒加载、不触发 GC
     */
    private fun loadModelFile(modelFileName: String): MappedByteBuffer {
        val assetFd = context.assets.openFd("models/$modelFileName") // 打开 APK 内 assets/models/ 下的文件，拿到文件描述信息
        val inputStream = FileInputStream(assetFd.fileDescriptor)     // 根据描述信息创建文件输入流
        val fileChannel = inputStream.channel                         // 拿到 NIO 文件通道，支持 mmap 操作
        val startOffset = assetFd.startOffset                         // 文件在 APK 中的起始偏移量
        val declaredLength = assetFd.declaredLength                   // 文件的字节长度
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength) // mmap：将文件映射为 MappedByteBuffer，只读模式
    }

    /**
     * @DESC: 根据 ModelConfig 创建 Interpreter.Options，配置对应的硬件 Delegate
     * 1. NPU → 创建 NNAPI Delegate（要求 API 31+），否则降级 CPU
     * 2. GPU → 创建 GPU Delegate，创建失败则降级 CPU
     * 3. CPU → 不加 Delegate，设置线程数
     *
     * @Parma: config — 模型配置，包含加速器类型和线程数
     *
     * 补充：降级链 NPU → GPU → CPU，确保在任何设备上都能跑
     */
    private fun createInterpreterOptions(config: ModelConfig): Interpreter.Options {
        return Interpreter.Options().apply {
            when (config.accelerator) {
                Accelerator.NPU -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        nnapiDelegate = NnApiDelegate()
                        addDelegate(nnapiDelegate!!)
                    } else {
                        // API < 31，NPU 不可靠，降级到 CPU
                        Timber.w("API < 31, NNAPI 不可靠，降级到 CPU")
                        setNumThreads(config.numThreads)
                    }
                }
                Accelerator.GPU -> {
                    try {
                        gpuDelegate = GpuDelegate()
                        addDelegate(gpuDelegate!!)
                    } catch (e: Exception) {
                        Timber.w(e, "GPU Delegate 创建失败，降级到 CPU")
                        setNumThreads(config.numThreads)
                    }
                }
                Accelerator.CPU -> {
                    setNumThreads(config.numThreads)
                }
            }
        }
    }

    /**
     * @DESC: 检测能否创建对应加速器的 Delegate（不实际使用，创建后立即释放）
     * 1. NPU → 要求 API 31+，创建 NNAPI Delegate 验证
     * 2. GPU → 创建 GPU Delegate 验证
     * 3. CPU → 永远返回 true（不需要 Delegate）
     *
     * @Parma: accelerator — 要检测的加速器类型
     *
     * 补充：创建后立即 close() 释放，只是验证能不能创建，不占用资源
     */
    private fun canCreateDelegate(accelerator: Accelerator): Boolean {
        return try {
            when (accelerator) {
                Accelerator.NPU -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {                              // API 31+ 才尝试创建 NNAPI Delegate
                        val delegate = NnApiDelegate()
                        delegate.close()                                                                // 验证完立即释放
                        true
                    } else false                                                                        // API < 31，NPU 不可靠
                }
                Accelerator.GPU -> {
                    val delegate = GpuDelegate()
                    delegate.close()                                                                    // 验证完立即释放
                    true
                }
                Accelerator.CPU -> true                                                                 // CPU 不需要 Delegate，永远可用
            }
        } catch (e: Exception) {
            Timber.w(e, "创建 ${accelerator.name} Delegate 失败")
            false                                                                                       // 创建失败说明设备不支持
        }
    }

    /**
     * @DESC: 根据模型输出维度创建对应大小的数组容器，用于接收推理结果
     * 1. 从 Interpreter 获取模型第一个输出张量的形状（如 [1, 1000]）
     * 2. 根据维度数创建匹配的数组容器
     *
     * @Parma: interp — 已加载模型的 Interpreter 实例
     *
     * 补充：LiteRT 的 run() 要求调用方预先创建好输出容器，它不会帮你创建
     */
    private fun createOutputContainer(interp: Interpreter): Any {
        val outputShape = interp.getOutputTensor(0).shape()                                            // 获取模型输出张量的形状（如 [1, 1000]）
        return when (outputShape.size) {
            2 -> Array(outputShape[0]) { FloatArray(outputShape[1]) }                                  // 二维：如 [1, 1000] → Array(1) { FloatArray(1000) }
            1 -> FloatArray(outputShape[0])                                                            // 一维：如 [1000] → FloatArray(1000)
            else -> FloatArray(outputShape.last())                                                     // 兜底：取最后一维的大小
        }
    }

    /**
     * @DESC: 创建全 0.5f 的 dummy 输入，用于 Benchmark（只测速度不关心结果）
     * 1. 从 Interpreter 获取模型输入张量的形状
     * 2. 根据维度创建对应形状的全 0.5f 数组
     *
     * @Parma: interp — 已加载模型的 Interpreter 实例
     *
     * 补充：为什么用 0.5f 不用 0f — 避免全零输入导致某些模型计算异常（如除零）
     *       通用方法，适配不同模型的不同输入维度（1/2/4 维）
     */
    private fun createDummyInput(interp: Interpreter): Any {
        val inputShape = interp.getInputTensor(0).shape()                                              // 获取模型输入张量的形状（如 [1, 224, 224, 3]）
        return when (inputShape.size) {
            4 -> Array(inputShape[0]) {                                                                 // 四维：如 [1, 224, 224, 3] → batch
                Array(inputShape[1]) {                                                                  // height
                    Array(inputShape[2]) {                                                              // width
                        FloatArray(inputShape[3]) { 0.5f }                                             // RGB 通道，如 inputShape[3]=3 → [0.5, 0.5, 0.5]
                    }
                }
            }
            2 -> Array(inputShape[0]) { FloatArray(inputShape[1]) { 0.5f } }                           // 二维：如 [1, 1000]
            1 -> FloatArray(inputShape[0]) { 0.5f }                                                    // 一维：如 [1000]
            else -> FloatArray(inputShape.last()) { 0.5f }                                             // 兜底
        }
    }
}
