package com.nextthing.app.presentation.screens.userinfo

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nextthing.app.R
import com.nextthing.app.presentation.theme.BgCard
import com.nextthing.app.presentation.theme.BgPrimary
import com.nextthing.app.presentation.theme.Border
import com.nextthing.app.presentation.theme.Primary
import com.nextthing.app.presentation.theme.TextPrimary
import com.nextthing.app.presentation.theme.TextSecondary
import com.nextthing.app.presentation.theme.TextMuted

private val ProfileStatusBar = Color.White
private val ProfileBgStart = Color(0xFFF4EFFF)
private val ProfileBgMid = Color(0xFFF7F3FF)
private val ProfileBgEnd = Color(0xFFFBFAFF)
private val ProfilePurple = Color(0xFF7057F5)
private val ProfilePurpleSoft = Color(0xFFB06DFF)
private val ProfileInk = Color(0xFF202331)
private val ProfileDeep = Color(0xFF2F2850)
private val ProfileSub = Color(0xFF656B78)
private val ProfileMuted = Color(0xFFA6ACB8)
private val ProfileLine = Color(0xFFE8ECF1)
private val ProfileGreen = Color(0xFF20A875)
private val ProfileDanger = Color(0xFFDF5C66)

private fun Modifier.profilePageBackground(): Modifier = background(Color(0xFFF7F8FC))

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen(
    onBackPressed: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateToAchievement: () -> Unit = {},
    viewModel: UserInfoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val logoutEvent by viewModel.logoutEvent.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current

    DisposableEffect(view) {
        val window = context.findActivity()?.window
        val previousStatusBarColor = window?.statusBarColor
        val previousLightStatusBar = window?.let {
            WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars
        }
        window?.statusBarColor = ProfileStatusBar.toArgb()
        window?.let {
            WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = true
        }
        onDispose {
            window?.let {
                previousStatusBarColor?.let { color -> it.statusBarColor = color }
                previousLightStatusBar?.let { light ->
                    WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = light
                }
            }
        }
    }

    LaunchedEffect(logoutEvent) {
        if (logoutEvent) {
            onLogout()
        }
    }

    var showNicknameDialog by remember { mutableStateOf(false) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let(viewModel::updateAvatar)
    }

    Scaffold(
        modifier = Modifier.profilePageBackground(),
        containerColor = BgPrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "个人资料",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgCard)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
                item {
                    IdentityCard(
                        nickname = uiState.nickname.ifBlank { "未设置昵称" },
                        avatarUri = uiState.avatarUri,
                        usageDays = uiState.usageDays,
                        unlockedAchievements = uiState.unlockedAchievementsCount,
                        totalAchievements = uiState.totalAchievementsCount,
                        onAchievementClick = onNavigateToAchievement
                    )
                }

                item {
                    SectionHeader(title = "资料编辑", trailing = "头像与昵称")
                    ProfileListCard {
                        ProfileEditRow(
                            iconText = "IMG",
                            iconColor = ProfilePurple,
                            title = "头像",
                            subtitle = "点击更换头像",
                            showArrow = false,
                            onClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                        DividerLine()
                        ProfileEditRow(
                            iconText = "NAME",
                            iconColor = ProfilePurpleSoft,
                            title = "昵称",
                            subtitle = "点击编辑昵称",
                            showArrow = true,
                            onClick = { showNicknameDialog = true }
                        )
                    }
                }

                item {
                    SectionHeader(title = "账号状态", trailing = "登录")
                    LoginStatusCard()
                }

                item {
                    SectionHeader(title = "账号操作")
                    LogoutButton(onClick = viewModel::logout)
                }
        }
    }

    if (showNicknameDialog) {
        EditNicknameDialog(
            onDismiss = { showNicknameDialog = false },
            onConfirm = { newNickname ->
                viewModel.updateNickname(newNickname)
                showNicknameDialog = false
            }
        )
    }
}

