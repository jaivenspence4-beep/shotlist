package app.shotlist.health.android

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import app.shotlist.ui.theme.ShotlistTheme

/**
 * Exported only because Health Connect requires a privacy-rationale entry point.
 *
 * This activity deliberately ignores its Intent, data URI, extras, and caller.
 * It reads no database state and performs no navigation or privileged action.
 */
class HealthConnectRationaleActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            ShotlistTheme {
                HealthConnectPrivacyRationale()
            }
        }
    }
}

@Composable
private fun HealthConnectPrivacyRationale() {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 48.dp),
        ) {
            Text(
                text = "Your glucose story stays private",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "If you connect Metabolic Lens, Shotlist reads glucose readings from Health Connect only while you use the private tracker.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "The readings stay on this phone. Shotlist does not write health data, upload it, use it for ads, or include it in memories, notifications, widgets, or sharing.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "Lingo data can arrive about three hours later. Metabolic Lens is not live and is not medical advice.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
    }
}
