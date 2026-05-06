package com.classapp.schedule.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.classapp.schedule.util.WeekUtils

@Composable
fun WeekSelector(
    currentWeek: Int,
    onWeekSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // "第X周" title
        Text(
            text = "第 ${currentWeek} 周",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(WeekUtils.totalWeeks) { week ->
                val weekIndex = week + 1
                val isSelected = weekIndex == currentWeek
                val isCurrentWeek = weekIndex == WeekUtils.getCurrentWeek()

                val bgColor by animateColorAsState(
                    targetValue = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isCurrentWeek -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    label = "weekBg"
                )

                val textColor by animateColorAsState(
                    targetValue = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    label = "weekText"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .clickable { onWeekSelected(weekIndex) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$weekIndex",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected || isCurrentWeek) FontWeight.SemiBold else FontWeight.Normal,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
