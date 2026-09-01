package app.shotlist

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import app.shotlist.actions.ShotlistActions
import app.shotlist.engine.IngestWorker
import app.shotlist.ui.shell.AppShell
import app.shotlist.ui.theme.ShotlistTheme

// FragmentActivity (a ComponentActivity subclass) is required by
// BiometricPrompt for the vault; Compose setContent works unchanged.
class MainActivity : FragmentActivity() {
    var deepLinkFindingId by mutableStateOf<Long?>(null)
        private set
    var deepLinkSerial by mutableIntStateOf(0)
        private set
    var targetTab by mutableStateOf<String?>(null)
        private set
    var openVaultRequested by mutableStateOf(false)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleShare(intent)
        handleDeepLink(intent)
        setContent {
            ShotlistTheme {
                AppShell()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShare(intent)
        handleDeepLink(intent)
    }

    /** Store-compliant fallback ingest: user shares any image into Shotlist. */
    private fun handleShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        if (intent.type?.startsWith("image/") != true) return
        val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        uri?.let { IngestWorker.enqueueShared(applicationContext, it) }
    }

    private fun handleDeepLink(intent: Intent?) {
        val findingId = intent
            ?.getLongExtra(ShotlistActions.EXTRA_FINDING_ID, -1L)
            ?.takeIf { it >= 0 }
        val requestedTab = intent?.getStringExtra(EXTRA_TARGET_TAB)
        val requestedVault = intent?.getBooleanExtra(EXTRA_OPEN_VAULT, false) == true
        if (findingId == null && requestedTab == null && !requestedVault) return
        deepLinkFindingId = findingId
        targetTab = requestedTab ?: findingId?.let { "inbox" }
        openVaultRequested = requestedVault
        deepLinkSerial += 1
    }

    companion object {
        const val EXTRA_TARGET_TAB = "targetTab"
        const val EXTRA_OPEN_VAULT = "openVault"
    }
}
