package com.jetcemetery.twotwothree

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jetcemetery.twotwothree.ui.theme.TwoTwoThreeTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

@Composable
fun CalendarScreen(modifier: Modifier = Modifier) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val initialPage = 500 // Arbitrary middle point for the pager
    val pagerState = rememberPagerState(initialPage = initialPage) { 1000 }
    val scope = rememberCoroutineScope()

    // Synchronize currentMonth with pagerState
    LaunchedEffect(pagerState.currentPage) {
        val diff = pagerState.currentPage - initialPage
        currentMonth = YearMonth.now().plusMonths(diff.toLong())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CalendarHeader(
            currentMonth = currentMonth,
            onTodayClick = {
                scope.launch {
                    pagerState.scrollToPage(initialPage)
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        DayOfWeekHeader()
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top,
            beyondViewportPageCount = 1
        ) { page ->
            val month = remember(page) { YearMonth.now().plusMonths((page - initialPage).toLong()) }
            CalendarGrid(month)
        }
        ScheduleLegend()
    }
}

@Composable
fun ScheduleLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = MaterialTheme.colorScheme.primaryContainer, label = "Work Day")
        Spacer(modifier = Modifier.width(24.dp))
        LegendItem(color = Color.Transparent, label = "Off Day", border = true)
    }
}

@Composable
fun LegendItem(color: Color, label: String, border: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
                .then(if (border) Modifier.background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape) else Modifier)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun CalendarHeader(
    currentMonth: YearMonth,
    onTodayClick: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onTodayClick) {
                Icon(Icons.Default.Today, contentDescription = "Today")
            }
        }
    }
}

@Composable
fun DayOfWeekHeader() {
    val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        daysOfWeek.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun CalendarGrid(currentMonth: YearMonth) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstOfMonth = currentMonth.atDay(1)
    val firstDayOfWeek = firstOfMonth.dayOfWeek.value
    val offset = firstDayOfWeek - 1 // Monday-based offset

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        val totalDays = daysInMonth + offset
        val rows = (totalDays + 6) / 7
        
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (column in 0 until 7) {
                    val index = row * 7 + column
                    val dayOfMonth = index - offset + 1
                    
                    Box(modifier = Modifier.weight(1f)) {
                        if (dayOfMonth in 1..daysInMonth) {
                            val date = remember(currentMonth, dayOfMonth) {
                                currentMonth.atDay(dayOfMonth)
                            }
                            DayItem(date)
                        } else {
                            Spacer(modifier = Modifier.aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayItem(date: LocalDate) {
    val isWorkDay = remember(date) { ScheduleUtils.isWorkDay(date) }
    val isToday = remember(date) { date == LocalDate.now() }
    
    val containerColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        isWorkDay -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        else -> Color.Transparent
    }
    
    val contentColor = when {
        isToday -> MaterialTheme.colorScheme.onPrimary
        isWorkDay -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable { /* Handle date click if needed */ },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                color = contentColor,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
            if (isWorkDay && !isToday) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(contentColor, CircleShape)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarPreview() {
    TwoTwoThreeTheme {
        CalendarScreen()
    }
}
