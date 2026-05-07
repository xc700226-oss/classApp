package com.classapp.schedule.ui.schedule

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.classapp.schedule.data.model.Course
import com.classapp.schedule.ui.theme.*
import com.classapp.schedule.util.WeekUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// ── 课程网格数据模型 ──
private data class GridCourse(
    val course: Course,
    val active: Boolean,
    val rowStart: Int,
    val rowSpan: Int
)

private val DAY_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

// 滑动手势阈值 (px)
private const val SWIPE_THRESHOLD = 80f

@Composable
fun ScheduleScreen(
    onAddCourse: (Int?) -> Unit = {},
    onEditCourse: (Course) -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: ScheduleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showWeekend = WeekUtils.showWeekend
    val showNonCurrentWeek = WeekUtils.showNonCurrentWeek
    val maxDay = if (showWeekend) 7 else 5
    var selectedDay by remember { mutableIntStateOf(((LocalDate.now().dayOfWeek.value - 1) % 7) + 1) }
    var showDeleteDialog by remember { mutableStateOf<Course?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val gridCourses = remember(uiState.courses, uiState.currentWeek, showNonCurrentWeek) {
        buildGridCourses(uiState.courses, uiState.currentWeek, showNonCurrentWeek)
    }

    val today = LocalDate.now()
    val isCurrentWeek = uiState.currentWeek == WeekUtils.getCurrentWeek()
    val dayOfWeekName = DAY_NAMES.getOrElse(selectedDay - 1) { "周一" }
    val dateTitle = today.format(DateTimeFormatter.ofPattern("yy年MM月dd日"))
    val weekSubtitle = "第 ${uiState.currentWeek} 周" +
        if (isCurrentWeek) " (本周) $dayOfWeekName" else " $dayOfWeekName"

    // 滑动切周动画
    val swipeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var swipeAccumulator by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val screenWidthPx = with(density) { androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp.toPx() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .graphicsLayer { translationX = swipeOffset.value }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { swipeAccumulator = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        swipeAccumulator += dragAmount
                        coroutineScope.launch {
                            swipeOffset.snapTo(swipeAccumulator * 0.6f)
                        }
                    },
                    onDragEnd = {
                        val threshold = SWIPE_THRESHOLD
                        if (swipeAccumulator < -threshold) {
                            // 左滑 → 下一周，滑出动画
                            coroutineScope.launch {
                                swipeOffset.animateTo(-screenWidthPx, tween(200))
                                viewModel.nextWeek()
                                swipeOffset.snapTo(screenWidthPx)
                                swipeOffset.animateTo(0f, tween(200))
                            }
                        } else if (swipeAccumulator > threshold) {
                            // 右滑 → 上一周，滑出动画
                            coroutineScope.launch {
                                swipeOffset.animateTo(screenWidthPx, tween(200))
                                viewModel.previousWeek()
                                swipeOffset.snapTo(-screenWidthPx)
                                swipeOffset.animateTo(0f, tween(200))
                            }
                        } else {
                            // 未达阈值，弹回
                            coroutineScope.launch {
                                swipeOffset.animateTo(0f, tween(150))
                            }
                        }
                        swipeAccumulator = 0f
                    }
                )
            }
    ) {
        // ── 顶部导航栏 ──
        ScheduleTopBar(
            dateTitle = dateTitle,
            weekSubtitle = weekSubtitle,
            showMenu = showMenu,
            onMenuClick = { showMenu = !showMenu },
            onDismissMenu = { showMenu = false },
            onSettingsClick = onOpenSettings,
            onDeleteAllClick = { showDeleteAllDialog = true }
        )

        // ── 星期选择栏 ──
        DaySelectorBar(
            selectedDay = selectedDay,
            maxDay = maxDay,
            onDaySelected = { selectedDay = it }
        )

        // ── 课程表主体 (填满剩余空间) ──
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp)
        ) {
            val availableHeight = maxHeight
            val rowCount = WeekUtils.slotGroups.size
            // 每行高度 = 可用高度 / 行数，至少 60dp
            val rowHeight = maxOf((availableHeight / rowCount), 60.dp)

            Column(modifier = Modifier.fillMaxSize()) {
                for (rowIndex in 0 until rowCount) {
                    val label = WeekUtils.timeSlotLabels[rowIndex]
                    val rowCourses = gridCourses.filter { gc ->
                        (showNonCurrentWeek || gc.active) && gc.rowStart == rowIndex
                    }

                    ScheduleRow(
                        slotLabel = label,
                        rowCourses = rowCourses,
                        rowIndex = rowIndex,
                        selectedDay = selectedDay,
                        maxDay = maxDay,
                        rowHeight = rowHeight,
                        onCourseClick = { onEditCourse(it.course) },
                        onCourseLongClick = { showDeleteDialog = it.course },
                        onEmptySlot = { day -> onAddCourse(day) }
                    )
                }
            }
        }
    }

    // 删除确认对话框
    showDeleteDialog?.let { course ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除课程") },
            text = { Text("确定要删除「${course.name}」吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCourse(course)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppPrimary)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("取消") }
            }
        )
    }

    // 清空课表确认对话框
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("清空课表") },
            text = { Text("确定要删除整个学期的所有课程吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAll()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppPrimary)
                ) { Text("确认清空") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) { Text("取消") }
            }
        )
    }
}

