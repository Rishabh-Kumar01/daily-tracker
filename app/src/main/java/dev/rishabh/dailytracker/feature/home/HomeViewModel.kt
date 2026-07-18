package dev.rishabh.dailytracker.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.rishabh.dailytracker.feature.activities.ActivityRepository
import dev.rishabh.dailytracker.feature.activities.HomeActivity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Immutable Home state: the activity list, plus whether the first load has arrived. */
data class HomeUiState(
    val activities: List<HomeActivity> = emptyList(),
    val loading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: ActivityRepository,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = repository.observeHome()
        .map { HomeUiState(activities = it, loading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = HomeUiState(loading = true),
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
