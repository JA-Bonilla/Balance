package com.jabonilla.balance

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private var reduceMotion by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(AndroidColor.TRANSPARENT), SystemBarStyle.dark(AndroidColor.TRANSPARENT))
        val store = (application as BalanceApplication).store
        TimerSound.createChannels(this)
        setContent {
            BalanceTheme {
                CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
                    var settings by remember { mutableStateOf(false) }
                    if (store.data.goals.isEmpty()) OnboardingScreen(store)
                    else if (settings) SettingsScreen(store) { settings = false }
                    else HomeScreen(store) { settings = true }

                    store.error?.let { message ->
                        AlertDialog(
                            onDismissRequest = { store.error = null },
                            title = { Text("Balance") },
                            text = { Text(message) },
                            confirmButton = { TextButton({ store.error = null }) { Text("OK") } },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Same as iOS scenePhase == .active.
        (application as BalanceApplication).store.finishTimerIfDue()
        reduceMotion = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
