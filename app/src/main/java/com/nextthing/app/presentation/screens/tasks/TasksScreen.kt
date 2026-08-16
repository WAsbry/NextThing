package com.nextthing.app.presentation.screens.tasks

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nextthing.app.domain.model.Category
import com.nextthing.app.domain.model.Task
import com.nextthing.app.domain.model.TaskStatus
import com.nextthing.app.presentation.components.CategoryIconView
import com.nextthing.app.presentation.components.TaskItemCard
import com.nextthing.app.presentation.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ══════════════════════════════════════════
//  主入口
// ══════════════════════════════════════════

@Composable
fun TasksScreen(
    viewModel: TasksViewModel = hiltViewModel(),
    onNavigateToTaskDetail: (String) -> Unit = {},
    initialView: TaskView? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(initialView) {
        initialView?.let(viewModel::selectView)
    }

    // 分类和优先级暂未在一级页提供入口，进入页面时不能让历史条件继续隐性生效。
    LaunchedEffect(Unit) {
        viewModel.clearHiddenFilters()
    }

    // 下拉菜单展开状态
    var openDropdown by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        // ── 顶部标题/搜索栏 ──
        TasksTopBar(
            isSearchActive = uiState.isSearchActive,
            searchQuery = uiState.searchQuery,
            aiSearchState = uiState.aiSearchState,
            onSearchToggle = {
                viewModel.setSearchActive(!uiState.isSearchActive)
            },
            onSearchQueryChange = { viewModel.setSearchQuery(it) },
            onAISearch = {
                keyboardController?.hide()
                viewModel.searchWithAI()
            }
        )

        // ── 筛选栏 ──
        if (uiState.aiSearchState != AISearchState.LOADING) {
            TasksFilterBar(
                selectedView = uiState.selectedView,
                statusFilter = uiState.statusFilter,
                rangeLabel = if (uiState.selectedView == TaskView.LIST) {
                    viewModel.getWeekLabel(uiState.currentWeekOffset)
                } else {
                    viewModel.getMonthLabel()
                },
                onViewSelected = { viewModel.selectView(it) },
                onRangePrev = {
                    if (uiState.selectedView == TaskView.LIST) {
                        viewModel.changeWeek(uiState.currentWeekOffset - 1)
                    } else {
                        viewModel.previousMonth()
                    }
                },
                onRangeNext = {
                    if (uiState.selectedView == TaskView.LIST) {
                        viewModel.changeWeek(uiState.currentWeekOffset + 1)
                    } else {
                        viewModel.nextMonth()
                    }
                },
                onRangeReset = {
                    if (uiState.selectedView == TaskView.LIST) {
                        viewModel.resetWeek()
                    } else {
                        viewModel.resetMonth()
                    }
                },
                openDropdown = openDropdown,
                onOpenDropdown = { openDropdown = it },
                onStatusFilterSelected = {
                    viewModel.setStatusFilter(it)
                    openDropdown = null
                },
                onDismissDropdown = { openDropdown = null }
            )
        }

        if (
            uiState.isSearchActive &&
            uiState.searchQuery.isNotBlank() &&
            uiState.aiSearchState != AISearchState.IDLE &&
            uiState.aiSearchState != AISearchState.LOADING
        ) {
            AISearchFeedbackBar(
                state = uiState.aiSearchState,
                query = uiState.searchQuery,
                resultCount = uiState.searchResults.size,
                onExit = { viewModel.clearAISearch() },
                onRetry = { viewModel.searchWithAI() }
            )
        }

        // ── 内容区域 ──
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (uiState.aiSearchState == AISearchState.LOADING) {
            AISearchLoadingContent(query = uiState.searchQuery)
        } else {
            AnimatedContent(
                targetState = when {
                    uiState.isSearchActive && uiState.searchQuery.isNotBlank() -> "search"
                    uiState.selectedView == TaskView.LIST -> "list"
                    else -> "calendar"
                },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "content"
            ) { target ->
                when (target) {
                    "search" -> TasksSearchResults(
                        results = uiState.searchResults,
                        query = uiState.searchQuery,
                        isAIResult = uiState.aiSearchState == AISearchState.ACTIVE ||
                            uiState.aiSearchState == AISearchState.EMPTY,
                        onTaskClick = { onNavigateToTaskDetail(it.id) },
                        onToggleStatus = { viewModel.toggleTaskStatus(it) },
                        onDefer = { viewModel.deferTask(it) },
                        onCancel = { viewModel.cancelTask(it) }
                    )
                    "list" -> TasksListContent(
                        overdueGroup = uiState.overdueGroup,
                        isOverdueSectionExpanded = uiState.isOverdueSectionExpanded,
                        taskGroups = uiState.taskGroups,
                        onToggleOverdueSection = { viewModel.toggleOverdueSection() },
                        onTaskClick = { onNavigateToTaskDetail(it.id) },
                        onToggleStatus = { viewModel.toggleTaskStatus(it) },
                        onDefer = { viewModel.deferTask(it) },
                        onCancel = { viewModel.cancelTask(it) }
                    )
                    "calendar" -> TasksCalendarContent(
                        calendarDays = uiState.calendarDays,
                        selectedDate = uiState.selectedDate,
                        selectedDateTasks = uiState.selectedDateTasks,
                        selectedDateCompletedCount = uiState.selectedDateCompletedCount,
                        selectedDatePendingCount = uiState.selectedDatePendingCount,
                        selectedDateOverdueCount = uiState.selectedDateOverdueCount,
                        selectedDateCancelledCount = uiState.selectedDateCancelledCount,
                        onDateSelected = { viewModel.selectDate(it) },
                        onNavigateToTaskDetail = onNavigateToTaskDetail
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════
//  顶部标题 / 搜索栏
// ══════════════════════════════════════════

@Composable
private fun TasksTopBar(
    isSearchActive: Boolean,
    searchQuery: String,
    aiSearchState: AISearchState = AISearchState.IDLE,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAISearch: () -> Unit = {}
) {
    val focusRequester = remember { FocusRequester() }
    var searchFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = searchQuery,
                selection = TextRange(searchQuery.length)
            )
        )
    }

    // 外部恢复关键词时同步内容；用户仅移动光标时不覆盖其选区。
    LaunchedEffect(searchQuery) {
        if (searchFieldValue.text != searchQuery) {
            searchFieldValue = TextFieldValue(
                text = searchQuery,
                selection = TextRange(searchQuery.length)
            )
        }
    }

    // 每次进入搜索态都从关键词末尾继续输入。
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            searchFieldValue = searchFieldValue.copy(
                selection = TextRange(searchFieldValue.text.length)
            )
            focusRequester.requestFocus()
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BgCard,
        shadowElevation = 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isSearchActive) {
                Text(
                    text = "任务",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))

                // 搜索按钮
                IconButton(onClick = onSearchToggle) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = TextSecondary
                    )
                }

            } else {
                // 搜索模式
                IconButton(onClick = onSearchToggle) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = TextPrimary
                    )
                }

                TextField(
                    value = searchFieldValue,
                    onValueChange = { value ->
                        searchFieldValue = value
                        onSearchQueryChange(value.text)
                    },
                    placeholder = {
                        Text("搜索任务...", color = TextMuted, fontSize = 15.sp)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
                )

                if (searchFieldValue.text.isNotBlank()) {
                    IconButton(
                        onClick = {
                            searchFieldValue = TextFieldValue("")
                            onSearchQueryChange("")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "清除",
                            tint = TextSecondary
                        )
                    }
                }

                // AI 搜索属于任务查找能力，保留为文字操作，避免使用 Emoji。
                if (searchFieldValue.text.isNotBlank()) {
                    val isAIActive = aiSearchState == AISearchState.ACTIVE ||
                        aiSearchState == AISearchState.EMPTY
                    val isAILoading = aiSearchState == AISearchState.LOADING
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isAIActive -> Primary
                            isAILoading -> BgSecondary
                            else -> Primary.copy(alpha = 0.08f)
                        },
                        border = BorderStroke(
                            1.dp,
                            when {
                                isAIActive -> Primary
                                isAILoading -> Border
                                else -> Primary.copy(alpha = 0.28f)
                            }
                        ),
                        modifier = Modifier.clickable(
                            enabled = !isAILoading,
                            onClick = onAISearch
                        )
                    ) {
                        Text(
                            text = "智能",
                            color = when {
                                isAIActive -> Color.White
                                isAILoading -> TextMuted
                                else -> Primary
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                        )
                        }
                }

            }
            }
            HorizontalDivider(thickness = 0.5.dp, color = Border)
        }
    }
}

