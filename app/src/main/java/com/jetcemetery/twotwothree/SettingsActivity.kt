package com.jetcemetery.twotwothree

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
                        settingsManager = settingsManager
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, settingsManager: SettingsManager) {
    val scope = rememberCoroutineScope()
    val workDayLabel by settingsManager.workDayLabel.collectAsState(initial = "Work Day")
    val offDayLabel by settingsManager.offDayLabel.collectAsState(initial = "Off Day")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = workDayLabel,
            onValueChange = { scope.launch { settingsManager.updateWorkDayLabel(it) } },
            label = { Text("Work Day Label") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Work Day or 🏢") }
        )

        OutlinedTextField(
            value = offDayLabel,
            onValueChange = { scope.launch { settingsManager.updateOffDayLabel(it) } },
            label = { Text("Off Day Label") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Off Day or 🏠") }
        )
        
        Text(
            text = "Changes are saved automatically and applied throughout the app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
