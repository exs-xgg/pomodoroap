package com.example.background

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.CadenceApplication
import com.example.model.PomodoroPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PomodoroService : Service() {

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private var tickerJob: Job? = null

  private val repository by lazy { (application as CadenceApplication).repository }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    NotificationHelper.createChannels(this)
    when (intent?.action) {
      ACTION_START -> {
        if (intent.getBooleanExtra(EXTRA_DISMISS_ALERT, false)) {
          NotificationHelper.cancelNotification(this, NotificationHelper.NOTIF_ID_POMODORO_ALERT)
        }
        val explicitPhaseName = intent.getStringExtra(EXTRA_PHASE_NAME)
        startCountdown(explicitPhaseName)
      }
      ACTION_PAUSE -> pauseCountdown()
      ACTION_RESET -> resetCountdown()
      else -> stopSelfSafely()
    }
    return START_NOT_STICKY
  }

  private fun startCountdown(explicitPhaseName: String?) {
    // Enter foreground immediately with a placeholder or current notification to satisfy Android FGS contract
    val initialPhase = PomodoroPhase.fromName(explicitPhaseName)
    val placeholderEnd = System.currentTimeMillis() + 25 * 60_000L
    startForegroundWithNotification(initialPhase, placeholderEnd)

    tickerJob?.cancel()
    tickerJob =
      serviceScope.launch {
        val state = repository.currentState()
        val phase =
          if (explicitPhaseName != null) PomodoroPhase.fromName(explicitPhaseName) else state.phase
        val durationMillis =
          if (explicitPhaseName != null && explicitPhaseName != state.phase.name) {
            phase.durationMillis(
              state.focusMinutes,
              state.shortBreakMinutes,
              state.longBreakMinutes,
            )
          } else if (state.isRunning && state.endTimeWallMillis > System.currentTimeMillis()) {
            state.endTimeWallMillis - System.currentTimeMillis()
          } else {
            state.remainingMillisWhenPaused.coerceAtLeast(1_000L)
          }

        val endTimeWallMillis = System.currentTimeMillis() + durationMillis
        repository.startTimer(phase, endTimeWallMillis)
        WaterReminderScheduler.schedulePomodoroBackupAlarm(this@PomodoroService, endTimeWallMillis)
        startForegroundWithNotification(phase, endTimeWallMillis)

        while (isActive) {
          val remaining = endTimeWallMillis - System.currentTimeMillis()
          if (remaining <= 0L) {
            completeTimer()
            break
          }
          delay(remaining.coerceAtMost(500L))
        }
      }
  }

  private fun pauseCountdown() {
    tickerJob?.cancel()
    WaterReminderScheduler.cancelPomodoroBackupAlarm(this)
    serviceScope.launch {
      val state = repository.currentState()
      if (state.isRunning) {
        val remaining = (state.endTimeWallMillis - System.currentTimeMillis()).coerceAtLeast(1_000L)
        repository.pauseTimer(remaining)
      }
      stopSelfSafely()
    }
  }

  private fun resetCountdown() {
    tickerJob?.cancel()
    WaterReminderScheduler.cancelPomodoroBackupAlarm(this)
    serviceScope.launch {
      repository.resetTimer()
      stopSelfSafely()
    }
  }

  private suspend fun completeTimer() {
    WaterReminderScheduler.cancelPomodoroBackupAlarm(this)
    val latest = repository.currentState()
    if (latest.isRunning) {
      val (finishedPhase, nextPhase) = repository.advancePhaseAfterCompletion(countAsCompletedFocus = true)
      val nextMinutes =
        nextPhase.durationMinutes(
          latest.focusMinutes,
          latest.shortBreakMinutes,
          latest.longBreakMinutes,
        )
      NotificationHelper.showPomodoroCompletedNotification(
        context = this,
        finishedPhase = finishedPhase,
        nextPhase = nextPhase,
        nextPhaseMinutes = nextMinutes,
      )
    }
    stopSelfSafely()
  }

  private fun startForegroundWithNotification(phase: PomodoroPhase, endTimeWallMillis: Long) {
    val notification =
      NotificationHelper.buildOngoingPomodoroNotification(this, phase, endTimeWallMillis)
    val fgsType =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
      } else {
        0
      }
    ServiceCompat.startForeground(
      this,
      NotificationHelper.NOTIF_ID_POMODORO_ONGOING,
      notification,
      fgsType,
    )
  }

  private fun stopSelfSafely() {
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  override fun onDestroy() {
    tickerJob?.cancel()
    serviceScope.cancel()
    super.onDestroy()
  }

  companion object {
    const val ACTION_START = "com.example.action.POMODORO_START"
    const val ACTION_PAUSE = "com.example.action.POMODORO_PAUSE"
    const val ACTION_RESET = "com.example.action.POMODORO_RESET"
    const val EXTRA_PHASE_NAME = "extra_phase_name"
    const val EXTRA_DISMISS_ALERT = "extra_dismiss_alert"

    fun startTimer(context: Context, phase: PomodoroPhase? = null) {
      val intent =
        Intent(context, PomodoroService::class.java).apply {
          action = ACTION_START
          if (phase != null) {
            putExtra(EXTRA_PHASE_NAME, phase.name)
          }
        }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun pauseTimer(context: Context) {
      val intent = Intent(context, PomodoroService::class.java).setAction(ACTION_PAUSE)
      context.startService(intent)
    }

    fun resetTimer(context: Context) {
      val intent = Intent(context, PomodoroService::class.java).setAction(ACTION_RESET)
      context.startService(intent)
    }
  }
}
