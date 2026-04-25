package com.jetcemetery.twotwothree

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    
    // Initial values from DataStore
    val currentScheduleType by settingsManager.scheduleType.collectAsState(initial = "2-2-3")

    // Local state to prevent lag and cursor jumping
    var onDayText by remember { mutableStateOf("") }
    var offDayText by remember { mutableStateOf("") }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showScheduleWarning by remember { mutableStateOf<String?>(null) }

    // Initialize local state with current values from DataStore once
    LaunchedEffect(Unit) {
        onDayText = settingsManager.workDayLabel.first()
        offDayText = settingsManager.offDayLabel.first()
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = onDayText,
            onValueChange = { onDayText = it },
            label = { Text("On Day") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Work Day or 🏢") }
        )

        OutlinedTextField(
            value = offDayText,
            onValueChange = { offDayText = it },
            label = { Text("Off Day") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Off Day or 🏠") }
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

        Text(
            text = "Changes will be applied throughout the app after saving.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
