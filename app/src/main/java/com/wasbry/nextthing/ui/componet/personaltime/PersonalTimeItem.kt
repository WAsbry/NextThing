package com.wasbry.nextthing.ui.componet.personaltime

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wasbry.nextthing.database.TodoDatabase
import com.wasbry.nextthing.database.model.PersonalTime
import com.wasbry.nextthing.database.repository.PersonalTimeRepository
import com.wasbry.nextthing.database.repository.TodoTaskRepository
import com.wasbry.nextthing.ui.componet.personaltime.edit.EditPersonalTimeDialog
import com.wasbry.nextthing.viewmodel.personalTime.PersonalTimeViewModel
import com.wasbry.nextthing.viewmodel.personalTime.PersonalTimeViewModelFactory
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberSwipeableState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay

import androidx.compose.ui.input.pointer.PointerEventType

@SuppressLint("MultipleAwaitPointerEventScopes")
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun PersonalTimeItem(time: PersonalTime) {
    var isEditDialogOpen by remember { mutableStateOf(false) }
    var isLongPressed by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current


    val database = TodoDatabase.getInstance(context)
    val repositoryPersonalTime = PersonalTimeRepository(database.personalTimeDao())
    val viewModelPersonalTime: PersonalTimeViewModel = viewModel(
        factory = PersonalTimeViewModelFactory(repositoryPersonalTime)
    )

    // 侧滑状态
    val swipeableState = rememberSwipeableState(initialValue = 0)
    val width = 120.dp
    val widthPx = with(LocalDensity.current) { width.toPx() }
    val anchors = mapOf(
        0f to 0,
        -widthPx to 1
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                isEditDialogOpen = true
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 根据 time.iconPath 获取对应的图标资源
            val iconResId = getDrawableResourceId(context, time.iconPath)
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.width(24.dp)
            )
            Text(text = time.timeDescription)
            DisplayValue("开始", time.startTime)
            DisplayValue("结束", time.endTime)
            DisplayValue("价值", time.timeValue.toString())
            DisplayValue("自主性", time.selfControlDegree.toString())
        }
    }

    if (isEditDialogOpen) {
        EditPersonalTimeDialog(
            personalTime = time,
            onDismiss = { isEditDialogOpen = false },
            onSave = { updatedTime ->
                // 在这里处理保存后的逻辑，比如更新数据源等
                Log.d("EditPersonalTimeDialog","更新后的值 = ${updatedTime}" )
                viewModelPersonalTime.updatePersonalTime(updatedTime)

            }
        )
    }
}

@Composable
fun DisplayValue(title: String, content: String) {
    Column (
        modifier = Modifier.width(50.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ){
        Text(text = title)
        Text(text = content)
    }
}

// 根据图标名称获取资源 ID（非 Compose 函数）
private fun getDrawableResourceId(context: Context, iconName: String): Int {
    // 这里假设传入的 iconName 是不带前缀和后缀的，例如 "icon_personal_time_cooking"
    // 实际使用时可能需要根据你的资源命名规则进行调整
    val packageName = context.packageName
    try {
        return context.resources.getIdentifier(
            iconName,
            "drawable",
            packageName
        )
    } catch (e: Resources.NotFoundException) {
        // 如果找不到资源，返回一个默认的资源 ID
        return android.R.drawable.ic_menu_report_image
    }
}