@Composable
private fun ProfileTopBar(onBackPressed: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(start = 12.dp, end = 16.dp, top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = Color(0xFFFAFBFD).copy(alpha = 0.78f),
            border = BorderStroke(1.dp, Color(0xFFE9ECF2).copy(alpha = 0.88f)),
            shadowElevation = 2.dp
        ) {
            IconButton(onClick = onBackPressed, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = ProfileInk,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Text(
            text = "个人资料",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = ProfileInk,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun IdentityCard(
    nickname: String,
    avatarUri: Uri?,
    usageDays: Int,
    unlockedAchievements: Int,
    totalAchievements: Int,
    onAchievementClick: () -> Unit
) {
    SimpleIdentityCard(
        nickname = nickname,
        avatarUri = avatarUri,
        usageDays = usageDays,
        unlockedAchievements = unlockedAchievements,
        totalAchievements = totalAchievements,
        onAchievementClick = onAchievementClick
    )
    return

    val breathingTransition = rememberInfiniteTransition(label = "profileIdentityBreathing")
    val breath by breathingTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "profileIdentityGlow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val strokeWidth = 2.dp.toPx() + 3.dp.toPx() * breath
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.24f + 0.18f * breath),
                            Color(0xFFFF7BEF).copy(alpha = 0.24f + 0.30f * breath),
                            Color(0xFF7AE9FF).copy(alpha = 0.20f + 0.28f * breath),
                            Color.White.copy(alpha = 0.18f + 0.14f * breath)
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    ),
                    cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx()),
                    style = Stroke(width = strokeWidth)
                )
            }
            .padding(4.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(ProfilePurple.copy(alpha = 0.95f), ProfilePurpleSoft.copy(alpha = 0.88f))
                )
            )
            .drawBehind {
                val softAlpha = 0.20f + 0.22f * breath
                val brightAlpha = 0.18f + 0.26f * breath
                val radiusBoost = 30.dp.toPx() * breath
                val drift = 26.dp.toPx() * breath

                drawCircle(
                    color = Color.White.copy(alpha = 0.18f + 0.14f * breath),
                    radius = 84.dp.toPx() + radiusBoost,
                    center = Offset(size.width - 4.dp.toPx(), -8.dp.toPx())
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = softAlpha),
                            Color(0xFFFF9FE8).copy(alpha = brightAlpha),
                            Color.Transparent
                        ),
                        center = Offset(62.dp.toPx() + drift, 62.dp.toPx()),
                        radius = 130.dp.toPx() + radiusBoost
                    ),
                    radius = 130.dp.toPx() + radiusBoost,
                    center = Offset(62.dp.toPx() + drift, 62.dp.toPx())
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF71F0FF).copy(alpha = 0.16f + 0.26f * breath),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.70f, size.height * 0.18f + drift),
                        radius = 120.dp.toPx() + radiusBoost
                    ),
                    radius = 120.dp.toPx() + radiusBoost,
                    center = Offset(size.width * 0.70f, size.height * 0.18f + drift)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFF1B9).copy(alpha = 0.18f + 0.18f * breath),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.82f, size.height * 0.76f),
                        radius = 140.dp.toPx() + 20.dp.toPx() * breath
                    ),
                    radius = 140.dp.toPx() + 20.dp.toPx() * breath,
                    center = Offset(size.width * 0.82f, size.height * 0.76f)
                )
            }
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val presetPainter = painterResource(R.drawable.preset_avatar)
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.62f + 0.22f * breath),
                                        Color(0xFFFF8DEB).copy(alpha = 0.38f + 0.30f * breath),
                                        Color(0xFF85EFFF).copy(alpha = 0.20f + 0.24f * breath),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = 48.dp.toPx() + 18.dp.toPx() * breath
                                ),
                                radius = 48.dp.toPx() + 18.dp.toPx() * breath,
                                center = center
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.28f + 0.28f * breath),
                                radius = 39.dp.toPx() + 8.dp.toPx() * breath,
                                center = center,
                                style = Stroke(width = 2.dp.toPx() + 2.dp.toPx() * breath)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = avatarUri,
                        contentDescription = "头像",
                        placeholder = presetPainter,
                        error = presetPainter,
                        fallback = presetPainter,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color.White.copy(alpha = 0.86f + 0.10f * breath), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = nickname,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        HonorBadge()
                    }
                    Text(
                        text = "你在 NextThing 中的身份资料",
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 7.dp)
                    )
                    MedalPreview()
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    label = "使用天数",
                    value = "${usageDays.coerceAtLeast(1)} 天",
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "成就解锁",
                    value = "$unlockedAchievements / $totalAchievements",
                    modifier = Modifier.weight(1f)
                )
            }

            AchievementEntry(onClick = onAchievementClick)
        }
    }
}

