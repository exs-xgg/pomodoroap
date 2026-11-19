package com.example.model

enum class PomodoroPhase {
  FOCUS,
  SHORT_BREAK,
  LONG_BREAK;

  fun durationMinutes(
    focusMinutes: Int,
    shortBreakMinutes: Int,
    longBreakMinutes: Int,
  ): Int =
    when (this) {
      FOCUS -> focusMinutes
      SHORT_BREAK -> shortBreakMinutes
      LONG_BREAK -> longBreakMinutes
    }

  fun durationMillis(
    focusMinutes: Int,
    shortBreakMinutes: Int,
    longBreakMinutes: Int,
  ): Long = durationMinutes(focusMinutes, shortBreakMinutes, longBreakMinutes) * 60_000L

  companion object {
    fun fromName(value: String?): PomodoroPhase =
      entries.firstOrNull { it.name == value } ?: FOCUS
  }
}

data class CadenceState(
  val phase: PomodoroPhase = PomodoroPhase.FOCUS,
  val isRunning: Boolean = false,
  val endTimeWallMillis: Long = 0L,
  val remainingMillisWhenPaused: Long = DEFAULT_FOCUS_MINUTES * 60_000L,
  val focusMinutes: Int = DEFAULT_FOCUS_MINUTES,
  val shortBreakMinutes: Int = DEFAULT_SHORT_BREAK_MINUTES,
  val longBreakMinutes: Int = DEFAULT_LONG_BREAK_MINUTES,
  val completedFocusInCycle: Int = 0,
  val waterEnabled: Boolean = false,
  val waterIntervalMinutes: Int = DEFAULT_WATER_INTERVAL_MINUTES,
  val nextWaterWallMillis: Long = 0L,
  val waterGlassesToday: Int = 0,
  val waterDateKey: String = "",
) {
  val totalPhaseMillis: Long
    get() = phase.durationMillis(focusMinutes, shortBreakMinutes, longBreakMinutes)

  companion object {
    const val DEFAULT_FOCUS_MINUTES = 25
    const val DEFAULT_SHORT_BREAK_MINUTES = 5
    const val DEFAULT_LONG_BREAK_MINUTES = 15
    const val DEFAULT_WATER_INTERVAL_MINUTES = 45
    const val SESSIONS_PER_CYCLE = 4
  }
}