// ══════════════════════════════════════════
// ── 顶部导航栏 ──
// ══════════════════════════════════════════
@Composable
private fun ScheduleTopBar(
    dateTitle: String,
    weekSubtitle: String,
    showMenu: Boolean,
    onMenuClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onSettingsClick: () -> Unit,
    onDeleteAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(AppBackground)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：汉堡菜单 + 下拉
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onMenuClick)
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "菜单",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("菜单", fontSize = 14.sp, color = TextPrimary)
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = onDismissMenu
            ) {
                DropdownMenuItem(
                    text = { Text("设置") },
                    onClick = { onDismissMenu(); onSettingsClick() }
                )
                DropdownMenuItem(
                    text = { Text("清空课表", color = AppPrimary) },
                    onClick = { onDismissMenu(); onDeleteAllClick() }
                )
            }
        }

        // 中间：日期 + 周次
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dateTitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = weekSubtitle,
                fontSize = 16.sp,
                color = AppPrimaryDark
            )
        }

        // 右侧：极窄屏标签
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(NarrowTagBg)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text("极窄屏", fontSize = 14.sp, color = NarrowTagText)
        }
    }
}

// ══════════════════════════════════════════
// ── 星期选择栏 ──
// ══════════════════════════════════════════
@Composable
private fun DaySelectorBar(
    selectedDay: Int,
    maxDay: Int,
    onDaySelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        // 左侧占位，与时间轴等宽
        Spacer(modifier = Modifier.width(80.dp))

        for (day in 1..maxDay) {
            val isSelected = day == selectedDay
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onDaySelected(day) }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = DAY_NAMES.getOrElse(day - 1) { "" },
                    fontSize = 18.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) DaySelected else DayUnselected
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(DaySelected)
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════
// ── 单行 ──
// ══════════════════════════════════════════
@Composable
private fun ScheduleRow(
    slotLabel: Triple<String, String, String>,
    rowCourses: List<GridCourse>,
    rowIndex: Int,
    selectedDay: Int,
    maxDay: Int,
    rowHeight: Dp,
    onCourseClick: (GridCourse) -> Unit,
    onCourseLongClick: (GridCourse) -> Unit,
    onEmptySlot: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
    ) {
        // 左侧时间轴
        Column(
            modifier = Modifier
                .width(80.dp)
                .fillMaxHeight()
                .background(AppBackground),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = slotLabel.first,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = slotLabel.second,
                fontSize = 12.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
            Text(
                text = slotLabel.third,
                fontSize = 12.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
        }

        // 右侧课程格子
        for (day in 1..maxDay) {
            val dayCourses = rowCourses.filter { it.course.dayOfWeek == day }
            val isSelectedDay = day == selectedDay

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (isSelectedDay) Color(0xFFFFF0F0) else AppSurface)
                    .clickable { onEmptySlot(day) },
                contentAlignment = Alignment.Center
            ) {
                if (dayCourses.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        dayCourses.forEach { gc ->
                            CourseCard(
                                course = gc.course,
                                active = gc.active,
                                rowSpan = gc.rowSpan,
                                rowHeight = rowHeight,
                                onClick = { onCourseClick(gc) },
                                onLongClick = { onCourseLongClick(gc) }
                            )
                        }
                    }
                }
            }
        }
    }

    // 行底部分隔线
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DividerColor)
    )
}

// ══════════════════════════════════════════
// ── 课程卡片 ──
// ══════════════════════════════════════════
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CourseCard(
    course: Course,
    active: Boolean,
    rowSpan: Int,
    rowHeight: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bgColor = CourseColors.getOrElse(course.colorIndex) { CourseColors[0] }
    val alpha = if (active) 1f else 0.4f
    val cardHeight = rowHeight * rowSpan

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .padding(horizontal = 1.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor.copy(alpha = alpha))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 4.dp, vertical = 3.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = course.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 3,
                lineHeight = 15.sp,
                overflow = TextOverflow.Ellipsis
            )
            if (course.classroom.isNotBlank()) {
                Text(
                    text = course.classroom,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    softWrap = true,
                    lineHeight = 13.sp
                )
            }
            if (course.teacher.isNotBlank()) {
                Text(
                    text = course.teacher,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

// ── Helper: 构建网格课程 ──
private fun buildGridCourses(
    allCourses: List<Course>,
    currentWeek: Int,
    showNonCurrentWeek: Boolean
): List<GridCourse> {
    val result = mutableListOf<GridCourse>()
    for (course in allCourses) {
        val active = WeekUtils.isCourseActiveThisWeek(course.weeks, currentWeek)
        if (!active && !showNonCurrentWeek) continue
        val startIdx = WeekUtils.getSlotGroupIndex(course.startSlot)
        val endIdx = WeekUtils.getSlotGroupIndex(course.endSlot)
        val span = endIdx - startIdx + 1
        if (startIdx in WeekUtils.slotGroups.indices) {
            result.add(GridCourse(course, active, startIdx, span))
        }
    }
    return result
}
