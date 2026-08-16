package com.nextthing.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ToastPurple = Color(0xFF7057F5)
private val ToastPurpleSoft = Color(0xFFB06DFF)
private val ToastInk = Color(0xFF202331)
private val ToastSub = Color(0xFF656B78)

enum class AppToastType {
    Info,
    Success,
    Warning,
    Error
}

data class AppToastData(
    val message: String,
    val type: AppToastType = AppToastType.Info
)

@Stable
class AppToastHostState internal constructor(
    private val scope: CoroutineScope
) {
    var currentToast by mutableStateOf<AppToastData?>(null)
        private set

    private var pendingJob: Job? = null
    private var hideJob: Job? = null

    fun showDebounced(
        message: String,
        type: AppToastType = AppToastType.Info,
        debounceMillis: Long = 420L,
        durationMillis: Long = 1800L
    ) {
        pendingJob?.cancel()
        pendingJob = scope.launch {
            delay(debounceMillis)
            currentToast = AppToastData(message = message, type = type)
            hideJob?.cancel()
            hideJob = launch {
                delay(durationMillis)
                currentToast = null
            }
        }
    }

    fun dismiss() {
        pendingJob?.cancel()
        hideJob?.cancel()
        currentToast = null
    }
}

@Composable
fun rememberAppToastHostState(): AppToastHostState {
    val scope = rememberCoroutineScope()
    return remember(scope) { AppToastHostState(scope) }
}

@Composable
fun AppToastHost(
    hostState: AppToastHostState,
    modifier: Modifier = Modifier
) {
    val toast = hostState.currentToast

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 22.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn(animationSpec = tween(160, easing = FastOutSlowInEasing)) +
                    slideInVertically(
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        initialOffsetY = { it / 2 }
                    ),
            exit = fadeOut(animationSpec = tween(120)) +
                    slideOutVertically(
                        animationSpec = tween(160),
                        targetOffsetY = { it / 3 }
                    )
        ) {
            toast?.let {
                AppToastContent(toast = it)
            }
        }
    }
}

@Composable
private fun AppToastContent(toast: AppToastData) {
    val accent = when (toast.type) {
        AppToastType.Info -> ToastPurple
        AppToastType.Success -> Color(0xFF20A875)
        AppToastType.Warning -> Color(0xFFDF9639)
        AppToastType.Error -> Color(0xFFDF5C66)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.16f)),
        shadowElevation = 18.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(accent, if (toast.type == AppToastType.Info) ToastPurpleSoft else accent.copy(alpha = 0.76f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (toast.type) {
                        AppToastType.Info -> "i"
                        AppToastType.Success -> "✓"
                        AppToastType.Warning -> "!"
                        AppToastType.Error -> "!"
                    },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Text(
                text = toast.message,
                modifier = Modifier.weight(1f),
                color = if (toast.type == AppToastType.Info) ToastInk else ToastSub,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp
            )
        }
    }
}
