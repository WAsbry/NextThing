package com.example.nextthingb1.presentation.screens.userinfo

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.nextthingb1.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(
    onBackPressed: () -> Unit = {},
    viewModel: UserInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.updateAvatar(it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        // 顶部导航栏
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "账号信息",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Text(
                        text = "‹",
                        fontSize = 32.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Light
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.White
            ),
            modifier = Modifier.height(48.dp)
        )

        // 内容区域
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 基础信息标题
            item {
                Text(
                    text = "基础信息",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 头像
            item {
                UserInfoItem(
                    label = "头像",
                    avatarUri = uiState.avatarUri,
                    showAvatar = true,
                    showArrow = true,
                    onClick = {
                        imagePickerLauncher.launch("image/*")
                    }
                )
            }

            // 昵称
            item {
                UserInfoItem(
                    label = "昵称",
                    value = uiState.nickname.ifEmpty { "未设置" },
                    showArrow = true,
                    onClick = { /* TODO: 修改昵称对话框 */ }
                )
            }

            // ID
            item {
                UserInfoItem(
                    label = "ID",
                    value = uiState.userId.ifEmpty { "加载中..." },
                    showCopy = true,
                    onClick = { /* TODO: 复制ID */ }
                )
            }

            // 账号绑定标题
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "账号绑定",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 手机号
            item {
                UserInfoItem(
                    label = "手机号",
                    value = uiState.phoneNumber.ifEmpty { "未绑定" },
                    actionText = if (uiState.phoneNumber.isEmpty()) "" else "换绑",
                    showArrow = true,
                    onClick = {
                        viewModel.showBindDialog(BindType.PHONE)
                    }
                )
            }

            // 微信
            item {
                UserInfoItem(
                    label = "微信",
                    value = uiState.wechatId.ifEmpty { "未绑定" },
                    actionText = if (uiState.wechatId.isEmpty()) "" else "换绑",
                    showArrow = true,
                    onClick = {
                        viewModel.showBindDialog(BindType.WECHAT)
                    }
                )
            }

            // QQ
            item {
                UserInfoItem(
                    label = "QQ",
                    value = uiState.qqId.ifEmpty { "未绑定" },
                    actionText = if (uiState.qqId.isEmpty()) "" else "换绑",
                    showArrow = true,
                    onClick = {
                        viewModel.showBindDialog(BindType.QQ)
                    }
                )
            }

            // 退出登录按钮
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { viewModel.logout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "退出登录",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 注销账号文字
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "注销账号",
                    fontSize = 14.sp,
                    color = TextMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.deleteAccount() }
                        .padding(vertical = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 绑定输入对话框
        if (uiState.showBindDialog) {
            BindAccountDialog(
                type = uiState.bindDialogType ?: BindType.PHONE,
                onDismiss = { viewModel.hideBindDialog() },
                onConfirm = { value ->
                    viewModel.bindAccount(value)
                }
            )
        }
    }
}

@Composable
private fun BindAccountDialog(
    type: BindType,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var inputValue by remember { mutableStateOf("") }

    val title = when (type) {
        BindType.PHONE -> "绑定手机号"
        BindType.WECHAT -> "绑定微信"
        BindType.QQ -> "绑定QQ"
    }

    val placeholder = when (type) {
        BindType.PHONE -> "请输入手机号"
        BindType.WECHAT -> "请输入微信号"
        BindType.QQ -> "请输入QQ号"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            OutlinedTextField(
                value = inputValue,
                onValueChange = { inputValue = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(text = placeholder, color = TextMuted)
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Border,
                    cursorColor = Primary
                ),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (inputValue.isNotBlank()) {
                        onConfirm(inputValue)
                    }
                }
            ) {
                Text(
                    text = "确定",
                    color = Primary,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "取消",
                    color = TextSecondary
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun UserInfoItem(
    label: String,
    value: String = "",
    avatarUri: Uri? = null,
    showAvatar: Boolean = false,
    showArrow: Boolean = false,
    showCopy: Boolean = false,
    actionText: String = "",
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = showArrow || showCopy) { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 标签
            Text(
                text = label,
                fontSize = 16.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.weight(1f))

            // 右侧内容
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 头像
                if (showAvatar) {
                    if (avatarUri != null) {
                        AsyncImage(
                            model = avatarUri,
                            contentDescription = "头像",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "👤",
                                fontSize = 20.sp
                            )
                        }
                    }
                }

                // 值
                if (value.isNotEmpty() && !showAvatar) {
                    Text(
                        text = value,
                        fontSize = 15.sp,
                        color = TextSecondary
                    )
                }

                // 操作文字（换绑）
                if (actionText.isNotEmpty()) {
                    Text(
                        text = actionText,
                        fontSize = 14.sp,
                        color = Primary
                    )
                }

                // 复制图标
                if (showCopy) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_save),
                        contentDescription = "复制",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 箭头
                if (showArrow) {
                    Text(
                        text = "›",
                        fontSize = 24.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Light
                    )
                }
            }
        }
    }
}
