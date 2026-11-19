package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CadenceState
import com.example.model.PomodoroPhase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.cadenceDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "cadence_preferences"
)

class CadenceRepository(private val context: Context) {

  private object Keys {
    val PHASE = stringPreferencesKey("pomodoro_phase")
    val IS_RUNNING = booleanPreferencesKey("pomodoro_is_running")
    val END_TIME_WALL = longPreferencesKey("pomodoro_end_time_wall")
    val REMAINING_PAUSED = longPreferencesKey("pomodoro_remaining_paused")
    val FOCUS_MINUTES = intPreferencesKey("pomodoro_focus_minutes")
    val SHORT_BREAK_MINUTES = intPreferencesKey("pomodoro_short_break_minutes")
    val LONG_BREAK_MINUTES = intPreferencesKey("pomodoro_long_break_minutes")
    val COMPLETED_CYCLE = intPreferencesKey("pomodoro_completed_cycle")

    val WATER_ENABLED = booleanPreferencesKey("water_enabled")
    val WATER_INTERVAL_MINUTES = intPreferencesKey("water_interval_minutes")
    val NEXT_WATER_WALL = longPreferencesKey("next_water_wall")
    val WATER_GLASSES_TODAY = intPreferencesKey("water_glasses_today")
    val WATER_DATE_KEY = stringPreferencesKey("water_date_key")
  }

  val stateFlow: Flow<CadenceState> =
    context.cadenceDataStore.data.map { prefs -> prefsToState(prefs) }

  suspend fun currentState(): CadenceState = stateFlow.first()

  suspend fun startTimer(phase: PomodoroPhase, endTimeWallMillis: Long) {
    context.cadenceDataStore.edit { prefs ->
      prefs[Keys.PHASE] = phase.name
      prefs[Keys.IS_RUNNING] = true
      prefs[Keys.END_TIME_WALL] = endTimeWallMillis
    }
  }

  suspend fun pauseTimer(remainingMillis: Long) {
    context.cadenceDataStore.edit { prefs ->
      prefs[Keys.IS_RUNNING] = false
      prefs[Keys.END_TIME_WALL] = 0L
      prefs[Keys.REMAINING_PAUSED] = remainingMillis.coerceAtLeast(1_000L)
    }
  }

  suspend fun resetTimer(phase: PomodoroPhase? = null) {
    context.cadenceDataStore.edit { prefs ->
      val current = prefsToState(prefs)
      val targetPhase = phase ?: current.phase
      val fullDuration =
        targetPhase.durationMillis(
          current.focusMinutes,
          current.shortBreakMinutes,
          current.longBreakMinutes,
        )
      prefs[Keys.PHASE] = targetPhase.name
      prefs[Keys.IS_RUNNING] = false
      prefs[Keys.END_TIME_WALL] = 0L
      prefs[Keys.REMAINING_PAUSED] = fullDuration
    }
  }

  suspend fun advancePhaseAfterCompletion(countAsCompletedFocus: Boolean): Pair<PomodoroPhase, PomodoroPhase> {
    var transition = PomodoroPhase.FOCUS to PomodoroPhase.SHORT_BREAK
    context.cadenceDataStore.edit { prefs ->
      val current = prefsToState(prefs)
      val finishedPhase = current.phase
      var nextCompleted = current.completedFocusInCycle
      val nextPhase =
        when (finishedPhase) {
          PomodoroPhase.FOCUS -> {
            if (countAsCompletedFocus) {
              nextCompleted += 1
            }
            if (nextCompleted >= CadenceState.SESSIONS_PER_CYCLE) {
              if (countAsCompletedFocus) {
                nextCompleted = 0
              }
              PomodoroPhase.LONG_BREAK
            } else {
              PomodoroPhase.SHORT_BREAK
            }
          }
          PomodoroPhase.SHORT_BREAK,
          PomodoroPhase.LONG_BREAK -> PomodoroPhase.FOCUS
        }

      val nextDuration =
        nextPhase.durationMillis(
          current.focusMinutes,
          current.shortBreakMinutes,
          current.longBreakMinutes,
        )
      prefs[Keys.PHASE] = nextPhase.name
      prefs[Keys.IS_RUNNING] = false
      prefs[Keys.END_TIME_WALL] = 0L
      prefs[Keys.REMAINING_PAUSED] = nextDuration
      prefs[Keys.COMPLETED_CYCLE] = nextCompleted
      transition = finishedPhase to nextPhase
    }
    return transition
  }

