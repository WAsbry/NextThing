package com.nextthing.app.data.asr

import com.nextthing.app.data.preferences.ASRPreferences
import com.nextthing.app.data.preferences.ASRProvider
import com.nextthing.app.domain.service.ASRService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DelegatingASRService @Inject constructor(
    private val iflyASRService: IFlyASRService,
    private val zhiPuASRService: ZhiPuASRService,
    private val asrPreferences: ASRPreferences
) : ASRService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var activeDelegate: ASRService? = null

    private suspend fun resolveDelegate(): ASRService {
        val provider = asrPreferences.getProviderOnce()
        Timber.tag("ASR-Delegate").d("当前 provider: ${provider.name}")
        return when (provider) {
            ASRProvider.ZHIPU -> zhiPuASRService
            ASRProvider.IFLY -> iflyASRService
        }
    }

    override suspend fun isConfigured(): Boolean {
        val delegate = resolveDelegate()
        val configured = delegate.isConfigured()
        Timber.tag("ASR-Delegate").d("isConfigured: $configured (${delegate::class.simpleName})")
        return configured
    }

    override fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Timber.tag("ASR-Delegate").d("start() 被调用")
        scope.launch {
            val delegate = resolveDelegate()
            activeDelegate = delegate
            Timber.tag("ASR-Delegate").d("转发到 ${delegate::class.simpleName}")
            delegate.start(
                onPartial = { text ->
                    Timber.tag("ASR-Delegate").d("onPartial: $text")
                    onPartial(text)
                },
                onFinal = { text ->
                    Timber.tag("ASR-Delegate").d("onFinal: $text")
                    onFinal(text)
                },
                onError = { error ->
                    Timber.tag("ASR-Delegate").e("onError: $error")
                    onError(error)
                }
            )
        }
    }

    override fun stop() {
        Timber.tag("ASR-Delegate").d("stop() → ${activeDelegate?.let { it::class.simpleName } ?: "null"}")
        activeDelegate?.stop()
    }
}
