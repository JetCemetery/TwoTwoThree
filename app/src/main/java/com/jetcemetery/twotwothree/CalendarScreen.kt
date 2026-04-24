package com.jetcemetery.twotwothree

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
fun CalendarScreen(modifier: Modifier = Modifier, settingsManager: SettingsManager? = null) {
    val context = LocalContext.current
    val workDayLabel by settingsManager?.workDayLabel?.collectAsState(initial = "Work Day") ?: remember { mutableStateOf("Work Day") }
    val offDayLabel by settingsManager?.offDayLabel?.collectAsState(initial = "Off Day") ?: remember { mutableStateOf("Off Day") }
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
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
            compact = isLandscape,
            onTodayClick = {
                scope.launch {
                    pagerState.scrollToPage(initialPage)
                }
            },
            onSettingsClick = {
                context.startActivity(Intent(context, SettingsActivity::class.java))
            }
        )
        
        if (!isLandscape) {
            Spacer(modifier = Modifier.height(8.dp))
            DayOfWeekHeader()
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top,
            beyondViewportPageCount = 1
        ) { page ->
            val month = remember(page) { YearMonth.now().plusMonths((page - initialPage).toLong()) }
            Column {
                if (isLandscape) {
                    DayOfWeekHeader(compact = true)
                }
                CalendarGrid(month)
            }
        }
        
        if (!isLandscape) {
            ScheduleLegend(workDayLabel, offDayLabel)
        }
    }
}

@Composable
fun ScheduleLegend(workDayLabel: String, offDayLabel: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = MaterialTheme.colorScheme.primaryContainer, label = workDayLabel)
        Spacer(modifier = Modifier.width(24.dp))
        LegendItem(color = Color.Transparent, label = offDayLabel, border = true)
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
    compact: Boolean = false,
    onTodayClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = if (compact) 4.dp else 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.calendar),
                    contentDescription = null,
                    modifier = Modifier.size(if (compact) 24.dp else 32.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                    style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row {
                IconButton(
                    onClick = onTodayClick,
                    modifier = if (compact) Modifier.size(32.dp) else Modifier
                ) {
                    Icon(Icons.Default.Today, contentDescription = "Today")
                }
                IconButton(
                    onClick = onSettingsClick,
                    modifier = if (compact) Modifier.size(32.dp) else Modifier
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }
    }
}

@Composable
fun DayOfWeekHeader(compact: Boolean = false) {
    val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 2.dp else 8.dp)
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
            .fillMaxSize()
            .padding(vertical = 4.dp)
    ) {
        val totalDays = daysInMonth + offset
        val rows = (totalDays + 6) / 7
        
        for (row in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                for (column in 0 until 7) {
                    val index = row * 7 + column
                    val dayOfMonth = index - offset + 1
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayOfMonth in 1..daysInMonth) {
                            val date = remember(currentMonth, dayOfMonth) {
                                currentMonth.atDay(dayOfMonth)
                            }
                            DayItem(date)
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        val bubbleSize = minOf(maxWidth, maxHeight)
        Box(
            modifier = Modifier
                .size(bubbleSize)
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
}

@Preview(showBackground = true)
@Composable
fun CalendarPreview() {
    TwoTwoThreeTheme {
        CalendarScreen(settingsManager = null)
    }
}