@Composable
private fun AISearchLoadingContent(query: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp,
                color = Primary
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "正在智能筛选",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "正在理解“$query”",
                color = TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
private fun AISearchFeedbackBar(
    state: AISearchState,
    query: String,
    resultCount: Int,
    onExit: () -> Unit,
    onRetry: () -> Unit
) {
    val isError = state == AISearchState.ERROR
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isError) Danger.copy(alpha = 0.06f) else Primary.copy(alpha = 0.06f),
        border = BorderStroke(
            1.dp,
            if (isError) Danger.copy(alpha = 0.24f) else Primary.copy(alpha = 0.22f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state == AISearchState.LOADING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = Primary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = when (state) {
                    AISearchState.LOADING -> "正在理解“$query”…"
                    AISearchState.ACTIVE -> "智能筛选 · 找到 $resultCount 项"
                    AISearchState.EMPTY -> "智能筛选未找到匹配任务"
                    AISearchState.ERROR -> "智能筛选失败，已保留普通结果"
                    AISearchState.IDLE -> ""
                },
                color = if (isError) Danger else TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            when (state) {
                AISearchState.ACTIVE,
                AISearchState.EMPTY -> TextButton(onClick = onExit) {
                    Text("退出", color = Primary, fontSize = 12.sp)
                }
                AISearchState.ERROR -> TextButton(onClick = onRetry) {
                    Text("重试", color = Danger, fontSize = 12.sp)
                }
                else -> Unit
            }
        }
    }
}

