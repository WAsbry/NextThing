package com.nextthing.app.domain.service

import com.nextthing.app.domain.model.SERResult

/**
 * SER（语音情绪识别）服务接口
 *
 * @DESC: 串联音频预处理和模型推理，提供端到端情绪识别能力
 * 1. 接收原始 PCM 音频数据
 * 2. 通过 AudioPreprocessor 提取 MFCC 特征
 * 3. 通过 OnDeviceAIEngine 执行模型推理
 * 4. 返回情绪识别结果
 *
 * 补充：调用方只需传入 PCM 数据，不需要关心预处理和推理的细节
 */
interface SERService {

    /**
     * @DESC: 加载 SER 模型
     * 1. 构造 ModelConfig（指定 ser_model.tflite）
     * 2. 调用 OnDeviceAIEngine.loadModel()
     *
     * 补充：应在应用启动时调用
     */
    suspend fun loadModel()

    /**
     * @DESC: 对一段 PCM 音频进行情绪识别
     * 1. AudioPreprocessor 提取 MFCC 特征
     * 2. OnDeviceAIEngine 执行推理
     * 3. Softmax 将 logits 转为概率
     * 4. 找到最大概率对应的情绪
     *
     * @Param: samples — PCM 采样点数组（16bit 有符号整数）
     * @Param: sampleRate — 采样率（如 16000）
     * @return SERResult（情绪 + 置信度 + 概率分布 + 耗时）
     */
    suspend fun recognize(samples: ShortArray, sampleRate: Int = 16000): SERResult

    /**
     * @DESC: 释放模型资源
     */
    fun release()
}
