package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.CadenceApplication
import com.example.background.NotificationHelper
import com.example.background.PomodoroService
import com.example.background.WaterReminderScheduler
import com.example.model.CadenceState
import com.example.model.PomodoroPhase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CadenceUiState(
  val state: CadenceState = CadenceState(),
  val displayedRemainingMillis: Long = CadenceState.DEFAULT_FOCUS_MINUTES * 60_000L,
  val waterRemainingMillis: Long = 0L,
  val hasNotificationPermission: Boolean = true,
) {
  val progressFraction: Float
    get() {
      val total = state.totalPhaseMillis.coerceAtLeast(1L)
      return (displayedRemainingMillis.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }
}

class CadenceViewModel(application: Application) : AndroidViewModel(application) {

  private val app = application as CadenceApplication
  private val repository = app.repository
  private val permissionGrantedFlow =
    MutableStateFlow(NotificationHelper.hasNotificationPermission(application))

  private val clockFlow = flow {
    while (true) {
      emit(System.currentTimeMillis())
      delay(250L)
    }
  }

  val uiState: StateFlow<CadenceUiState> =
    combine(repository.stateFlow, clockFlow, permissionGrantedFlow) {
      cadenceState,
      nowMillis,
      hasPermission ->
      val remainingPomodoro =
        if (cadenceState.isRunning) {
          (cadenceState.endTimeWallMillis - nowMillis).coerceAtLeast(0L)
        } else {
          cadenceState.remainingMillisWhenPaused
        }

      val waterRemaining =
        if (cadenceState.waterEnabled && cadenceState.nextWaterWallMillis > 0L) {
          (cadenceState.nextWaterWallMillis - nowMillis).coerceAtLeast(0L)
        } else {
          0L
        }

      CadenceUiState(
        state = cadenceState,
        displayedRemainingMillis = remainingPomodoro,
        waterRemainingMillis = waterRemaining,
        hasNotificationPermission = hasPermission,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000L),
      initialValue =
        CadenceUiState(hasNotificationPermission = NotificationHelper.hasNotificationPermission(app)),
    )

  fun refreshNotificationPermission() {
    permissionGrantedFlow.value = NotificationHelper.hasNotificationPermission(app)
  }

  fun toggleStartPausePomodoro() {
    val current = uiState.value.state
    if (current.isRunning) {
      PomodoroService.pauseTimer(app)
    } else {
      NotificationHelper.cancelNotification(app, NotificationHelper.NOTIF_ID_POMODORO_ALERT)
      PomodoroService.startTimer(app)
    }
  }

  fun resetPomodoro() {
    val current = uiState.value.state
    if (current.isRunning) {
      PomodoroService.resetTimer(app)
    } else {
      viewModelScope.launch { repository.resetTimer(current.phase) }
    }
  }

  fun skipPhase() {
    val current = uiState.value.state
    if (current.isRunning) {
      PomodoroService.resetTimer(app)
    }
    viewModelScope.launch { repository.advancePhaseAfterCompletion(countAsCompletedFocus = false) }
  }

  fun selectPhase(phase: PomodoroPhase) {
    val current = uiState.value.state
    if (current.phase == phase) return
    if (current.isRunning) {
      PomodoroService.resetTimer(app)
    }
    viewModelScope.launch { repository.resetTimer(phase) }
  }

  fun updatePomodoroDurations(focusMinutes: Int, shortBreakMinutes: Int, longBreakMinutes: Int) {
    viewModelScope.launch {
      repository.updatePomodoroDurations(focusMinutes, shortBreakMinutes, longBreakMinutes)
    }
  }

  fun toggleWaterReminder(enabled: Boolean) {
    viewModelScope.launch {
      if (enabled) {
        val interval = uiState.value.state.waterIntervalMinutes
        val nextTrigger = WaterReminderScheduler.scheduleNextWaterReminder(app, interval)
        repository.setWaterReminder(enabled = true, nextTriggerWallMillis = nextTrigger)
      } else {
        WaterReminderScheduler.cancelWaterReminder(app)
        repository.setWaterReminder(enabled = false, nextTriggerWallMillis = 0L)
      }
    }
  }

  fun setWaterInterval(intervalMinutes: Int) {
    val clamped = intervalMinutes.coerceIn(5, 180)
    viewModelScope.launch {
      val nextTrigger =
        if (uiState.value.state.waterEnabled) {
          WaterReminderScheduler.scheduleNextWaterReminder(app, clamped)
        } else {
          null
        }
      repository.setWaterInterval(clamped, nextTrigger)
    }
  }

  fun logWaterGlass() {
    viewModelScope.launch {
      repository.incrementWaterGlass()
      NotificationHelper.cancelNotification(app, NotificationHelper.NOTIF_ID_WATER_REMINDER)
    }
  }

  fun sendTestWaterNotification() {
    NotificationHelper.createChannels(app)
    NotificationHelper.showWaterReminderNotification(app)
  }
}