// ══════════════════════════════════════════
//  筛选栏
// ══════════════════════════════════════════

@Composable
private fun TasksFilterBar(
    selectedView: TaskView,
    statusFilter: StatusFilter,
    rangeLabel: String,
    onViewSelected: (TaskView) -> Unit,
    onRangePrev: () -> Unit,
    onRangeNext: () -> Unit,
    onRangeReset: () -> Unit,
    openDropdown: String?,
    onOpenDropdown: (String) -> Unit,
    onStatusFilterSelected: (StatusFilter) -> Unit,
    onDismissDropdown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 周/月切换
        ViewSwitchPills(
            selectedView = selectedView,
            onViewSelected = onViewSelected
        )

        Spacer(modifier = Modifier.width(6.dp))

        // 周/月视图共享同一时间导航位置，避免切换视图时布局跳动。
        TimeRangeNavRow(
            rangeLabel = rangeLabel,
            onPrev = onRangePrev,
            onNext = onRangeNext,
            onReset = onRangeReset
        )
        Spacer(modifier = Modifier.width(6.dp))

        Spacer(modifier = Modifier.weight(1f))

        Box {
            FilterChipButton(
                label = statusFilter.label,
                isActive = statusFilter != StatusFilter.ALL,
                onClick = { onOpenDropdown("status") }
            )
            StatusFilterDropdown(
                expanded = openDropdown == "status",
                current = statusFilter,
                onSelect = onStatusFilterSelected,
                onDismiss = onDismissDropdown
            )
        }
    }
}

@Composable
private fun ViewSwitchPills(
    selectedView: TaskView,
    onViewSelected: (TaskView) -> Unit
) {
    Row(
        modifier = Modifier
            .background(BgSecondary, RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF29293A).copy(alpha = 0.23f), RoundedCornerShape(8.dp))
            .padding(2.dp)
    ) {
        TaskView.entries.forEach { view ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selectedView == view) Primary else Color.Transparent)
                    .clickable { onViewSelected(view) }
                    .padding(horizontal = 13.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = view.title,
                    color = if (selectedView == view) Color.White else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (selectedView == view) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun TimeRangeNavRow(
    rangeLabel: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onReset: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowLeft,
            contentDescription = "上一周",
            tint = TextSecondary,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable { onPrev() }
        )
        Text(
            text = rangeLabel,
            fontSize = 12.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(min = 42.dp, max = 72.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onReset)
                .padding(horizontal = 2.dp, vertical = 4.dp)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "下一周",
            tint = TextSecondary,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .clickable { onNext() }
        )
    }
}

