package dev.rishabh.dailytracker.feature.diet.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.rishabh.dailytracker.core.designsystem.ActivityKey
import dev.rishabh.dailytracker.core.designsystem.DailyTrackerTheme
import dev.rishabh.dailytracker.core.designsystem.OnSurface
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceFaint
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceVariant
import dev.rishabh.dailytracker.core.designsystem.Outline
import dev.rishabh.dailytracker.core.designsystem.ProvideActivityAccent
import dev.rishabh.dailytracker.core.designsystem.Radius
import dev.rishabh.dailytracker.core.designsystem.Spacing
import dev.rishabh.dailytracker.core.designsystem.Surface0
import dev.rishabh.dailytracker.core.designsystem.Surface2
import dev.rishabh.dailytracker.core.designsystem.component.AccentButton
import dev.rishabh.dailytracker.core.designsystem.component.BackTopBar
import dev.rishabh.dailytracker.core.designsystem.component.ConfirmField
import dev.rishabh.dailytracker.core.designsystem.component.ConfirmSheet
import dev.rishabh.dailytracker.core.designsystem.component.OutlineButton
import dev.rishabh.dailytracker.feature.diet.ManualProductInput

/**
 * Tier-1 nutrition capture: scan a barcode, look it up, confirm it, save it.
 *
 * The lookup only ever proposes. Whether Open Food Facts had the product, had it without
 * numbers, or had nothing at all, the same editable confirmation sheet stands between the
 * result and the database — and a miss keeps the barcode so the product is still
 * identifiable next time.
 */
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onLogExisting: (productId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val savedProductId by viewModel.savedProductId.collectAsStateWithLifecycle()

    // Saving is the end of this screen's job; the meal screen shows the new brand.
    LaunchedEffect(savedProductId) {
        if (savedProductId != null) onBack()
    }

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

    Surface(modifier = modifier.fillMaxSize(), color = Surface0) {
        ProvideActivityAccent(ActivityKey.DIET) {
            Column(modifier = Modifier.fillMaxSize()) {
                BackTopBar(title = "Scan barcode", onBack = onBack)
                Box(modifier = Modifier.fillMaxSize().imePadding()) {
                    when {
                        // Only the camera step needs permission. Once the user has chosen to
                        // type the label instead, the rationale must get out of the way —
                        // otherwise "Enter manually" appears to do nothing.
                        !granted && state is ScanState.Scanning -> CameraRationale(
                            denied = asked,
                            onGrant = { launcher.launch(Manifest.permission.CAMERA) },
                            onEnterManually = { viewModel.onEnterManually("") },
                        )

                        else -> ScanContent(
                            state = state,
                            onBarcode = viewModel::onBarcodeDetected,
                            onRetry = viewModel::onRetry,
                            onEnterManually = viewModel::onEnterManually,
                            onRescan = viewModel::onRescan,
                            onFieldChange = viewModel::onFieldChange,
                            onSave = viewModel::onSave,
                            onAdjustExisting = viewModel::onAdjustExisting,
                            onLogExisting = onLogExisting,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanContent(
    state: ScanState,
    onBarcode: (String) -> Unit,
    onRetry: (String) -> Unit,
    onEnterManually: (String) -> Unit,
    onRescan: () -> Unit,
    onFieldChange: (Int, String) -> Unit,
    onSave: () -> Unit,
    onAdjustExisting: () -> Unit,
    onLogExisting: (String) -> Unit,
) {
    when (state) {
        ScanState.Scanning -> Box(Modifier.fillMaxSize()) {
            BarcodeCamera(onBarcode = onBarcode, modifier = Modifier.fillMaxSize())
            ScanReticle(modifier = Modifier.align(Alignment.Center))
            Hint(
                "Point the camera at the barcode",
                modifier = Modifier.align(Alignment.BottomCenter).padding(Spacing.sp6),
            )
        }

        is ScanState.LookingUp -> Centered {
            CircularProgressIndicator(color = DailyTrackerTheme.accent.base)
            Text(
                "Looking up ${state.barcode}",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.sp4),
            )
        }

        is ScanState.LookupFailed -> Centered {
            Text(
                if (state.offline) "No connection" else "Lookup failed",
                style = MaterialTheme.typography.titleLarge,
                color = OnSurface,
            )
            Text(
                if (state.offline) {
                    "Open Food Facts needs a connection. You can still enter the label yourself — the barcode is kept either way."
                } else {
                    "Open Food Facts could not be reached. Try again, or enter the label yourself."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = Spacing.sp4),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sp3)) {
                OutlineButton("Retry") { onRetry(state.barcode) }
                AccentButton("Enter manually", accent = DailyTrackerTheme.accent.base) {
                    onEnterManually(state.barcode)
                }
            }
        }

        is ScanState.AlreadySaved -> Centered {
            Text("Already in your foods", style = MaterialTheme.typography.titleLarge, color = OnSurface)
            Text(
                listOfNotNull(state.product.brand, state.product.productName).joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.sp2),
            )
            Text(
                state.product.per100gLine,
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.sp1, bottom = Spacing.sp4),
            )
            AccentButton("Log it now", accent = DailyTrackerTheme.accent.base) {
                onLogExisting(state.product.productId)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sp3),
                modifier = Modifier.padding(top = Spacing.sp3),
            ) {
                OutlineButton("Adjust", onClick = onAdjustExisting)
                OutlineButton("Rescan", onClick = onRescan)
            }
        }

        is ScanState.Confirm -> Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            if (state.notice != null) {
                Notice(state.notice)
            }
            if (state.barcode.isNotEmpty()) {
                Notice("Barcode ${state.barcode}", faint = true)
            }
            if (state.error != null) {
                Notice(state.error, accent = true)
            }
            ConfirmSheet(
                title = "Confirm product",
                fields = ManualProductInput.LABELS.mapIndexed { index, label ->
                    ConfirmField(label = label, value = state.input.fieldAt(index), suffix = suffixFor(label))
                },
                accent = DailyTrackerTheme.accent,
                confirmLabel = "Save product",
                cancelLabel = "Rescan",
                onFieldChange = onFieldChange,
                onConfirm = onSave,
                onCancel = onRescan,
            )
        }
    }
}

@Composable
private fun CameraRationale(denied: Boolean, onGrant: () -> Unit, onEnterManually: () -> Unit) {
    Centered {
        Text("Camera access", style = MaterialTheme.typography.titleLarge, color = OnSurface)
        Text(
            if (denied) {
                "Camera permission was declined. Scanning needs it, but you can still add the product by typing its label."
            } else {
                "Scanning reads the barcode on the packet. The camera is used only while this screen is open, and no image is stored."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = Spacing.sp4),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sp3)) {
            OutlineButton("Enter manually", onClick = onEnterManually)
            AccentButton(
                if (denied) "Try again" else "Allow camera",
                accent = DailyTrackerTheme.accent.base,
                onClick = onGrant,
            )
        }
    }
}

