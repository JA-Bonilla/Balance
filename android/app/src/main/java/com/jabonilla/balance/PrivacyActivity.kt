package com.jabonilla.balance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Health Connect's required privacy rationale screen. */
class PrivacyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BalanceTheme {
                Column(Modifier.safeDrawingPadding().padding(24.dp)) {
                    Text(
                        "Balance reads exercise sessions and mindfulness sessions from the last 28 days, counts them as growth minutes, " +
                            "stores them only in balance.json on this phone, and sends nothing anywhere.",
                    )
                }
            }
        }
    }
}