@Composable
private fun FilterChipButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) Primary.copy(alpha = 0.08f) else BgCard,
        border = BorderStroke(
            1.dp,
            if (isActive) Primary.copy(alpha = 0.32f)
            else Color(0xFF29293A).copy(alpha = 0.23f)
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = if (isActive) Primary else TextSecondary,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = if (isActive) Primary else TextMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

// ── 下拉菜单 ──

@Composable
private fun StatusFilterDropdown(
    expanded: Boolean,
    current: StatusFilter,
    onSelect: (StatusFilter) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(8.dp),
        containerColor = BgCard,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, Border),
        modifier = Modifier.width(168.dp)
    ) {
        StatusFilter.entries.forEach { filter ->
            DropdownMenuItem(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (filter == current) Primary.copy(alpha = 0.10f)
                        else Color.Transparent
                    ),
                text = {
                    Text(
                        text = filter.label,
                        color = if (filter == current) Primary else TextPrimary,
                        fontWeight = if (filter == current) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                },
                onClick = { onSelect(filter) }
            )
        }
    }
}

@Composable
private fun CategoryFilterDropdown(
    expanded: Boolean,
    categories: List<Category>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        // 全部
        DropdownMenuItem(
            text = {
                Text(
                    text = "全部分类",
                    color = if (selectedId == null) Primary else TextPrimary,
                    fontWeight = if (selectedId == null) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            },
            onClick = { onSelect(null) }
        )
        categories.forEach { category ->
            DropdownMenuItem(
                leadingIcon = {
                    CategoryIconView(icon = category.icon, size = 18.dp)
                },
                text = {
                    Text(
                        text = category.name,
                        color = if (category.id == selectedId) Primary else TextPrimary,
                        fontWeight = if (category.id == selectedId) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                },
                onClick = { onSelect(category.id) }
            )
        }
    }
}

@Composable
private fun PriorityFilterDropdown(
    expanded: Boolean,
    current: PriorityFilter,
    onSelect: (PriorityFilter) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        PriorityFilter.entries.forEach { filter ->
            val dotColor = when (filter) {
                PriorityFilter.ALL -> null
                PriorityFilter.IMPORTANT_URGENT -> Color(0xFFF44336)
                PriorityFilter.IMPORTANT_NOT_URGENT -> Color(0xFFFF9800)
                PriorityFilter.NOT_IMPORTANT_URGENT -> Color(0xFF2196F3)
                PriorityFilter.NOT_IMPORTANT_NOT_URGENT -> Color(0xFF4CAF50)
            }
            DropdownMenuItem(
                leadingIcon = dotColor?.let { color ->
                    {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(color, CircleShape)
                        )
                    }
                },
                text = {
                    Text(
                        text = filter.label,
                        color = if (filter == current) Primary else TextPrimary,
                        fontWeight = if (filter == current) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                },
                onClick = { onSelect(filter) }
            )
        }
    }
}

// ══════════════════════════════════════════
//  周视图 — 列表内容
// ══════════════════════════════════════════

@Composable
private fun TasksListContent(
    overdueGroup: List<Task>,
    isOverdueSectionExpanded: Boolean,
    taskGroups: List<TaskGroup>,
    onToggleOverdueSection: () -> Unit,
    onTaskClick: (Task) -> Unit,
    onToggleStatus: (String) -> Unit,
    onDefer: (String) -> Unit,
    onCancel: (String) -> Unit
) {
    if (overdueGroup.isEmpty() && taskGroups.isEmpty()) {
        TasksEmptyState()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // 逾期折叠组
        if (overdueGroup.isNotEmpty()) {
            item(key = "overdue_header") {
                OverdueSectionHeader(
                    count = overdueGroup.size,
                    expanded = isOverdueSectionExpanded,
                    onToggle = onToggleOverdueSection
                )
            }
            if (isOverdueSectionExpanded) {
                items(overdueGroup, key = { "overdue_${it.id}" }) { task ->
                    TaskListItem(
                        task = task,
                        onClick = { onTaskClick(task) },
                        onToggleStatus = { onToggleStatus(task.id) },
                        onDefer = { onDefer(task.id) },
                        onCancel = { onCancel(task.id) }
                    )
                }
            }
        }

        // 日期分组
        taskGroups.forEach { group ->
            item(key = "group_header_${group.date}") {
                TaskGroupHeader(group = group)
            }
            items(group.tasks, key = { "task_${it.id}" }) { task ->
                TaskListItem(
                    task = task,
                    onClick = { onTaskClick(task) },
                    onToggleStatus = { onToggleStatus(task.id) },
                    onDefer = { onDefer(task.id) },
                    onCancel = { onCancel(task.id) }
                )
            }
        }
    }
}

