package com.jetcemetery.twotwothree

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.jetcemetery.twotwothree.ui.theme.TwoTwoThreeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Immutable data class representing the final visual state of a day.
 */
@Immutable
data class DayViewState(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isWorkDay: Boolean,
    val isToday: Boolean,
    val isSwitchDate: Boolean,
    val hasCalendarEvent: Boolean,
    val containerColor: Color,
    val contentColor: Color
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(modifier: Modifier = Modifier, settingsManager: SettingsManager? = null) {
    val context = LocalContext.current
    val workDayLabel by settingsManager?.workDayLabel?.collectAsState(initial = "Work Day") ?: remember { mutableStateOf("Work Day") }
    val offDayLabel by settingsManager?.offDayLabel?.collectAsState(initial = "Off Day") ?: remember { mutableStateOf("Off Day") }
    val switchDates by settingsManager?.switchDates?.collectAsState(initial = emptySet()) ?: remember { mutableStateOf(emptySet()) }
    val scheduleType by settingsManager?.scheduleType?.collectAsState(initial = "2-2-3") ?: remember { mutableStateOf("2-2-3") }
    val onDayColorHex by settingsManager?.onDayColor?.collectAsState(initial = "#9E9E9E") ?: remember { mutableStateOf("#9E9E9E") }
    val offDayColorHex by settingsManager?.offDayColor?.collectAsState(initial = "#795548") ?: remember { mutableStateOf("#795548") }
    val selectedCalendarIds by settingsManager?.selectedCalendarIds?.collectAsState(initial = emptySet()) ?: remember { mutableStateOf(emptySet()) }
    
    var showSwitchDialog by remember { mutableStateOf<LocalDate?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }
    
    // Popup state
    var popupDate by remember { mutableStateOf<LocalDate?>(null) }
    var popupEvents by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val initialPage = 500
    val pagerState = rememberPagerState(initialPage = initialPage) { 1000 }
    val scope = rememberCoroutineScope()

    val currentMonth by remember {
        derivedStateOf {
            YearMonth.now().plusMonths((pagerState.currentPage - initialPage).toLong())
        }
    }

    val monthCache = remember { mutableStateMapOf<YearMonth, List<DayViewState>>() }
    val colorScheme = MaterialTheme.colorScheme

    fun calculateMonthData(month: YearMonth, calendarEvents: Set<LocalDate>): List<DayViewState> {
        val sortedSwitches = switchDates.toList().sorted()
        val today = LocalDate.now()
        val firstOfMonth = month.atDay(1)
        val gridOffset = firstOfMonth.dayOfWeek.value - 1
        
        val onDayColor = ColorPalette.fromHex(onDayColorHex)
        val offDayColor = ColorPalette.fromHex(offDayColorHex)

        return (0 until 42).map { i ->
            val date = firstOfMonth.plusDays((i - gridOffset).toLong())
            val isWorkDay = ScheduleUtils.isWorkDaySwitchedOptimized(date, sortedSwitches, scheduleType)
            val isToday = date == today
            val isSwitchDate = switchDates.contains(date)
            val hasEvent = calendarEvents.contains(date)
            
            val containerColor = if (isWorkDay) onDayColor else offDayColor
            val contentColor = when {
                isSwitchDate -> Color(0xFF2E7D32)
                isWorkDay -> ColorPalette.getContrastColor(onDayColor)
                else -> ColorPalette.getContrastColor(offDayColor)
            }

            DayViewState(
                date = date,
                isCurrentMonth = date.monthValue == month.monthValue && date.year == month.year,
                isWorkDay = isWorkDay,
                isToday = isToday,
                isSwitchDate = isSwitchDate,
                hasCalendarEvent = hasEvent,
                containerColor = containerColor,
                contentColor = contentColor
            )
        }
    }

    // Proactive background calculation. Clears on launch/resume due to LaunchedEffect trigger
    LaunchedEffect(pagerState.currentPage, switchDates, scheduleType, colorScheme, onDayColorHex, offDayColorHex, selectedCalendarIds) {
        withContext(Dispatchers.Default) {
            val currentCenter = pagerState.currentPage
            
            val calendarEvents = if (selectedCalendarIds.isNotEmpty()) {
                try {
                    val start = YearMonth.now().plusMonths((currentCenter - initialPage - 4).toLong()).atDay(1)
                    val end = YearMonth.now().plusMonths((currentCenter - initialPage + 4).toLong()).atEndOfMonth()
                    CalendarUtils.getEventDates(context, start, end, selectedCalendarIds)
                } catch (e: Exception) { emptySet() }
            } else emptySet()

            for (offset in -3..3) {
                if (offset == 0) continue 
                val page = currentCenter + offset
                val month = YearMonth.now().plusMonths((page - initialPage).toLong())
                if (!monthCache.containsKey(month)) {
                    monthCache[month] = calculateMonthData(month, calendarEvents)
                }
            }
        }
    }

    LaunchedEffect(switchDates, scheduleType, onDayColorHex, offDayColorHex, selectedCalendarIds) {
        monthCache.clear()
    }

    // Confirmation Dialog
    showSwitchDialog?.let { date ->
        AlertDialog(
            onDismissRequest = { showSwitchDialog = null },
            title = { Text("Switch Schedule?") },
            text = { Text("Do you want to switch the schedule starting from ${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())}? This will affect all following days.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { settingsManager?.toggleSwitchDate(date) }
                    showSwitchDialog = null
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showSwitchDialog = null }) { Text("No") }
            }
        )
    }

    if (showMonthPicker) {
        YearMonthPickerDialog(
            initialMonth = currentMonth,
            onDismiss = { showMonthPicker = false },
            onConfirm = { selectedMonth ->
                scope.launch {
                    val monthsDiff = ChronoUnit.MONTHS.between(YearMonth.now(), selectedMonth).toInt()
                    pagerState.scrollToPage(initialPage + monthsDiff)
                }
                showMonthPicker = false
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CalendarHeader(
            currentMonth = currentMonth,
            compact = isLandscape,
            onTodayClick = { scope.launch { pagerState.scrollToPage(initialPage) } },
            onSettingsClick = { context.startActivity(Intent(context, SettingsActivity::class.java)) },
            onMonthClick = { showMonthPicker = true }
        )
        
        if (!isLandscape) {
            Spacer(modifier = Modifier.height(8.dp))
            DayOfWeekHeader()
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top,
            beyondViewportPageCount = 2,
            key = { it }
        ) { page ->
            val month = remember(page) { YearMonth.now().plusMonths((page - initialPage).toLong()) }
            
            val daysData = monthCache[month] ?: remember(month, switchDates, scheduleType, onDayColorHex, offDayColorHex, selectedCalendarIds, colorScheme) {
                val calendarEvents = if (selectedCalendarIds.isNotEmpty()) {
                    try {
                        CalendarUtils.getEventDates(context, month.atDay(1), month.atEndOfMonth(), selectedCalendarIds)
                    } catch (e: Exception) { emptySet() }
                } else emptySet()
                calculateMonthData(month, calendarEvents)
            }

            Column {
                if (isLandscape) {
                    DayOfWeekHeader(compact = true)
                }
                CalendarGrid(
                    daysData = daysData,
                    isLandscape = isLandscape,
                    onDayLongClick = { showSwitchDialog = it },
                    onDayClick = { date ->
                        if (selectedCalendarIds.isNotEmpty()) {
                            scope.launch {
                                val events = withContext(Dispatchers.IO) {
                                    CalendarUtils.getEventsForDate(context, date, selectedCalendarIds)
                                }
                                if (events.isNotEmpty()) {
                                    popupEvents = events
                                    popupDate = date
                                }
                            }
                        }
                    }
                )
            }
        }
        
        // Event Popup - Moved outside Pager to prevent multiple instances
        popupDate?.let { date ->
            Popup(
                alignment = Alignment.Center,
                onDismissRequest = { popupDate = null },
                properties = PopupProperties(focusable = true)
            ) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .widthIn(max = 300.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Events on ${date.dayOfMonth} ${date.month.getDisplayName(TextStyle.FULL, Locale.getDefault())}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val showAccount = popupEvents.size <= 3
                        
                        Column(
                            modifier = Modifier
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            popupEvents.forEach { event ->
                                Text(
                                    text = if (showAccount) "${event.accountName}: ${event.title}" else event.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }

                        TextButton(
                            onClick = { popupDate = null },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
        
        ScheduleLegend(
            workDayLabel = workDayLabel, 
            offDayLabel = offDayLabel, 
            onDayColorHex = onDayColorHex,
            offDayColorHex = offDayColorHex,
            compact = isLandscape
        )
    }
}

@Composable
fun CalendarGrid(
    daysData: List<DayViewState>,
    isLandscape: Boolean,
    onDayLongClick: (LocalDate) -> Unit,
    onDayClick: (LocalDate) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp)
    ) {
        for (row in 0 until 6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                for (column in 0 until 7) {
                    val dayData = daysData[row * 7 + column]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayData.isCurrentMonth) {
                            DayItem(
                                dayData = dayData,
                                isLandscape = isLandscape,
                                onLongClick = { onDayLongClick(dayData.date) },
                                onClick = { onDayClick(dayData.date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DayItem(
    dayData: DayViewState,
    isLandscape: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Uniform Container Box: Constrained by height in landscape to prevent overlapping rows
        Box(
            modifier = Modifier
                .then(if (isLandscape) Modifier.fillMaxHeight(0.95f) else Modifier.fillMaxSize(0.85f))
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            // Core Date Circle: Sized relative to the container
            Box(
                modifier = Modifier
                    .fillMaxSize(if (isLandscape) 0.65f else 0.85f)
                    .aspectRatio(1f)
                    .background(dayData.containerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val baseStyle = if (isLandscape) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge
                    Text(
                        text = dayData.date.dayOfMonth.toString(),
                        color = dayData.contentColor,
                        style = if (dayData.isSwitchDate) {
                            baseStyle.copy(
                                fontSize = (baseStyle.fontSize.value + 1).sp,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic
                            )
                        } else {
                            baseStyle.copy(
                                fontWeight = if (dayData.isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    if (dayData.isWorkDay) {
                        Box(
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .size(if (isLandscape) 2.dp else 4.dp)
                                .background(dayData.contentColor, CircleShape)
                        )
                    }
                }
            }

            // High-Visibility Indicators
            if (isLandscape) {
                // Landscape: Concentric Encompassing Rings
                if (dayData.hasCalendarEvent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.82f)
                            .aspectRatio(1f)
                            .border(1.5.dp, Color(0xFFFF9800), CircleShape)
                    )
                }
                if (dayData.isToday) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(1.0f)
                            .aspectRatio(1f)
                            .border(1.5.dp, Color(0xFF2E7D32), CircleShape)
                    )
                }
            } else {
                // Portrait: Precision Bars (anchored 2dp from circle)
                if (dayData.hasCalendarEvent) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth(0.65f)
                            .height(3.dp)
                            .background(Color(0xFFFF9800), RoundedCornerShape(2.dp))
                    )
                }
                if (dayData.isToday) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(0.65f)
                            .height(3.dp)
                            .background(Color(0xFF2E7D32), RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleLegend(
    workDayLabel: String, 
    offDayLabel: String, 
    onDayColorHex: String,
    offDayColorHex: String,
    compact: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(if (compact) 4.dp else 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = ColorPalette.fromHex(onDayColorHex), label = workDayLabel)
        Spacer(modifier = Modifier.width(if (compact) 12.dp else 24.dp))
        LegendItem(color = ColorPalette.fromHex(offDayColorHex), label = offDayLabel, border = true)
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
    onSettingsClick: () -> Unit,
    onMonthClick: () -> Unit
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onMonthClick() }
            ) {
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
fun YearMonthPickerDialog(
    initialMonth: YearMonth,
    onDismiss: () -> Unit,
    onConfirm: (YearMonth) -> Unit
) {
    var selectedMonth by remember { mutableStateOf(initialMonth.monthValue) }
    var selectedYear by remember { mutableStateOf(initialMonth.year) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Month and Year") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Simple Year Selector
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Year", style = MaterialTheme.typography.labelMedium)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            var expanded by remember { mutableStateOf(false) }
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(selectedYear.toString(), style = MaterialTheme.typography.bodyMedium)
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                // Range of years: Current year +/- 50
                                val currentYear = YearMonth.now().year
                                (currentYear - 50..currentYear + 50).forEach { year ->
                                    DropdownMenuItem(
                                        text = { Text(year.toString(), style = MaterialTheme.typography.bodyMedium) },
                                        onClick = {
                                            selectedYear = year
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Simple Month Selector
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Month", style = MaterialTheme.typography.labelMedium)
                        Box(modifier = Modifier.fillMaxWidth()) {
                            var expanded by remember { mutableStateOf(false) }
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(
                                    java.time.Month.of(selectedMonth).getDisplayName(TextStyle.FULL, Locale.getDefault()),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                (1..12).forEach { month ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                java.time.Month.of(month).getDisplayName(TextStyle.FULL, Locale.getDefault()),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        onClick = {
                                            selectedMonth = month
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(YearMonth.of(selectedYear, selectedMonth)) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun CalendarPreview() {
    TwoTwoThreeTheme {
        CalendarScreen(settingsManager = null)
    }
}
