package dev.rishabh.dailytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import dev.rishabh.dailytracker.core.designsystem.DailyTrackerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyTrackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { insets ->
                    Box(
                        modifier = Modifier.fillMaxSize().padding(insets),
                        contentAlignment = Alignment.Center,
                    ) {
                        Placeholder()
                    }
                }
            }
        }
    }
}

@Composable
private fun Placeholder() {
    // Replaced by the Home screen in M5.
    Text(text = "Daily Tracker")
}