@Composable
private fun OverdueSectionHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Danger.copy(alpha = 0.08f))
            .clickable { onToggle() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (expanded) "▾" else "▸",
            fontSize = 14.sp,
            color = Danger,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "逾期 ($count)",
            fontSize = 14.sp,
            color = Danger,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TaskGroupHeader(group: TaskGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatDateDisplay(group.date),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Primary.copy(alpha = 0.1f)
        ) {
            Text(
                text = "${group.completedCount}/${group.totalCount}",
                color = Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun TaskListItem(
    task: Task,
    onClick: () -> Unit,
    onToggleStatus: () -> Unit,
    onDefer: () -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 10.dp)) {
        TaskItemCard(
            task = task,
            onClick = onClick,
            showSwipeActions = true,
            onToggleStatus = onToggleStatus,
            onPostpone = onDefer,
            onCancel = onCancel
        )
        HorizontalDivider(thickness = 0.5.dp, color = Border)
    }
}

// ══════════════════════════════════════════
//  搜索结果
// ══════════════════════════════════════════

@Composable
private fun TasksSearchResults(
    results: List<Task>,
    query: String,
    isAIResult: Boolean,
    onTaskClick: (Task) -> Unit,
    onToggleStatus: (String) -> Unit,
    onDefer: (String) -> Unit,
    onCancel: (String) -> Unit
) {
    if (results.isEmpty()) {
        SearchEmptyState(query = query, isAIResult = isAIResult)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Text(
                text = if (isAIResult) {
                    "智能筛选结果 · ${results.size} 项"
                } else {
                    "找到 ${results.size} 个结果"
                },
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
        items(results, key = { "search_${it.id}" }) { task ->
            TaskListItem(
                task = task,
                onClick = { onTaskClick(task) },
                onToggleStatus = { onToggleStatus(task.id) },
                onDefer = { onDefer(task.id) },
                onCancel = { onCancel(task.id) }
            )
        }
    }
}

// ══════════════════════════════════════════
//  月视图 — 日历
// ══════════════════════════════════════════

@Composable
private fun TasksCalendarContent(
    calendarDays: List<CalendarDay>,
    selectedDate: String?,
    selectedDateTasks: List<Task>,
    selectedDateCompletedCount: Int,
    selectedDatePendingCount: Int,
    selectedDateOverdueCount: Int,
    selectedDateCancelledCount: Int,
    onDateSelected: (String) -> Unit,
    onNavigateToTaskDetail: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // 星期标题与日期网格使用统一卡片边框，和项目内其他信息卡保持一致。
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                shape = RoundedCornerShape(8.dp),
                color = BgCard,
                border = BorderStroke(1.dp, Border)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                            Text(
                                text = day,
                                fontSize = 12.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    CalendarGrid(
                        calendarDays = calendarDays,
                        selectedDate = selectedDate,
                        onDateSelected = onDateSelected
                    )
                }
            }
        }

        // 选中日期详情
        selectedDate?.let { date ->
            item {
                SelectedDateDetailCard(
                    selectedDate = date,
                    tasks = selectedDateTasks,
                    completedCount = selectedDateCompletedCount,
                    pendingCount = selectedDatePendingCount,
                    overdueCount = selectedDateOverdueCount,
                    cancelledCount = selectedDateCancelledCount,
                    onTaskClick = { task -> onNavigateToTaskDetail(task.id) }
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    calendarDays: List<CalendarDay>,
    selectedDate: String?,
    onDateSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        calendarDays.chunked(7).forEach { weekDays ->
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                weekDays.forEach { day ->
                    Box(modifier = Modifier.weight(1f)) {
                        CalendarDayItem(
                            day = day,
                            isSelected = selectedDate == day.date,
                            onClick = { onDateSelected(day.date) }
                        )
                    }
                }
                repeat(7 - weekDays.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CalendarDayItem(
    day: CalendarDay,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val textColor = when {
        isSelected -> Color.White
        !day.isCurrentMonth -> TextMuted
        day.isToday -> Primary
        else -> TextPrimary
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> Primary
                    day.isToday -> Primary.copy(alpha = 0.1f)
                    day.isCurrentMonth -> BgCard
                    else -> Color.Transparent
                }
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.dayNumber,
                fontSize = 14.sp,
                fontWeight = if (day.isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )

            if (day.hasTask) {
                Spacer(modifier = Modifier.height(2.dp))
                // Task status dot indicators (max 3, priority: overdue > pending > completed)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    val dots = mutableListOf<Color>()
                    if (day.overdueCount > 0) dots.add(if (isSelected) Color.White.copy(alpha = 0.7f) else Danger)
                    if (day.pendingCount > 0) dots.add(if (isSelected) Color.White.copy(alpha = 0.7f) else Primary)
                    if (day.completedCount > 0) dots.add(if (isSelected) Color.White.copy(alpha = 0.7f) else Success)
                    dots.take(3).forEach { dotColor ->
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(dotColor, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDateDetailCard(
    selectedDate: String,
    tasks: List<Task>,
    completedCount: Int,
    pendingCount: Int,
    overdueCount: Int,
    cancelledCount: Int,
    onTaskClick: (Task) -> Unit
) {
    val dateInfo = remember(selectedDate) { formatSelectedDateDisplay(selectedDate) }
    val totalCount = pendingCount + completedCount + overdueCount + cancelledCount

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 12.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 顶部概览
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // 日期 + 总数
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateInfo.text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "共 $totalCount 项任务",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                // 状态统计行（只显示数量>0的）
                if (totalCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (pendingCount > 0) {
                            StatusDotLabel(color = Primary, label = "待办 $pendingCount")
                        }
                        if (completedCount > 0) {
                            StatusDotLabel(color = Success, label = "已完成 $completedCount")
                        }
                        if (overdueCount > 0) {
                            StatusDotLabel(color = Danger, label = "逾期 $overdueCount")
                        }
                        if (cancelledCount > 0) {
                            StatusDotLabel(color = Warning, label = "已取消 $cancelledCount")
                        }
                    }
                }
            }

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "无任务", color = TextMuted, fontSize = 14.sp)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    tasks.forEach { task ->
                        TaskItemCard(
                            task = task,
                            onClick = { onTaskClick(task) },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDotLabel(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
}

// ══════════════════════════════════════════
//  空状态
// ══════════════════════════════════════════

@Composable
private fun TasksEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "暂无任务",
                fontSize = 18.sp,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "调整筛选条件或切换周试试",
                fontSize = 13.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun SearchEmptyState(query: String, isAIResult: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isAIResult) "智能筛选没有找到匹配任务" else "没有找到\"$query\"",
                fontSize = 16.sp,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isAIResult) {
                    "尝试补充时间、状态或分类条件"
                } else {
                    "换一个关键词重新搜索"
                },
                fontSize = 13.sp,
                color = TextMuted
            )
        }
    }
}

// ══════════════════════════════════════════
//  工具函数
// ══════════════════════════════════════════

private fun formatDateDisplay(dateString: String): String {
    return try {
        val today = LocalDate.now()
        val taskDate = LocalDate.parse(dateString)
        when {
            taskDate == today -> "今天"
            taskDate == today.minusDays(1) -> "昨天"
            taskDate == today.plusDays(1) -> "明天"
            taskDate.year == today.year -> {
                val formatter = DateTimeFormatter.ofPattern("MM月dd日")
                taskDate.format(formatter)
            }
            else -> {
                val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")
                taskDate.format(formatter)
            }
        }
    } catch (_: Exception) {
        dateString
    }
}

private data class DateDisplayInfo(
    val text: String,
    val fontSize: Int
)

private fun formatSelectedDateDisplay(dateString: String): DateDisplayInfo {
    return try {
        val today = LocalDate.now()
        val selectedDate = LocalDate.parse(dateString)
        when {
            selectedDate.year == today.year -> {
                DateDisplayInfo("${selectedDate.monthValue}月${selectedDate.dayOfMonth}日", 18)
            }
            else -> {
                val shortYear = selectedDate.year % 100
                DateDisplayInfo("${shortYear}年${selectedDate.monthValue}月${selectedDate.dayOfMonth}日", 16)
            }
        }
    } catch (_: Exception) {
        DateDisplayInfo(dateString, 18)
    }
}