@Composable
private fun SimpleIdentityCard(
    nickname: String,
    avatarUri: Uri?,
    usageDays: Int,
    unlockedAchievements: Int,
    totalAchievements: Int,
    onAchievementClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Border, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = BgCard
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val presetPainter = painterResource(R.drawable.preset_avatar)
                AsyncImage(
                    model = avatarUri,
                    contentDescription = "头像",
                    placeholder = presetPainter,
                    error = presetPainter,
                    fallback = presetPainter,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .border(1.dp, Border, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(nickname, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("个人资料", color = TextSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileMetric("${usageDays.coerceAtLeast(0)}", "使用天数", Modifier.weight(1f))
                ProfileMetric("$unlockedAchievements / $totalAchievements", "成就解锁", Modifier.weight(1f))
            }
            DividerLine()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAchievementClick)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("我的成就", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("查看已解锁成就与徽章详情", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextMuted)
            }
        }
    }
}

@Composable
private fun ProfileMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Primary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun HonorBadge() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color(0xFFFFD97C).copy(alpha = 0.70f))
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.linearGradient(listOf(Color(0xFF3A276B), Color(0xFF6B4EE8), Color(0xFF2B2052))),
                    RoundedCornerShape(999.dp)
                )
                .padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFFFF1B9), Color(0xFFD79D2A))))
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "见习掌控师",
                color = Color(0xFFF9E7A9),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MedalPreview() {
    Row(
        modifier = Modifier.padding(top = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(ProfilePurple, Color(0xFFF1A832), Color(0xFF1AAEC0)).forEachIndexed { index, color ->
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .offset(x = (-5 * index).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.28f), color)
                        )
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.72f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "★",
                    color = Color(0xFFFFF4BC),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.68f),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun AchievementEntry(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(17.dp))
            .clickable(onClick = onClick)
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(17.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFFFF3B3), Color(0xFFF0AC35)))),
            contentAlignment = Alignment.Center
        ) {
            Text("★", color = Color(0xFF5A3B00), fontSize = 13.sp, fontWeight = FontWeight.Black)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Text("成就中心", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Text(
                "查看已解锁成就与徽章详情",
                color = Color.White.copy(alpha = 0.70f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 3.dp)
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.74f)
        )
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, end = 2.dp, top = 14.dp, bottom = 7.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = ProfileInk,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black
        )
        if (trailing != null) {
            Text(
                text = trailing,
                color = ProfileMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProfileListCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BgCard)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
    ) {
        content()
    }
}

@Composable
private fun ProfileEditRow(
    iconText: String,
    iconColor: Color,
    title: String,
    subtitle: String,
    showArrow: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (iconText == "IMG") Icons.Filled.Image else Icons.Filled.Edit,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Text(title, color = ProfileInk, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                subtitle,
                color = ProfileSub,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }

        if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = ProfileMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(start = 61.dp)
            .background(ProfileLine)
    )
}

@Composable
private fun LoginStatusCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BgCard)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ProfileGreen, modifier = Modifier.size(28.dp))
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text("登录状态正常", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("当前账号已登录，可正常同步和使用 AI 能力", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
        }
        Text("已登录", color = ProfileGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
    return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.82f))
            .border(1.dp, ProfileGreen.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ProfileGreen),
            contentAlignment = Alignment.Center
        ) {
            Text("OK", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            Text("登录状态正常", color = ProfileInk, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "当前账号已登录，可正常同步和使用 AI 能力",
                color = ProfileSub,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }

        Text("已登录", color = ProfileGreen, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun LogoutButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ProfileDanger.copy(alpha = 0.08f),
            contentColor = ProfileDanger
        ),
        border = BorderStroke(1.dp, ProfileDanger.copy(alpha = 0.16f)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Text("退出登录", fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun EditNicknameDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var inputValue by remember { mutableStateOf("") }
    val trimmed = inputValue.trim()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "编辑昵称",
                    color = ProfileInk,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "输入你的新昵称",
                    color = ProfileSub,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 14.dp)
                )

                OutlinedTextField(
                    value = inputValue,
                    onValueChange = {
                        if (it.length <= 20) {
                            inputValue = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("输入你的新昵称", color = Color(0xFFA1A8B8), fontSize = 14.sp)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ProfilePurple,
                        unfocusedBorderColor = Color(0xFFE6E9F2),
                        focusedContainerColor = Color(0xFFF7F8FC),
                        unfocusedContainerColor = Color(0xFFF7F8FC),
                        cursorColor = ProfilePurple,
                        focusedTextColor = ProfileInk,
                        unfocusedTextColor = ProfileInk
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFF0F2F8))
                    ) {
                        Text("取消", color = Color(0xFF5F6678), fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    Button(
                        onClick = { onConfirm(trimmed) },
                        enabled = trimmed.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ProfilePurple,
                            contentColor = Color.White,
                            disabledContainerColor = ProfilePurple.copy(alpha = 0.38f),
                            disabledContentColor = Color.White.copy(alpha = 0.72f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text("确认", fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
