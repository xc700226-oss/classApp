package com.classapp.schedule.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.classapp.schedule.util.WeekUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onImportWebView: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showWeeksPicker by remember { mutableStateOf(false) }
    var showTimeSlotEditor by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.parseExcelFile(it) }
    }

    LaunchedEffect(uiState.importStatus) {
        when (uiState.importStatus) {
            is ImportStatus.Done -> {
                snackbarHostState.showSnackbar(uiState.importMessage)
                viewModel.resetImportStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "课程表设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── 基本设置 ──
            SectionHeader("基本设置")

            // 开始上课时间
            SettingsItem(
                title = "开始上课时间",
                subtitle = "课表开始的第一天，不是开学时间",
                value = uiState.semesterStart.format(DateTimeFormatter.ofPattern("yyyy/M/dd")),
                showArrow = true,
                onClick = { showDatePicker = true }
            )

            // 当前的周数（只读，由 semesterStart 自动计算）
            SettingsItem(
                title = "当前的周数",
                subtitle = "开学到现在几周，便于我们确定单双周",
                value = "第${WeekUtils.getCurrentWeek()}周",
                showArrow = false,
                onClick = {}
            )

            // 本学期总周数
            SettingsItem(
                title = "本学期总周数",
                subtitle = "请选择本学期总共多少周",
                value = "${uiState.totalWeeks}周",
                showArrow = true,
                onClick = { showWeeksPicker = true }
            )

            // 是否显示周末
            SettingsItemWithSwitch(
                title = "是否显示周末",
                subtitle = "如果周末有课程，可打开该设置",
                checked = uiState.showWeekend,
                onCheckedChange = { viewModel.toggleShowWeekend(it) }
            )

            // 是否显示非本周课程
            SettingsItemWithSwitch(
                title = "是否显示非本周课程",
                subtitle = "开启后单双周课程都可以看见哦",
                checked = uiState.showNonCurrentWeek,
                onCheckedChange = { viewModel.toggleShowNonCurrentWeek(it) }
            )

            // 课表时间设置
            SettingsItem(
                title = "课表时间设置",
                subtitle = "自定义每节课的上课和下课时间",
                value = null,
                showArrow = true,
                onClick = { showTimeSlotEditor = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── 教务系统账号 ──
            SectionHeader("教务系统账号")

            // 教务系统网址
            OutlinedTextField(
                value = uiState.portalUrl,
                onValueChange = { viewModel.updatePortalUrl(it) },
                label = { Text("教务系统网址") },
                placeholder = { Text("https://jwxt.example.edu.cn") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            )

            // 学号
            OutlinedTextField(
                value = uiState.username,
                onValueChange = { viewModel.updateUsername(it) },
                label = { Text("学号") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            )

            // 密码
            OutlinedTextField(
                value = uiState.password,
                onValueChange = { viewModel.updatePassword(it) },
                label = { Text("密码") },
                placeholder = {
                    if (uiState.hasStoredCredentials) Text("已保存，不修改请留空")
                },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            )

            // 保存凭据按钮
            Button(
                onClick = { viewModel.saveCredentials() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("保存凭据")
            }

            // 自动同步开关
            SettingsItemWithSwitch(
                title = "自动同步课表",
                subtitle = "每24小时自动从教务系统获取最新课表",
                checked = uiState.autoFetchEnabled,
                onCheckedChange = { viewModel.toggleAutoFetch(it) }
            )

            // 立即同步
            SettingsItem(
                title = "立即同步",
                subtitle = if (uiState.lastSyncTime > 0) {
                    "上次同步: ${java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(uiState.lastSyncTime))}"
                } else {
                    "点击立即从教务系统获取课表"
                },
                value = null,
                showArrow = true,
                onClick = { viewModel.manualSync() }
            )

            // 同步状态
            when (val status = uiState.syncStatus) {
                is SyncStatus.Syncing -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                is SyncStatus.Success -> {
                    Text(
                        status.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                is SyncStatus.Error -> {
                    Text(
                        status.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 导入课表 ──
            SectionHeader("导入课表")

            // Excel import
            SettingsItem(
                title = "导入 Excel 课表",
                subtitle = "支持 .xlsx 格式，列表格式或周课表格式",
                value = null,
                showArrow = true,
                onClick = {
                    filePickerLauncher.launch(arrayOf(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    ))
                }
            )

            // WebView import
            SettingsItem(
                title = "从教务系统导入",
                subtitle = "通过 WebVPN 登录青果/强智教务系统导入",
                value = null,
                showArrow = true,
                onClick = onImportWebView
            )

            if (uiState.importStatus is ImportStatus.Parsing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            if (uiState.importStatus is ImportStatus.Error) {
                Text(
                    uiState.importMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── 关于 ──
            SectionHeader("关于")

            SettingsItem(
                title = "ClassApp 课程表",
                subtitle = "版本 1.0.0 · 大学生课表管理工具",
                value = null,
                showArrow = false,
                onClick = {}
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.semesterStart
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            // Snap to Monday
                            val monday = if (date.dayOfWeek == java.time.DayOfWeek.SUNDAY) {
                                date.minusDays(6)
                            } else {
                                date.minusDays(date.dayOfWeek.value.toLong() - 1)
                            }
                            viewModel.updateSemesterStart(monday)
                        }
                        showDatePicker = false
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Total weeks picker dialog
    if (showWeeksPicker) {
        WeeksPickerDialog(
            currentWeeks = uiState.totalWeeks,
            onConfirm = { viewModel.updateTotalWeeks(it); showWeeksPicker = false },
            onDismiss = { showWeeksPicker = false }
        )
    }

    // Time slot editor dialog
    if (showTimeSlotEditor) {
        TimeSlotEditorDialog(
            onDismiss = { showTimeSlotEditor = false }
        )
    }

    // Import preview dialog
    if (uiState.importStatus is ImportStatus.Preview || uiState.importStatus is ImportStatus.Importing) {
        val courses = uiState.parsedCourses
        AlertDialog(
            onDismissRequest = {
                if (uiState.importStatus !is ImportStatus.Importing) viewModel.cancelImport()
            },
            icon = {
                if (uiState.importStatus is ImportStatus.Importing) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            },
            title = { Text(if (uiState.importStatus is ImportStatus.Importing) "正在导入..." else "确认导入") },
            text = {
                Column {
                    Text("共解析出 ${courses.size} 门课程")
                    Spacer(modifier = Modifier.height(8.dp))
                    courses.take(5).forEach { course ->
                        val day = getDayName(course.dayOfWeek)
                        val oddEvenText = when (course.oddEven) {
                            1 -> " 单周"
                            2 -> " 双周"
                            else -> ""
                        }
                        Text(
                            "• ${course.name}（${day} 第${course.startSlot}-${course.endSlot}节$oddEvenText）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (courses.size > 5) {
                        Text("... 还有 ${courses.size - 5} 门",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                if (uiState.importStatus !is ImportStatus.Importing) {
                    Button(onClick = { viewModel.confirmImport() }) { Text("确认导入") }
                }
            },
            dismissButton = {
                if (uiState.importStatus !is ImportStatus.Importing) {
                    TextButton(onClick = { viewModel.cancelImport() }) { Text("取消") }
                }
            }
        )
    }
}

@Composable
private fun WeeksPickerDialog(
    currentWeeks: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableIntStateOf(currentWeeks) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("本学期总周数") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("选择本学期总周数", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { if (selected > 10) selected-- }) {
                        Text("-", style = MaterialTheme.typography.headlineMedium)
                    }
                    Text(
                        "$selected",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    IconButton(onClick = { if (selected < 30) selected++ }) {
                        Text("+", style = MaterialTheme.typography.headlineMedium)
                    }
                }
                // Quick select chips
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(16, 18, 20, 22).forEach { w ->
                        FilterChip(
                            selected = selected == w,
                            onClick = { selected = w },
                            label = { Text("${w}周") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun TimeSlotEditorDialog(
    onDismiss: () -> Unit
) {
    val slotCount = WeekUtils.timeSlotLabels.size
    val startTimes = remember {
        mutableStateListOf(*WeekUtils.timeSlotLabels.map { it.second }.toTypedArray())
    }
    val endTimes = remember {
        mutableStateListOf(*WeekUtils.timeSlotLabels.map { it.third }.toTypedArray())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("课表时间设置") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "自定义每节课的上课和下课时间",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                for (i in 0 until slotCount) {
                    val label = WeekUtils.timeSlotLabels[i].first
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(48.dp)
                        )

                        OutlinedTextField(
                            value = startTimes[i],
                            onValueChange = { startTimes[i] = it },
                            label = { Text("开始", fontSize = 11.sp) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Text("~", modifier = Modifier.padding(horizontal = 2.dp))

                        OutlinedTextField(
                            value = endTimes[i],
                            onValueChange = { endTimes[i] = it },
                            label = { Text("结束", fontSize = 11.sp) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                TextButton(
                    onClick = {
                        val defaults = listOf(
                            "08:30" to "10:00",
                            "10:10" to "11:40",
                            "13:00" to "14:30",
                            "14:40" to "16:10",
                            "16:40" to "17:20"
                        )
                        for (i in 0 until minOf(slotCount, defaults.size)) {
                            startTimes[i] = defaults[i].first
                            endTimes[i] = defaults[i].second
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("恢复默认")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    for (i in 0 until slotCount) {
                        val s = startTimes[i].trim()
                        val e = endTimes[i].trim()
                        if (s.matches(Regex("\\d{2}:\\d{2}")) && e.matches(Regex("\\d{2}:\\d{2}"))) {
                            WeekUtils.updateSlotTime(i, s, e)
                        }
                    }
                    onDismiss()
                }
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun getDayName(day: Int): String = when (day) {
    1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"
    5 -> "周五"; 6 -> "周六"; 7 -> "周日"; else -> ""
}

// ── Section Header ──
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        fontSize = 14.sp,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

// ── Settings Item (title + subtitle + value + arrow) ──
@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    value: String?,
    showArrow: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            if (showArrow) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }

    // Divider
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        thickness = 0.5.dp
    )
}

// ── Settings Item with Switch ──
@Composable
private fun SettingsItemWithSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        thickness = 0.5.dp
    )
}
