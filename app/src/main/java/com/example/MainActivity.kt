package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.data.cloud.worker.BackendSyncWorker
import com.example.ui.MainAppScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val notificationDestinations = kotlinx.coroutines.flow.MutableSharedFlow<String>(replay = 1)
  private fun acceptDestination(intent: android.content.Intent?) {
    intent?.getStringExtra("destination_tab")?.takeIf { it == "TASKS" }?.let { notificationDestinations.tryEmit(it) }
  }
  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    acceptDestination(intent)
  }
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    acceptDestination(intent)
    enableEdgeToEdge()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      window.isNavigationBarContrastEnforced = false
    }
    
    // Schedule periodic background cloud sync via WorkManager
    try {
      BackendSyncWorker.schedulePeriodicSync(applicationContext)
    } catch (_: Throwable) {}

    setContent {
      MainAppScreen(notificationDestinations = notificationDestinations)
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme { Greeting("Android") }
}
