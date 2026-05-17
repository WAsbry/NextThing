package com.nextthing.app.presentation.screens.tasks

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
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
    onNavigateToTaskDetail: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

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
            activeFilterCount = uiState.activeFilterCount,
            onSearchToggle = {
                viewModel.setSearchActive(!uiState.isSearchActive)
            },
            onSearchQueryChange = { viewModel.setSearchQuery(it) },
            onClearFilters = { viewModel.clearAllFilters() }
        )

        // ── 筛选栏 ──
        TasksFilterBar(
            selectedView = uiState.selectedView,
            currentWeekOffset = uiState.currentWeekOffset,
            statusFilter = uiState.statusFilter,
            selectedCategoryId = uiState.selectedCategoryId,
            priorityFilter = uiState.priorityFilter,
            availableCategories = uiState.availableCategories,
            weekLabel = viewModel.getWeekLabel(uiState.currentWeekOffset),
            onViewSelected = { viewModel.selectView(it) },
            onWeekPrev = { viewModel.changeWeek(uiState.currentWeekOffset - 1) },
            onWeekNext = { viewModel.changeWeek(uiState.currentWeekOffset + 1) },
            openDropdown = openDropdown,
            onOpenDropdown = { openDropdown = it },
            onStatusFilterSelected = {
                viewModel.setStatusFilter(it)
                openDropdown = null
            },
            onCategoryFilterSelected = {
                viewModel.setCategoryFilter(it)
                openDropdown = null
            },
            onPriorityFilterSelected = {
                viewModel.setPriorityFilter(it)
                openDropdown = null
            },
            onDismissDropdown = { openDropdown = null }
        )

        // ── 每日名句 ──
        if (!uiState.isSearchActive) {
            DailyQuoteBanner(quote = uiState.dailyQuote)
        }

        // ── 内容区域 ──
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Primary)
            }
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
                        currentMonth = uiState.currentMonth,
                        selectedDate = uiState.selectedDate,
                        selectedDateTasks = uiState.selectedDateTasks,
                        selectedDateCompletedCount = uiState.selectedDateCompletedCount,
                        selectedDatePendingCount = uiState.selectedDatePendingCount,
                        selectedDateOverdueCount = uiState.selectedDateOverdueCount,
                        selectedDateCancelledCount = uiState.selectedDateCancelledCount,
                        onDateSelected = { viewModel.selectDate(it) },
                        onPreviousMonth = { viewModel.previousMonth() },
                        onNextMonth = { viewModel.nextMonth() },
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
    activeFilterCount: Int,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearFilters: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BgCard,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isSearchActive) {
                Spacer(modifier = Modifier.width(12.dp))
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

                // 筛选状态 badge
                if (activeFilterCount > 0) {
                    TextButton(onClick = onClearFilters) {
                        Text(
                            text = "清除筛选($activeFilterCount)",
                            fontSize = 12.sp,
                            color = Primary
                        )
                    }
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
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
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

                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "清除",
                            tint = TextSecondary
                        )
                    }
                }

                LaunchedEffect(isSearchActive) {
                    if (isSearchActive) {
                        focusRequester.requestFocus()
                    }
                }
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
    currentWeekOffset: Int,
    statusFilter: StatusFilter,
    selectedCategoryId: String?,
    priorityFilter: PriorityFilter,
    availableCategories: List<Category>,
    weekLabel: String,
    onViewSelected: (TaskView) -> Unit,
    onWeekPrev: () -> Unit,
    onWeekNext: () -> Unit,
    openDropdown: String?,
    onOpenDropdown: (String) -> Unit,
    onStatusFilterSelected: (StatusFilter) -> Unit,
    onCategoryFilterSelected: (String?) -> Unit,
    onPriorityFilterSelected: (PriorityFilter) -> Unit,
    onDismissDropdown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 周/月切换
        ViewSwitchPills(
            selectedView = selectedView,
            onViewSelected = onViewSelected
        )

        Spacer(modifier = Modifier.width(6.dp))

        // 周导航（仅周视图）
        if (selectedView == TaskView.LIST) {
            WeekNavRow(
                weekLabel = weekLabel,
                onPrev = onWeekPrev,
                onNext = onWeekNext
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        // 筛选chips（横向滚动）
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            // 状态筛选
            item {
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

            // 分类筛选
            item {
                val categoryLabel = if (selectedCategoryId == null) "分类"
                else availableCategories.find { it.id == selectedCategoryId }?.name ?: "分类"
                Box {
                    FilterChipButton(
                        label = categoryLabel,
                        isActive = selectedCategoryId != null,
                        onClick = { onOpenDropdown("category") }
                    )
                    CategoryFilterDropdown(
                        expanded = openDropdown == "category",
                        categories = availableCategories,
                        selectedId = selectedCategoryId,
                        onSelect = onCategoryFilterSelected,
                        onDismiss = onDismissDropdown
                    )
                }
            }

            // 优先级筛选
            item {
                Box {
                    FilterChipButton(
                        label = if (priorityFilter == PriorityFilter.ALL) "优先级" else priorityFilter.label,
                        isActive = priorityFilter != PriorityFilter.ALL,
                        onClick = { onOpenDropdown("priority") }
                    )
                    PriorityFilterDropdown(
                        expanded = openDropdown == "priority",
                        current = priorityFilter,
                        onSelect = onPriorityFilterSelected,
                        onDismiss = onDismissDropdown
                    )
                }
            }
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
            .background(BgSecondary, RoundedCornerShape(20.dp))
            .padding(3.dp)
    ) {
        TaskView.entries.forEach { view ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(17.dp))
                    .background(if (selectedView == view) Primary else Color.Transparent)
                    .clickable { onViewSelected(view) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
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
private fun WeekNavRow(
    weekLabel: String,
    onPrev: () -> Unit,
    onNext: () -> Unit
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
            text = weekLabel,
            fontSize = 12.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 2.dp)
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
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) Primary.copy(alpha = 0.12f) else BgSecondary,
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
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        StatusFilter.entries.forEach { filter ->
            DropdownMenuItem(
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
//  每日名句横幅
// ══════════════════════════════════════════

@Composable
private fun DailyQuoteBanner(quote: String) {
    if (quote.isBlank()) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Primary.copy(alpha = 0.06f))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = "💬 $quote",
            fontSize = 12.sp,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
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
    onTaskClick: (Task) -> Unit,
    onToggleStatus: (String) -> Unit,
    onDefer: (String) -> Unit,
    onCancel: (String) -> Unit
) {
    if (results.isEmpty()) {
        SearchEmptyState(query = query)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Text(
                text = "找到 ${results.size} 个结果",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
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
    currentMonth: String,
    selectedDate: String?,
    selectedDateTasks: List<Task>,
    selectedDateCompletedCount: Int,
    selectedDatePendingCount: Int,
    selectedDateOverdueCount: Int,
    selectedDateCancelledCount: Int,
    onDateSelected: (String) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onNavigateToTaskDetail: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // 月份导航
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "上月",
                        tint = TextSecondary
                    )
                }
                Text(
                    text = currentMonth,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                IconButton(onClick = onNextMonth, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "下月",
                        tint = TextSecondary
                    )
                }
            }
        }

        // 星期标题
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
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
        }

        // 日历网格
        item {
            CalendarGrid(
                calendarDays = calendarDays,
                selectedDate = selectedDate,
                onDateSelected = onDateSelected
            )
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
            .padding(horizontal = 16.dp)
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
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard)
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
            Text(text = "📋", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "暂无任务",
                fontSize = 16.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "调整筛选条件或切换周试试",
                fontSize = 13.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun SearchEmptyState(query: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🔍", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "没有找到\"$query\"",
                fontSize = 15.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
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
