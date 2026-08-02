package dev.rishabh.dailytracker.core.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import dev.rishabh.dailytracker.core.designsystem.DailyTrackerTheme
import dev.rishabh.dailytracker.core.designsystem.OnSurface
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceVariant
import dev.rishabh.dailytracker.core.designsystem.ShapeFull
import dev.rishabh.dailytracker.core.designsystem.Spacing
import dev.rishabh.dailytracker.core.designsystem.Surface0
import dev.rishabh.dailytracker.core.designsystem.Surface2
import dev.rishabh.dailytracker.core.designsystem.component.AccentButton
import dev.rishabh.dailytracker.core.designsystem.component.OutlineButton
import java.io.File

/**
 * Full-screen front-of-pack photo capture.
 *
 * Self-contained: it asks for the camera permission itself, previews, and captures a JPEG
 * into a cache-dir temp file handed back through [onCaptured]. The file only becomes a
 * product photo when the caller adopts it — a cancel leaves nothing behind.
 */
@Composable
fun PhotoCaptureCamera(
    onCaptured: (File) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var asked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        asked = true
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Surface0) {
        if (granted) {
            CaptureContent(onCaptured = onCaptured, onCancel = onCancel)
        } else {
            Centered {
                Text("Camera access", style = MaterialTheme.typography.titleLarge, color = OnSurface)
                Text(
                    if (asked) {
                        "Camera permission was declined. The photo is optional — the product saves fine without one."
                    } else {
                        "The camera photographs the front of the pack, only while this screen is open. The photo stays on the device."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = Spacing.sp4),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sp3)) {
                    OutlineButton("Skip", onClick = onCancel)
                    AccentButton(
                        if (asked) "Try again" else "Allow camera",
                        accent = DailyTrackerTheme.accent.base,
                    ) { launcher.launch(Manifest.permission.CAMERA) }
                }
            }
        }
    }
}

@Composable
private fun CaptureContent(onCaptured: (File) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCaptured by rememberUpdatedState(onCaptured)

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    // A failed capture keeps the camera open and says so; the temp file is not kept.
    var captureFailed by remember { mutableStateOf(false) }

    DisposableEffect(previewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null

        providerFuture.addListener({
            provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            provider?.unbindAll()
            provider?.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture,
            )
        }, ContextCompat.getMainExecutor(context))

        onDispose { provider?.unbindAll() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        if (captureFailed) {
            Text(
                "Capture failed — try again",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurface,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(Spacing.sp6)
                    .clip(ShapeFull)
                    .background(Surface2)
                    .padding(horizontal = Spacing.sp4, vertical = Spacing.sp2),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Surface0.copy(alpha = 0.85f))
                .padding(Spacing.sp4),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Cancel",
                style = MaterialTheme.typography.labelLarge,
                color = OnSurfaceVariant,
                modifier = Modifier
                    .clip(ShapeFull)
                    .clickable(role = Role.Button, onClick = onCancel)
                    .padding(horizontal = Spacing.sp4, vertical = Spacing.sp2),
            )
            // Shutter
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .border(3.dp, DailyTrackerTheme.accent.base, CircleShape)
                    .clickable(role = Role.Button) {
                        val photoFile = File.createTempFile("product_front_", ".jpg", context.cacheDir)
                        imageCapture.takePicture(
                            ImageCapture.OutputFileOptions.Builder(photoFile).build(),
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    currentOnCaptured(photoFile)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    photoFile.delete()
                                    captureFailed = true
                                }
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DailyTrackerTheme.accent.base),
                )
            }
            // Balances the Cancel label so the shutter sits centred.
            Box(modifier = Modifier.size(64.dp))
        }
    }
}

/** Centred column for the permission rationale. */
@Composable
private fun Centered(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.sp6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}
