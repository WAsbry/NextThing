package com.wasbry.nextthing.ui.componet.personaltime

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.wasbry.nextthing.R

/**
 * 个人时间管理中的图片选择控件
 * */
@SuppressLint("RememberReturnType")
@Composable
fun IconSelector(
    iconPath: String,
    onIconPathChange: (String) -> Unit, // 替换默认图片的函数
    modifier: Modifier = Modifier
) {
    var showIconDialog by remember { mutableStateOf(false) } // 图片选择弹窗的展示状态
    val context = LocalContext.current // 上下文
    val resource = context.resources // 拿到资源相关的
    val iconResource = remember { getIconResources(resource,context) }

    Row (modifier = modifier,
        verticalAlignment = Alignment.CenterVertically)
    {
        // 左侧的图片
        val drawable = getDrawableFromPath(context,iconPath)
        Image(painter = painterResource(id = drawable), contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp)) // 空的

        // 右侧图片选择按钮噻
        Button(
            onClick = { showIconDialog = true },
            modifier = Modifier.height(36.dp)
        ) {
            Text(text = "编辑")
        }
    }

    // 图片选择弹窗
    if (showIconDialog) {
        AlertDialog(
            onDismissRequest = { showIconDialog = false },
            title = { Text(text = "选择图标")},
            text = {
                LazyColumn {
                    items(iconResource) { iconRes  ->
                        val iconName = resource.getResourceEntryName(iconRes )
                        Row (
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onIconPathChange(iconName)
                                    showIconDialog = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(painter = painterResource(id = iconRes), contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(16.dp))
                            Text(iconName)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showIconDialog = false }) {
                    Text(text = "确定")
                }
            },
            dismissButton = {
                Button(onClick = { showIconDialog = false }) {
                    Text(text = "取消")

                }
            }
        )
    }
}

/**
 * 获取指定路径下的所有图片资源的id,需要进行拼接才拿得到啊
 * */
/**
 * 获取指定路径下的所有图片资源的id,需要进行拼接才拿得到啊
 * */
private fun getIconResources(resource: Resources, context: Context): List<Int> {
    val packageName = context.packageName
    val drawableTypeName = "drawable"
    val fieldNames = R.drawable::class.java.declaredFields.map { it.name }
    val targetPrefix = "icon_personal_time_"

    val filteredFieldNames = fieldNames.filter { it.startsWith(targetPrefix) }
    // 添加日志输出筛选后的资源名称
    filteredFieldNames.forEach { Log.d("IconSelector", "Filtered resource name: $it") }

    return filteredFieldNames.mapNotNull { fieldName ->
        resource.getIdentifier(fieldName, drawableTypeName, packageName)
            .takeIf { it != 0 }
    }
}

// 根据路径获取Drawable 资源
fun getDrawableFromPath(context: Context, iconPath: String): Int {
    val packageName = context.packageName
    val identifier = context.resources.getIdentifier(iconPath, "drawable", packageName)
    // 添加检查，确保返回的资源ID有效
    if (identifier != 0) {
        return identifier
    } else {
        // 如果资源ID无效，尝试获取默认资源（这里使用icon_add_task为例）
        val defaultIdentifier = context.resources.getIdentifier("icon_add_task", "drawable", packageName)
        if (defaultIdentifier != 0) {
            return defaultIdentifier
        } else {
            // 如果默认资源也无效，返回一个绝对存在的资源ID，比如应用的图标
            return android.R.drawable.ic_dialog_info
        }
    }
}