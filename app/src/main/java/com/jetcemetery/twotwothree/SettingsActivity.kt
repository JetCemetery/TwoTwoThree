package com.jetcemetery.twotwothree

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.jetcemetery.twotwothree.ui.theme.TwoTwoThreeTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsManager = SettingsManager(this)
        enableEdgeToEdge()
        setContent {
            TwoTwoThreeTheme {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Settings") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    SettingsScreen(
                        modifier = Modifier.padding(innerPadding),
                        settingsManager = settingsManager,
                        onSaveComplete = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier, 
    settingsManager: SettingsManager,
    onSaveComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // Initial values from DataStore
    val currentScheduleType by settingsManager.scheduleType.collectAsState(initial = "2-2-3")
    val savedLoadCalendarEvents by settingsManager.loadCalendarEvents.collectAsState(initial = false)
    val savedSelectedCalendarIds by settingsManager.selectedCalendarIds.collectAsState(initial = emptySet())

    // Local state to prevent lag and cursor jumping
    var onDayText by remember { mutableStateOf("") }
    var offDayText by remember { mutableStateOf("") }
    var onDayColorHex by remember { mutableStateOf("#9E9E9E") }
    var offDayColorHex by remember { mutableStateOf("#795548") }
    var loadCalendarEvents by remember { mutableStateOf(false) }
    var selectedCalendarIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    var showAboutDialog by remember { mutableStateOf(false) }
    var showScheduleWarning by remember { mutableStateOf<String?>(null) }
    var showCalendarPickerDialog by remember { mutableStateOf(false) }
    var availableCalendars by remember { mutableStateOf<List<CalendarInfo>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val calendarGranted = permissions[Manifest.permission.READ_CALENDAR] ?: false
        if (calendarGranted) {
            availableCalendars = CalendarUtils.getAvailableCalendars(context)
            showCalendarPickerDialog = true
        } else {
            Toast.makeText(context, "Permissions required to sync Google Accounts", Toast.LENGTH_LONG).show()
        }
    }

    // Initialize local state with current values from DataStore once
    LaunchedEffect(Unit) {
        onDayText = settingsManager.workDayLabel.first()
        offDayText = settingsManager.offDayLabel.first()
        onDayColorHex = settingsManager.onDayColor.first()
        offDayColorHex = settingsManager.offDayColor.first()
        loadCalendarEvents = settingsManager.loadCalendarEvents.first()
        selectedCalendarIds = settingsManager.selectedCalendarIds.first()
    }

    if (showCalendarPickerDialog) {
        AlertDialog(
            onDismissRequest = { showCalendarPickerDialog = false },
            title = { Text("Select Calendars") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (availableCalendars.isEmpty()) {
                        Text("No accounts found. Please check:\n1. Calendar sync is enabled in Phone Settings.\n2. Google Accounts are logged in.\n3. You selected 'Allow' on the permission prompt.")
                    } else {
                        availableCalendars.forEach { calendar ->
                            val isSelected = selectedCalendarIds.contains(calendar.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCalendarIds = if (isSelected) {
                                            selectedCalendarIds - calendar.id
                                        } else {
                                            selectedCalendarIds + calendar.id
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = isSelected, onCheckedChange = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(calendar.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(calendar.accountName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    loadCalendarEvents = selectedCalendarIds.isNotEmpty()
                    showCalendarPickerDialog = false 
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCalendarPickerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showScheduleWarning != null) {
        AlertDialog(
            onDismissRequest = { showScheduleWarning = null },
            title = { Text("Warning: Change Schedule?") },
            text = { Text("Switching schedule types will permanently delete all your day swap history. This cannot be undone. Do you want to continue?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        showScheduleWarning?.let { settingsManager.updateScheduleType(it) }
                        showScheduleWarning = null
                    }
                }) {
                    Text("Yes, Switch and Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showScheduleWarning = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About") },
            text = {
                Text(
                    "Simple ad free, ad tracking free, no data collected, app that helps you track on and off days. Lets you swap days and unswap days if needed. \n\n" +
                            "Life is hard already, let this ease some burden off."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Labels", style = MaterialTheme.typography.titleMedium)
        
        Column {
            OutlinedTextField(
                value = onDayText,
                onValueChange = { input ->
                    val filtered = input.replace("\n", "").take(20)
                    onDayText = filtered
                },
                label = { Text("On Day") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Work Day or 🏢") },
                singleLine = true
            )
            if (onDayText.length >= 20) {
                Text(
                    "Maximum character limit reached",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
        }

        Column {
            OutlinedTextField(
                value = offDayText,
                onValueChange = { input ->
                    val filtered = input.replace("\n", "").take(20)
                    offDayText = filtered
                },
                label = { Text("Off Day") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Off Day or 🏠") },
                singleLine = true
            )
            if (offDayText.length >= 20) {
                Text(
                    "Maximum character limit reached",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }
        }

        HorizontalDivider()

        Text("Colors", style = MaterialTheme.typography.titleMedium)
        
        ColorDropdown(
            label = "On Day Color",
            selectedHex = onDayColorHex,
            onColorSelected = { onDayColorHex = it }
        )

        ColorDropdown(
            label = "Off Day Color",
            selectedHex = offDayColorHex,
            onColorSelected = { offDayColorHex = it }
        )

        HorizontalDivider()

        Text("External Calendar", style = MaterialTheme.typography.titleMedium)
        
        val isCalendarPermitted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
        val buttonColor = if (isCalendarPermitted && loadCalendarEvents) Color(0xFF2E7D32) else Color(0xFFC62828)

        Button(
            onClick = {
                val hasCalendar = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                if (hasCalendar) {
                    availableCalendars = CalendarUtils.getAvailableCalendars(context)
                    showCalendarPickerDialog = true
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_CALENDAR, 
                            Manifest.permission.GET_ACCOUNTS,
                            Manifest.permission.READ_CONTACTS
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
        ) {
            val buttonText = if (isCalendarPermitted && loadCalendarEvents) {
                "Calendar Access: Allowed (${selectedCalendarIds.size} selected)"
            } else if (isCalendarPermitted) {
                "Calendar Access: Enabled (None selected)"
            } else {
                "Calendar Access: Denied (Tap to allow)"
            }
            Text(buttonText, color = Color.White)
        }
        
        Text(
            "Highlights days with saved events in orange from your selected calendars.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        Text("Select Schedule Type", style = MaterialTheme.typography.titleMedium)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilterChip(
                selected = currentScheduleType == "2-2-3",
                onClick = { if (currentScheduleType != "2-2-3") showScheduleWarning = "2-2-3" },
                label = { Text("2-2-3 Schedule") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = currentScheduleType == "5-2",
                onClick = { if (currentScheduleType != "5-2") showScheduleWarning = "5-2" },
                label = { Text("5-2 Schedule") },
                modifier = Modifier.weight(1f)
            )
        }

        HorizontalDivider()
        
        Button(
            onClick = {
                scope.launch {
                    settingsManager.updateWorkDayLabel(onDayText)
                    settingsManager.updateOffDayLabel(offDayText)
                    settingsManager.updateOnDayColor(onDayColorHex)
                    settingsManager.updateOffDayColor(offDayColorHex)
                    settingsManager.updateSelectedCalendarIds(selectedCalendarIds)
                    Toast.makeText(context, "Settings Saved", Toast.LENGTH_SHORT).show()
                    onSaveComplete()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Changes")
        }

        OutlinedButton(
            onClick = { showAboutDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("About")
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.calendar),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "Changes will be applied throughout the app after saving.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ColorDropdown(
    label: String,
    selectedHex: String,
    onColorSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(ColorPalette.fromHex(selectedHex))
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        ColorPalette.options.find { it.hex == selectedHex }?.name ?: "Custom",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 400.dp)
            ) {
                ColorPalette.options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(ColorPalette.fromHex(option.hex))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(option.name)
                            }
                        },
                        onClick = {
                            onColorSelected(option.hex)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
