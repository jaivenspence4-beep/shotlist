package app.shotlist.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Scaffold-phase shell: four destinations, plain Material bottom bar.
 * Codex (t4) replaces this with the liquid-glass design system —
 * translucent haze surfaces, depth, spring transitions.
 */
private enum class Tab(val label: String, val icon: ImageVector) {
    Inbox("Inbox", Icons.Outlined.Inbox),
    Scan("Scan", Icons.Outlined.CameraAlt),
    Track("Track", Icons.Outlined.Favorite),
    You("You", Icons.Outlined.Person),
}

@Composable
fun AppShell() {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = when (Tab.entries[selected]) {
                    Tab.Inbox -> "Inbox — your screenshots become things that happen"
                    Tab.Scan -> "Scan — point at anything"
                    Tab.Track -> "Track — cycles, habits, streaks"
                    Tab.You -> "You — vault & privacy: 0 bytes have left this phone"
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
