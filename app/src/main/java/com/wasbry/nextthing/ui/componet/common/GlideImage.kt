package com.wasbry.nextthing.ui.componet.common

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target

// 使用 Glide 加载图片的自定义组件
@Composable
fun GlideImage(
    resourceName: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val uri = remember(resourceName) {
        // 构建资源 URI: android.resource://包名/资源类型/资源名
        Uri.parse("android.resource://${context.packageName}/mipmap/$resourceName")
    }

    AndroidView(
        factory = { ctx ->
            android.widget.ImageView(ctx).apply {
                // 设置图片大小
                layoutParams = android.view.ViewGroup.LayoutParams(
                    Target.SIZE_ORIGINAL,
                    Target.SIZE_ORIGINAL
                )
            }
        },
        update = { imageView ->
            // 使用 Glide 加载图片
            Log.d("glide","uri = ${uri}")
            Glide.with(context)
                .load(uri)
                .into(imageView)
        },
        modifier = modifier.size(60.dp)
    )
}
