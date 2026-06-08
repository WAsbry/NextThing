package com.nextthing.app.domain.service

import com.nextthing.app.domain.model.Accelerator
import com.nextthing.app.domain.model.BenchmarkResult
import com.nextthing.app.domain.model.InferenceResult
import com.nextthing.app.domain.model.ModelConfig

/**
 * 端侧 AI 推理引擎接口
 *
 * 职责：加载模型、执行推理、性能测试、释放资源
 * 实现：OnDeviceAIEngineImpl（LiteRT + NNAPI/GPU Delegate）
 */
interface OnDeviceAIEngine {

    /**
     * 加载模型
     * @param config 模型配置
     */
    suspend fun loadModel(config: ModelConfig)

    /**
     * 执行一次推理
     * @param input 输入数据（如图片像素数组）
     * @return 推理结果（输出数据 + 耗时 + 使用的加速器）
     */
    suspend fun infer(input: Any): InferenceResult

    /**
     * 性能测试：用三种加速器各跑若干次，取中位数对比
     * @param modelFileName 模型文件名
     * @param iterations 每种加速器跑多少次，默认 50
     * @return 三种加速器的 Benchmark 结果
     */
    suspend fun benchmark(
        modelFileName: String,
        iterations: Int = 50
    ): BenchmarkResult

    /**
     * 检测当前设备最佳的加速器
     * @return 可用的最高优先级加速器
     */
    fun detectBestAccelerator(): Accelerator

    /**
     * 释放模型占用的资源
     */
    fun release()
}