@Composable
private fun ScanReticle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth(0.8f)
            .height(180.dp)
            .border(2.dp, DailyTrackerTheme.accent.base, RoundedCornerShape(Radius.lg)),
    )
}

@Composable
private fun Hint(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = OnSurface,
        modifier = modifier
            .clip(RoundedCornerShape(Radius.md))
            .background(Surface2)
            .padding(horizontal = Spacing.sp4, vertical = Spacing.sp2),
    )
}

@Composable
private fun Notice(text: String, faint: Boolean = false, accent: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = when {
            accent -> DailyTrackerTheme.accent.base
            faint -> OnSurfaceFaint
            else -> OnSurfaceVariant
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sp4, vertical = Spacing.sp1),
    )
}

/** Full-screen centred column, shared by the permission, loading and failure states. */
@Composable
private fun Centered(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.sp6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content,
    )
}

private fun suffixFor(label: String): String? = when (label) {
    "kcal" -> "/100g"
    "Protein", "Carbs", "Fat" -> "g/100g"
    else -> null
}

@Preview(name = "Scan confirm", showBackground = true, backgroundColor = 0xFF0E1013, widthDp = 412, heightDp = 860)
@Composable
private fun ScanConfirmPreview() {
    DailyTrackerTheme {
        Surface(color = Surface0) {
            ScanContent(
                state = ScanState.Confirm(
                    barcode = "8901262010207",
                    input = ManualProductInput("Amul", "Butter School Pack", "724", "1", "0", "80"),
                    source = dev.rishabh.dailytracker.core.db.ProductSource.OFF,
                    notice = "From Open Food Facts — check it against the label",
                ),
                onBarcode = {}, onRetry = {}, onEnterManually = {}, onRescan = {},
                onFieldChange = { _, _ -> }, onSave = {},
                onAdjustExisting = {}, onLogExisting = {},
            )
        }
    }
}
