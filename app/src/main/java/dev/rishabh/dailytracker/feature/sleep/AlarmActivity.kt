package dev.rishabh.dailytracker.feature.sleep

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rishabh.dailytracker.core.designsystem.DailyTrackerTheme
import dev.rishabh.dailytracker.core.designsystem.OnAccent
import dev.rishabh.dailytracker.core.designsystem.OnSurface
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceFaint
import dev.rishabh.dailytracker.core.designsystem.OnSurfaceVariant
import dev.rishabh.dailytracker.core.designsystem.Radius
import dev.rishabh.dailytracker.core.designsystem.ShapeFull
import dev.rishabh.dailytracker.core.designsystem.Spacing
import dev.rishabh.dailytracker.core.designsystem.Surface2
import dev.rishabh.dailytracker.core.designsystem.TypeNumeric
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlarmUiState(
    val problem: MathProblem,
    val input: String = "",
    val wrong: Boolean = false,
    val done: Boolean = false,
)

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val repository: SleepRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val sessionId: String? = savedStateHandle[SleepAlarmReceiver.EXTRA_SESSION_ID]
    private val _state = MutableStateFlow(AlarmUiState(problem = MathMission.next()))
    val state: StateFlow<AlarmUiState> = _state.asStateFlow()

    fun onInput(value: String) {
        _state.update { it.copy(input = value.filter(Char::isDigit).take(4), wrong = false) }
    }

    /** Correct → dismiss and record the wake; wrong → a fresh problem, never a way to escape. */
    fun onSubmit() {
        val current = _state.value
        if (current.input.toIntOrNull() == current.problem.answer) {
            _state.update { it.copy(done = true) }
            viewModelScope.launch { sessionId?.let { repository.dismissAlarm(it) } }
        } else {
            _state.update { it.copy(problem = MathMission.next(), input = "", wrong = true) }
        }
    }
}

/**
 * The full-screen wake mission, shown over the lockscreen while the alarm rings.
 *
 * It shows itself on a locked, dozing phone, ignores the volume and back keys, and offers no
 * dismiss but solving the maths problem — the deliberately-simple mission that guarantees the
 * alarm can always be silenced.
 */
@AndroidEntryPoint
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Back must not dismiss the alarm — only the mission can.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        setContent {
            DailyTrackerTheme {
                val viewModel: AlarmViewModel = hiltViewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()
                LaunchedEffect(state.done) {
                    if (state.done) {
                        SleepAlarmService.stop(this@AlarmActivity)
                        finish()
                    }
                }
                AlarmMissionScreen(state = state, onInput = viewModel::onInput, onSubmit = viewModel::onSubmit)
            }
        }
    }

    /** Volume and camera keys must not silence the alarm. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_VOLUME_MUTE,
        KeyEvent.KEYCODE_CAMERA,
        -> true
        else -> super.onKeyDown(keyCode, event)
    }
}

@Composable
private fun AlarmMissionScreen(
    state: AlarmUiState,
    onInput: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val accent = DailyTrackerTheme.accents.sleep
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(Spacing.sp6),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Wake up", style = MaterialTheme.typography.headlineMedium, color = OnSurface)
            Text(
                "Solve to dismiss",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.sp2, bottom = Spacing.sp8),
            )
            Text(
                "${state.problem.question} =",
                style = MaterialTheme.typography.displaySmall,
                color = OnSurface,
            )
            BasicTextField(
                value = state.input,
                onValueChange = onInput,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TypeNumeric.copy(color = OnSurface, textAlign = TextAlign.Center, fontSize = MaterialTheme.typography.headlineLarge.fontSize),
                cursorBrush = SolidColor(accent.base),
                modifier = Modifier
                    .padding(vertical = Spacing.sp6)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.md))
                    .background(Surface2)
                    .padding(vertical = Spacing.sp4),
            )
            if (state.wrong) {
                Text(
                    "Not quite — here's another",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceFaint,
                    modifier = Modifier.padding(bottom = Spacing.sp3),
                )
            }
            Box(
                modifier = Modifier
                    .clip(ShapeFull)
                    .background(accent.base)
                    .clickable(role = Role.Button, onClick = onSubmit)
                    .padding(horizontal = Spacing.sp8, vertical = Spacing.sp3),
            ) {
                Text("Dismiss", style = MaterialTheme.typography.labelLarge, color = OnAccent)
            }
        }
    }
}
