package app.shotlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.shotlist.ui.shell.AppShell
import app.shotlist.ui.theme.ShotlistTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShotlistTheme {
                AppShell()
            }
        }
    }
}
