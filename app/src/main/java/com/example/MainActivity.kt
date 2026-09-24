package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.data.cloud.worker.BackendSyncWorker
import com.example.data.cloud.worker.FirestoreSyncWorker
import com.example.ui.MainAppScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Schedule periodic background cloud sync via WorkManager
    try {
      FirestoreSyncWorker.schedulePeriodicSync(applicationContext)
      BackendSyncWorker.schedulePeriodicSync(applicationContext)
    } catch (_: Throwable) {}

    setContent {
      MainAppScreen()
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
