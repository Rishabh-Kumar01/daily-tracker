package dev.rishabh.dailytracker.feature.sleep

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rishabh.dailytracker.core.db.entity.SleepSessionEntity
import dev.rishabh.dailytracker.core.designsystem.AccentColors
import dev.rishabh.dailytracker.core.designsystem.ActivityKey
import dev.rishabh.dailytracker.core.designsystem.DailyTrackerTheme
import dev.rishabh.dailytracker.core.designsystem.OnAccent
import dev.rishabh.dailytracker.core.designsystem.OnSurface
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceFaint
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceVariant
import dev.rishabh.dailytracker.core.designsystem.ProvideActivityAccent
import dev.rishabh.dailytracker.core.designsystem.Radius
import dev.rishabh.dailytracker.core.designsystem.ShapeFull
import dev.rishabh.dailytracker.core.designsystem.Spacing
import dev.rishabh.dailytracker.core.designsystem.Surface1
import dev.rishabh.dailytracker.core.designsystem.Surface2
import dev.rishabh.dailytracker.core.designsystem.component.AccentButton
import dev.rishabh.dailytracker.core.designsystem.component.BackTopBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class SleepUiState(
    val pending: SleepSessionEntity? = null,
    val targetHours: Double = 7.5,
)

@HiltViewModel
class SleepViewModel @Inject constructor(
    private val repository: SleepRepository,
) : ViewModel() {

    private val target = MutableStateFlow(7.5)

    val state: StateFlow<SleepUiState> =
        combine(repository.observePending(), target) { pending, hours ->
            SleepUiState(pending = pending, targetHours = hours)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SleepUiState())

    fun onTargetChange(hours: Double) { target.value = hours }
    fun onConfirm() { viewModelScope.launch { repository.confirmBedtime(target.value) } }
    fun onCancel() { viewModelScope.launch { repository.cancelPending() } }

    /** Schedules an alarm ~45s out so the full mission can be tried without waiting all night. */
    fun onTest() { viewModelScope.launch { repository.confirmBedtime(TEST_SECONDS / 3600.0) } }

    private companion object {
        const val TEST_SECONDS = 45.0
    }
}

@Composable
fun SleepScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SleepViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // The full-screen alarm shows regardless, but the ongoing notification needs this on 13+.
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val ensureNotifications: () -> Unit = {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    SleepContent(
        state = state,
        onBack = onBack,
        onTargetChange = viewModel::onTargetChange,
        onConfirm = { ensureNotifications(); viewModel.onConfirm() },
        onCancel = viewModel::onCancel,
        onTest = { ensureNotifications(); viewModel.onTest() },
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SleepContent(
    state: SleepUiState,
    onBack: () -> Unit,
    onTargetChange: (Double) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        ProvideActivityAccent(ActivityKey.SLEEP) {
            val accent = DailyTrackerTheme.accent
            Column {
                BackTopBar(title = "Wake alarm", onBack = onBack)
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = Spacing.screenGutter),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sp4),
                ) {
                    val pending = state.pending
                    if (pending != null) {
                        AlarmSetCard(pending = pending, accent = accent, onCancel = onCancel)
                    } else {
                        TargetPicker(state.targetHours, accent, onTargetChange)
                        Text(
                            "Wake at about ${clock(System.currentTimeMillis() + (state.targetHours * 3_600_000).toLong())}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceVariant,
                        )
                        AccentButton("Confirm bedtime", accent = accent.base, onClick = onConfirm)
                    }

                    Text(
                        "Test wake alarm (~45s)",
                        style = MaterialTheme.typography.labelLarge,
                        color = OnSurfaceFaint,
                        modifier = Modifier
                            .padding(top = Spacing.sp2)
                            .clip(RoundedCornerShape(Radius.md))
                            .clickable(role = Role.Button, onClick = onTest)
                            .padding(horizontal = Spacing.sp3, vertical = Spacing.sp3),
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmSetCard(pending: SleepSessionEntity, accent: AccentColors, onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.lg))
            .background(Surface1)
            .padding(Spacing.sp5),
        verticalArrangement = Arrangement.spacedBy(Spacing.sp2),
    ) {
        Text("Alarm set".uppercase(), style = MaterialTheme.typography.labelSmall, color = OnSurfaceFaint)
        Text(clock(pending.computedWakeAt), style = MaterialTheme.typography.displaySmall, color = OnSurface)
        Text(
            "In ${hoursMinutes(pending.computedWakeAt - System.currentTimeMillis())} · solve a maths problem to dismiss",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
        )
        Text(
            "Cancel alarm",
            style = MaterialTheme.typography.labelLarge,
            color = accent.base,
            modifier = Modifier
                .padding(top = Spacing.sp2)
                .clip(RoundedCornerShape(Radius.md))
                .clickable(role = Role.Button, onClick = onCancel)
                .padding(horizontal = Spacing.sp3, vertical = Spacing.sp2),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TargetPicker(selected: Double, accent: AccentColors, onChange: (Double) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sp2)) {
        Text("How long?".uppercase(), style = MaterialTheme.typography.labelSmall, color = OnSurfaceFaint)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sp2), verticalArrangement = Arrangement.spacedBy(Spacing.sp2)) {
            listOf(6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 9.0).forEach { hours ->
                val on = hours == selected
                Text(
                    if (hours % 1.0 == 0.0) "${hours.toInt()}h" else "${hours}h",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) accent.base else OnSurfaceVariant,
                    modifier = Modifier
                        .clip(ShapeFull)
                        .background(if (on) accent.container else Surface2)
                        .then(if (on) Modifier.border(1.5.dp, accent.base, ShapeFull) else Modifier)
                        .clickable(role = Role.RadioButton) { onChange(hours) }
                        .padding(horizontal = Spacing.sp4, vertical = Spacing.sp2),
                )
            }
        }
    }
}

private fun clock(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))

private fun hoursMinutes(millis: Long): String {
    val totalMinutes = (millis / 60_000).coerceAtLeast(0)
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
