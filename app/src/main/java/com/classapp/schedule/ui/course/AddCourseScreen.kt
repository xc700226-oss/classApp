package com.classapp.schedule.ui.course

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.classapp.schedule.ui.theme.CourseColorBackgrounds
import com.classapp.schedule.ui.theme.CourseColors
import com.classapp.schedule.util.WeekUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseScreen(
    onBack: () -> Unit,
    viewModel: CourseViewModel = viewModel()
) {
    val formState by viewModel.formState.collectAsState()
    val isEditing = formState.editingCourseId != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "编辑课程" else "添加课程",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                tonalElevation = 1.dp
            ) {
                Button(
                    onClick = { viewModel.save(onSuccess = onBack) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = formState.name.isNotBlank()
                ) {
                    Icon(
                        if (isEditing) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isEditing) "保存修改" else "添加课程",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Course name
            OutlinedTextField(
                value = formState.name,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("课程名称") },
                placeholder = { Text("例：高等数学") },
                leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                )
            )

            // Teacher & Classroom
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = formState.teacher,
                    onValueChange = { viewModel.updateTeacher(it) },
                    label = { Text("授课教师") },
                    placeholder = { Text("教师姓名") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = formState.classroom,
                    onValueChange = { viewModel.updateClassroom(it) },
                    label = { Text("上课教室") },
                    placeholder = { Text("教室") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Day of week
            SectionLabel("上课日期")
            DaySelector(
                selectedDay = formState.dayOfWeek,
                onDaySelected = { viewModel.updateDay(it) }
            )

            // Time slots
            SectionLabel("上课节次")
            SlotSelector(
                startSlot = formState.startSlot,
                endSlot = formState.endSlot,
                onStartChange = { viewModel.updateStartSlot(it) },
                onEndChange = { viewModel.updateEndSlot(it) }
            )

            // Week range
            SectionLabel("上课周次")
            OutlinedTextField(
                value = formState.weeks,
                onValueChange = { viewModel.updateWeeks(it) },
                label = { Text("周次") },
                placeholder = { Text("例：1-13 或 1-4,6,8,10,12,14 或 3") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                supportingText = { Text("支持格式：1-13（连续）、3（单周）、1-4,6,8（混合）、1-13(单)（单双周）") }
            )

            // Color picker
            SectionLabel("标签颜色")
            ColorPicker(
                selectedIndex = formState.colorIndex,
                onColorSelected = { viewModel.updateColor(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun DaySelector(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for (day in 1..7) {
            val isSelected = day == selectedDay
            FilterChip(
                selected = isSelected,
                onClick = { onDaySelected(day) },
                label = {
                    Text(
                        WeekUtils.getDayName(day),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotSelector(
    startSlot: Int,
    endSlot: Int,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit
) {
    val slotLabels = listOf(
        "1-2节" to 1,
        "3-4节" to 3,
        "5-6节" to 5,
        "7-8节" to 7,
        "9-10节" to 9
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Start slot
        var startExpanded by remember { mutableStateOf(false) }
        val startLabel = slotLabels.find { it.second == startSlot }?.first ?: "${startSlot}节"

        ExposedDropdownMenuBox(
            expanded = startExpanded,
            onExpandedChange = { startExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = startLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("开始节次") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = startExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            ExposedDropdownMenu(
                expanded = startExpanded,
                onDismissRequest = { startExpanded = false }
            ) {
                slotLabels.filter { it.second <= endSlot }.forEach { (label, slot) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onStartChange(slot)
                            startExpanded = false
                        }
                    )
                }
            }
        }

        // End slot
        var endExpanded by remember { mutableStateOf(false) }
        val endLabel = slotLabels.find { it.second == endSlot }?.first ?: "${endSlot}节"

        ExposedDropdownMenuBox(
            expanded = endExpanded,
            onExpandedChange = { endExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = endLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("结束节次") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = endExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            ExposedDropdownMenu(
                expanded = endExpanded,
                onDismissRequest = { endExpanded = false }
            ) {
                slotLabels.filter { it.second >= startSlot }.forEach { (label, slot) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onEndChange(slot + 1)
                            endExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorPicker(
    selectedIndex: Int,
    onColorSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CourseColors.forEachIndexed { index, color ->
            val isSelected = index == selectedIndex

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .then(
                        if (isSelected) Modifier.border(3.dp, color.copy(alpha = 0.3f), CircleShape)
                        else Modifier
                    )
                    .clickable { onColorSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
