package app.shotlist.ui.scan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import app.shotlist.data.Finding
import app.shotlist.data.Scan
import app.shotlist.data.Shot
import app.shotlist.data.ShotlistDb
import app.shotlist.engine.IngestWorker
import app.shotlist.engine.ScreenshotRow
import app.shotlist.ui.glass.GlassPanel
import dev.chrisbanes.haze.HazeState
import java.io.File
import kotlinx.coroutines.delay

private enum class CapturePhase {
    Ready,
    Capturing,
    Reading,
    Result,
    Error,
}

@Composable
fun ScanScreen(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val db = remember(context) { ShotlistDb.get(context) }
    val allFindings by db.findings().byTypes(findingTypes).collectAsState(initial = emptyList())
    var cameraGranted by remember { mutableStateOf(hasCameraPermission(context)) }
    var phase by remember { mutableStateOf(CapturePhase.Ready) }
    var captureMediaId by remember { mutableStateOf<Long?>(null) }
    var resultShot by remember { mutableStateOf<Shot?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> cameraGranted = granted }

    LaunchedEffect(captureMediaId) {
        val mediaId = captureMediaId ?: return@LaunchedEffect
        repeat(120) {
            val shot = db.shots().byMediaId(mediaId)
            if (shot != null && shot.status != "NEW") {
                db.scans().insert(Scan(shotId = shot.id, mode = "ANYTHING"))
                resultShot = shot
                phase = CapturePhase.Result
                return@LaunchedEffect
            }
            delay(250)
        }
        errorMessage = "That took longer than expected. Try once more."
        phase = CapturePhase.Error
    }

    val resultFindings = remember(resultShot, allFindings) {
        val shotId = resultShot?.id
        if (shotId == null) emptyList() else allFindings.filter { it.shotId == shotId }
    }

    if (!cameraGranted) {
        CameraPermissionCard(
            hazeState = hazeState,
            onRequest = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                permissionLauncher.launch(Manifest.permission.CAMERA)
            },
            modifier = modifier,
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(34.dp)),
    ) {
        CameraPreview(
            imageCapture = imageCapture,
            modifier = Modifier.fillMaxSize(),
            onFailure = {
                errorMessage = "Camera unavailable. Close another camera app and retry."
                phase = CapturePhase.Error
            },
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        0f to Color(0x99070B1B),
                        0.28f to Color.Transparent,
                        0.68f to Color.Transparent,
                        1f to Color(0xCC070B1B),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xB51A2138), RoundedCornerShape(999.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF7EF5D8),
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("ANYTHING", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Point. Shotlist finds the useful bit.",
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
            )
            Text(
                "Dates, menus, labels, codes — processed here.",
                color = Color.White.copy(alpha = 0.76f),
                fontSize = 14.sp,
            )
        }

        FocusCorners(
            color = when (phase) {
                CapturePhase.Result -> Color(0xFF7EF5D8)
                CapturePhase.Error -> Color(0xFFFF788F)
                else -> Color.White.copy(alpha = 0.78f)
            },
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(280.dp)
                .padding(horizontal = 28.dp),
        )

        if (phase == CapturePhase.Capturing || phase == CapturePhase.Reading) {
            ReadingOverlay(phase = phase, modifier = Modifier.fillMaxSize())
        }

        AnimatedVisibility(
            visible = phase == CapturePhase.Result || phase == CapturePhase.Error,
            enter = fadeIn(tween(220)) + slideInVertically(tween(360)) { it / 2 },
            exit = fadeOut(tween(160)) + slideOutVertically(tween(240)) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(14.dp),
        ) {
            ResultSheet(
                hazeState = hazeState,
                phase = phase,
                findings = resultFindings,
                shot = resultShot,
                errorMessage = errorMessage,
                onAgain = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    captureMediaId = null
                    resultShot = null
                    errorMessage = ""
                    phase = CapturePhase.Ready
                },
            )
        }

        AnimatedVisibility(
            visible = phase == CapturePhase.Ready,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp),
        ) {
            ShutterButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    phase = CapturePhase.Capturing
                    captureToPipeline(
                        context = context,
                        imageCapture = imageCapture,
                        onQueued = { mediaId ->
                            captureMediaId = mediaId
                            phase = CapturePhase.Reading
                        },
                        onError = { message ->
                            errorMessage = message
                            phase = CapturePhase.Error
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun CameraPreview(
    imageCapture: ImageCapture,
    onFailure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember(context) {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        }
    }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(context) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { provider = it }
                    .onFailure { onFailure() }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    DisposableEffect(provider, lifecycleOwner, previewView) {
        val cameraProvider = provider
        if (cameraProvider != null) {
            runCatching {
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                )
            }.onFailure { onFailure() }
        }
        onDispose { cameraProvider?.unbindAll() }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

private fun captureToPipeline(
    context: Context,
    imageCapture: ImageCapture,
    onQueued: (Long) -> Unit,
    onError: (String) -> Unit,
) {
    val mediaId = -System.currentTimeMillis()
    val directory = File(context.filesDir, "shared").apply { mkdirs() }
    val file = File(directory, "scan-${-mediaId}.jpg")
    val options = ImageCapture.OutputFileOptions.Builder(file).build()
    imageCapture.takePicture(
        options,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                IngestWorker.enqueue(
                    context,
                    ScreenshotRow(
                        mediaId = mediaId,
                        uri = Uri.fromFile(file),
                        takenAt = System.currentTimeMillis(),
                    ),
                )
                onQueued(mediaId)
            }

            override fun onError(exception: ImageCaptureException) {
                file.delete()
                onError("Couldn’t read that shot. Hold steady and try again.")
            }
        },
    )
}

@Composable
private fun CameraPermissionCard(
    hazeState: HazeState,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassPanel(
            hazeState = hazeState,
            cornerRadius = 34.dp,
            contentPadding = PaddingValues(20.dp),
            accent = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Outlined.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text("Point at the useful thing", fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text(
                "A menu, flyer, label, or code becomes a card. Camera frames stay on this phone.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Spacer(Modifier.height(18.dp))
            FilledTonalButton(onClick = onRequest, modifier = Modifier.fillMaxWidth()) {
                Text("Open camera", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "No upload. No account.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun ShutterButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(86.dp)
            .background(Color(0x4DFFFFFF), CircleShape)
            .border(2.dp, Color.White.copy(alpha = 0.92f), CircleShape)
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = "Scan", tint = Color(0xFF7C5CFF))
        }
    }
}

@Composable
private fun ReadingOverlay(phase: CapturePhase, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scan-shimmer")
    val glow by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(tween(680), RepeatMode.Reverse),
        label = "scan-glow",
    )
    Box(
        modifier = modifier.background(Color(0x88070B1B)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .background(Color(0xFF7C5CFF).copy(alpha = glow), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                if (phase == CapturePhase.Capturing) "Holding that thought…" else "Finding the useful bit…",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text("On-device OCR", color = Color(0xFF7EF5D8), fontSize = 13.sp)
        }
    }
}

@Composable
private fun ResultSheet(
    hazeState: HazeState,
    phase: CapturePhase,
    findings: List<Finding>,
    shot: Shot?,
    errorMessage: String,
    onAgain: () -> Unit,
) {
    val success = phase == CapturePhase.Result
    val accent = if (success) Color(0xFF7EF5D8) else Color(0xFFFF788F)
    GlassPanel(
        hazeState = hazeState,
        cornerRadius = 30.dp,
        contentPadding = PaddingValues(16.dp),
        accent = accent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (success) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(9.dp))
            Text(
                if (success) "Got it" else "One more try",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onAgain) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Scan another", tint = accent)
            }
        }
        if (success && findings.isNotEmpty()) {
            findings.take(2).forEach { finding ->
                Spacer(Modifier.height(8.dp))
                Text(finding.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    finding.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Saved to your Inbox", color = accent, style = MaterialTheme.typography.labelMedium)
        } else if (success) {
            Text(
                shot?.ocrText?.lineSequence()?.filter { it.isNotBlank() }?.take(3)?.joinToString(" · ")
                    ?.take(180)
                    ?.ifBlank { "Nothing actionable in that frame." }
                    ?: "Nothing actionable in that frame.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun FocusCorners(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val length = 30.dp.toPx()
        val stroke = 2.dp.toPx()
        val right = size.width
        val bottom = size.height
        drawLine(color, Offset(0f, 0f), Offset(length, 0f), stroke)
        drawLine(color, Offset(0f, 0f), Offset(0f, length), stroke)
        drawLine(color, Offset(right, 0f), Offset(right - length, 0f), stroke)
        drawLine(color, Offset(right, 0f), Offset(right, length), stroke)
        drawLine(color, Offset(0f, bottom), Offset(length, bottom), stroke)
        drawLine(color, Offset(0f, bottom), Offset(0f, bottom - length), stroke)
        drawLine(color, Offset(right, bottom), Offset(right - length, bottom), stroke)
        drawLine(color, Offset(right, bottom), Offset(right, bottom - length), stroke)
    }
}

private fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private val findingTypes = listOf(
    "EVENT", "DEADLINE", "PRODUCT", "PLACE", "CODE", "WIFI", "TRACKING", "RECIPE",
)
