package com.nextthing.app.presentation.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nextthing.app.presentation.theme.BgPrimary
import com.nextthing.app.presentation.theme.BgSecondary
import com.nextthing.app.presentation.theme.Border
import com.nextthing.app.presentation.theme.Danger
import com.nextthing.app.presentation.theme.Primary
import com.nextthing.app.presentation.theme.TextMuted
import com.nextthing.app.presentation.theme.TextPrimary
import com.nextthing.app.presentation.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // NT Logo — 图标 + 文字
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // NT 图标
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(14.dp),
                            ambientColor = Primary.copy(alpha = 0.25f),
                            spotColor = Primary.copy(alpha = 0.35f)
                        )
                        .background(
                            color = Primary,
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NT",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-1).sp
                    )
                }

                // NextThing 文字
                Text(
                    text = "NextThing",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = (-0.8).sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 标题
            Text(
                text = if (uiState.isRegisterMode) "创建账号" else "欢迎回来",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = (-0.3).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 副标题
            Text(
                text = if (uiState.isRegisterMode) "开始你的 AI 伙伴之旅" else "登录以继续使用",
                fontSize = 14.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(36.dp))

            // 用户名输入框
            OutlinedTextField(
                value = uiState.username,
                onValueChange = viewModel::onUsernameChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = if (uiState.isRegisterMode) "设置用户名" else "请输入用户名",
                        color = TextMuted
                    )
                },
                singleLine = true,
                leadingIcon = {
                    Text("👤", fontSize = 18.sp)
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Border,
                    focusedContainerColor = Color(0xFFEEF0FF),
                    unfocusedContainerColor = BgSecondary,
                    cursorColor = Primary
                ),
                shape = RoundedCornerShape(14.dp)
            )

            // 邮箱（注册模式）
            if (uiState.isRegisterMode) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("请输入邮箱", color = TextMuted) },
                    singleLine = true,
                    leadingIcon = {
                        Text("📧", fontSize = 18.sp)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Border,
                        focusedContainerColor = Color(0xFFEEF0FF),
                        unfocusedContainerColor = BgSecondary,
                        cursorColor = Primary
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 密码输入框
            val passwordHasError = uiState.errorMessage != null
            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = if (uiState.isRegisterMode) "设置密码（至少6位）" else "请输入密码",
                        color = TextMuted
                    )
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                leadingIcon = {
                    Text("🔒", fontSize = 18.sp)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { viewModel.submit() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (passwordHasError) Danger else Primary,
                    unfocusedBorderColor = if (passwordHasError) Danger else Border,
                    focusedContainerColor = if (passwordHasError) Danger.copy(alpha = 0.04f) else Color(0xFFEEF0FF),
                    unfocusedContainerColor = BgSecondary,
                    cursorColor = Primary
                ),
                shape = RoundedCornerShape(14.dp)
            )

            // 确认密码（注册模式）
            if (uiState.isRegisterMode) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = uiState.confirmPassword ?: "",
                    onValueChange = viewModel::onConfirmPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("确认密码", color = TextMuted) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    leadingIcon = {
                        Text("🔒", fontSize = 18.sp)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { viewModel.submit() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Border,
                        focusedContainerColor = Color(0xFFEEF0FF),
                        unfocusedContainerColor = BgSecondary,
                        cursorColor = Primary
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // 错误提示
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.errorMessage!!,
                    fontSize = 12.sp,
                    color = Danger
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 登录/注册按钮 — 胶囊形拟态
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(999.dp),
                        ambientColor = Primary.copy(alpha = 0.25f),
                        spotColor = Primary.copy(alpha = 0.35f)
                    )
                    .background(
                        color = if (uiState.isLoading) Primary.copy(alpha = 0.7f) else Primary,
                        shape = RoundedCornerShape(999.dp)
                    )
                    .clickable(enabled = !uiState.isLoading) { viewModel.submit() },
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = if (uiState.isRegisterMode) "注 册" else "登 录",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 切换登录/注册
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.isRegisterMode) "已有账号？" else "没有账号？",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = if (uiState.isRegisterMode) "登录" else "注册",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary,
                    modifier = Modifier.clickable { viewModel.toggleMode() }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
