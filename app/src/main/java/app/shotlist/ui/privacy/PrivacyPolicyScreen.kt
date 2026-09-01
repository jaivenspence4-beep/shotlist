package app.shotlist.ui.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun PrivacyPolicyScreen(onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                        ) {
                            Text(
                                "Privacy policy",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                "Effective August 31, 2026",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                            )
                        }
                        IconButton(onClick = onClose) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close privacy policy")
                        }
                    }
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text(
                                "The short version",
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Shotlist processes screenshot contents on your device. " +
                                    "They are not sent to Shotlist servers, used for ads, or sold. " +
                                    "There is no Shotlist account.",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }

                item {
                    PolicySection(
                        title = "Photos you choose",
                        body = "Full photo access is optional. If you allow it, Shotlist looks for " +
                            "likely screenshots so it can create useful action cards. You can instead " +
                            "use Import screenshots or Android’s share menu; the system picker gives " +
                            "Shotlist access only to the images you select.",
                    )
                }
                item {
                    PolicySection(
                        title = "On-device processing and storage",
                        body = "Optical character recognition and classification run on your device. " +
                            "Shotlist stores screenshot references, recognized text, extracted findings, " +
                            "action states, and settings in private app storage. Picker and share imports " +
                            "may be copied there temporarily so processing can finish; temporary image " +
                            "copies are removed after processing.",
                    )
                }
                item {
                    PolicySection(
                        title = "Network and diagnostics",
                        body = "Shotlist does not request Android’s Internet permission and never sends " +
                            "screenshot images or recognized text to Shotlist servers. Shotlist uses " +
                            "Google’s on-device ML Kit SDK. Google documents that ML Kit may collect " +
                            "limited app and device information, performance metrics, and an installation " +
                            "identifier for diagnostics and usage analytics—not screenshot contents or " +
                            "recognized text.",
                    )
                }
                item {
                    PolicySection(
                        title = "Permissions and actions",
                        body = "Photo access supports automatic screenshot scanning. Camera access is used " +
                            "only when you open Scan. Notifications are optional. Calendar, maps, sharing, " +
                            "and clipboard actions happen only after you tap an action, and the app you " +
                            "choose then handles that information under its own privacy policy.",
                    )
                }
                item {
                    PolicySection(
                        title = "Sharing",
                        body = "Shotlist does not sell personal information or share screenshot contents " +
                            "with advertisers or data brokers. Information leaves Shotlist only when you " +
                            "choose an action that opens or shares to another app, apart from the limited " +
                            "ML Kit operational data described above.",
                    )
                }
                item {
                    PolicySection(
                        title = "Retention and deletion",
                        body = "Local results remain until you use Delete all my data, clear Shotlist’s " +
                            "storage in Android Settings, or uninstall the app. Android backup is disabled " +
                            "for Shotlist data. Deleting Shotlist data never deletes your phone’s original " +
                            "screenshots.",
                    )
                }
                item {
                    PolicySection(
                        title = "Children",
                        body = "Shotlist is not directed to children under 13. The app has no account and " +
                            "does not maintain server-side screenshot history.",
                    )
                }
                item {
                    PolicySection(
                        title = "Contact and changes",
                        body = "For privacy questions, use the developer support contact on Shotlist’s " +
                            "Google Play listing. Do not send screenshots, passwords, or access codes. " +
                            "This policy may change as Shotlist’s features, SDKs, or legal obligations " +
                            "change; the effective date above will be updated.",
                    )
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
        )
    }
}