  suspend fun updatePomodoroDurations(
    focusMinutes: Int,
    shortBreakMinutes: Int,
    longBreakMinutes: Int,
  ) {
    context.cadenceDataStore.edit { prefs ->
      val current = prefsToState(prefs)
      prefs[Keys.FOCUS_MINUTES] = focusMinutes
      prefs[Keys.SHORT_BREAK_MINUTES] = shortBreakMinutes
      prefs[Keys.LONG_BREAK_MINUTES] = longBreakMinutes
      if (!current.isRunning) {
        prefs[Keys.REMAINING_PAUSED] =
          current.phase.durationMillis(focusMinutes, shortBreakMinutes, longBreakMinutes)
      }
    }
  }

  suspend fun setWaterReminder(enabled: Boolean, nextTriggerWallMillis: Long) {
    context.cadenceDataStore.edit { prefs ->
      prefs[Keys.WATER_ENABLED] = enabled
      prefs[Keys.NEXT_WATER_WALL] = if (enabled) nextTriggerWallMillis else 0L
    }
  }

  suspend fun setWaterInterval(intervalMinutes: Int, nextTriggerWallMillis: Long?) {
    context.cadenceDataStore.edit { prefs ->
      prefs[Keys.WATER_INTERVAL_MINUTES] = intervalMinutes.coerceIn(5, 180)
      if (nextTriggerWallMillis != null) {
        prefs[Keys.NEXT_WATER_WALL] = nextTriggerWallMillis
      }
    }
  }

  suspend fun updateNextWaterWallMillis(nextTriggerWallMillis: Long) {
    context.cadenceDataStore.edit { prefs ->
      prefs[Keys.NEXT_WATER_WALL] = nextTriggerWallMillis
    }
  }

  suspend fun incrementWaterGlass() {
    val today = todayDateKey()
    context.cadenceDataStore.edit { prefs ->
      val savedDate = prefs[Keys.WATER_DATE_KEY] ?: ""
      val currentCount = if (savedDate == today) prefs[Keys.WATER_GLASSES_TODAY] ?: 0 else 0
      prefs[Keys.WATER_DATE_KEY] = today
      prefs[Keys.WATER_GLASSES_TODAY] = currentCount + 1
    }
  }

  private fun prefsToState(prefs: Preferences): CadenceState {
    val focusMin = prefs[Keys.FOCUS_MINUTES] ?: CadenceState.DEFAULT_FOCUS_MINUTES
    val shortMin = prefs[Keys.SHORT_BREAK_MINUTES] ?: CadenceState.DEFAULT_SHORT_BREAK_MINUTES
    val longMin = prefs[Keys.LONG_BREAK_MINUTES] ?: CadenceState.DEFAULT_LONG_BREAK_MINUTES
    val phase = PomodoroPhase.fromName(prefs[Keys.PHASE])
    val defaultPhaseDuration = phase.durationMillis(focusMin, shortMin, longMin)
    val today = todayDateKey()
    val savedDate = prefs[Keys.WATER_DATE_KEY] ?: today
    val glassesToday = if (savedDate == today) prefs[Keys.WATER_GLASSES_TODAY] ?: 0 else 0

    return CadenceState(
      phase = phase,
      isRunning = prefs[Keys.IS_RUNNING] ?: false,
      endTimeWallMillis = prefs[Keys.END_TIME_WALL] ?: 0L,
      remainingMillisWhenPaused = prefs[Keys.REMAINING_PAUSED] ?: defaultPhaseDuration,
      focusMinutes = focusMin,
      shortBreakMinutes = shortMin,
      longBreakMinutes = longMin,
      completedFocusInCycle = prefs[Keys.COMPLETED_CYCLE] ?: 0,
      waterEnabled = prefs[Keys.WATER_ENABLED] ?: false,
      waterIntervalMinutes =
        prefs[Keys.WATER_INTERVAL_MINUTES] ?: CadenceState.DEFAULT_WATER_INTERVAL_MINUTES,
      nextWaterWallMillis = prefs[Keys.NEXT_WATER_WALL] ?: 0L,
      waterGlassesToday = glassesToday,
      waterDateKey = today,
    )
  }

  companion object {
    fun todayDateKey(): String =
      SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
  }
}
