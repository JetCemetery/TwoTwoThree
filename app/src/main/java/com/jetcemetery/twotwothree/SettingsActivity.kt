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

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier, 
    settingsManager: SettingsManager,
    onSaveComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Initial values from DataStore
    val savedWorkDayLabel by settingsManager.workDayLabel.collectAsState(initial = "Work Day")
    val savedOffDayLabel by settingsManager.offDayLabel.collectAsState(initial = "Off Day")

    // Local state to prevent lag and cursor jumping
    var onDayText by remember { mutableStateOf("") }
    var offDayText by remember { mutableStateOf("") }

    // Initialize local state when DataStore emits for the first time
    LaunchedEffect(savedWorkDayLabel, savedOffDayLabel) {
        if (onDayText.isEmpty()) onDayText = savedWorkDayLabel
        if (offDayText.isEmpty()) offDayText = savedOffDayLabel
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

        Text(
            text = "Changes will be applied throughout the app after saving.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
