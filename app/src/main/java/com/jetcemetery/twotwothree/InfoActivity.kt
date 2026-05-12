package com.jetcemetery.twotwothree

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetcemetery.twotwothree.ui.theme.TwoTwoThreeTheme

class InfoActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TwoTwoThreeTheme {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Information") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    InfoScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun InfoScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        InfoSection(title = "Indicators") {
            Text("The green line (or ring in landscape) at the bottom/around a date indicates today's date, helping you quickly find where you are in the month.")
        }

        InfoSection(title = "Switching Days") {
            Text("Long-pressing on any date will open a confirmation dialog. If you confirm, the schedule will 'switch' (invert work/off days) starting from that day forward, allowing you to easily manage swaps or schedule changes.")
        }

        InfoSection(title = "Schedule Systems") {
            ScheduleDescription(
                name = "2-2-3 Schedule",
                description = "This system rotates through 2 days on, 2 days off, and 3 days on. In Week A, you work Monday, Tuesday, Friday, Saturday, and Sunday; in Week B, you only work Wednesday and Thursday."
            )
            
            ScheduleDescription(
                name = "5-2 Schedule",
                description = "This is a traditional fixed rotation. In Week A, you work a standard Monday through Friday shift and have the weekend off, while in Week B, the pattern continues similarly."
            )
            
            ScheduleDescription(
                name = "2-2-5-5 Schedule",
                description = "A longer rotation pattern involving 2 days on, 2 days off, followed by 5 days on and 5 days off. This creates a predictable cycle where long stretches of work are followed by equally long rest periods across the two weeks."
            )
        }
    }
}

@Composable
fun InfoSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun ScheduleDescription(name: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(text = description, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(8.dp))
    }
}
