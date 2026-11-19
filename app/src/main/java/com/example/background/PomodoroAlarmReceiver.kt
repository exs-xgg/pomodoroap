package com.example.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.CadenceApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PomodoroAlarmReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent?) {
    if (intent?.action != ACTION_POMODORO_COMPLETE) return
    val pendingResult = goAsync()
    val app = context.applicationContext as CadenceApplication
    CoroutineScope(Dispatchers.IO).launch {
      try {
        NotificationHelper.createChannels(context)
        val state = app.repository.currentState()
        if (state.isRunning && System.currentTimeMillis() >= state.endTimeWallMillis - 1_500L) {
          val (finishedPhase, nextPhase) =
            app.repository.advancePhaseAfterCompletion(countAsCompletedFocus = true)
          val nextMinutes =
            nextPhase.durationMinutes(
              state.focusMinutes,
              state.shortBreakMinutes,
              state.longBreakMinutes,
            )
          NotificationHelper.showPomodoroCompletedNotification(
            context = context,
            finishedPhase = finishedPhase,
            nextPhase = nextPhase,
            nextPhaseMinutes = nextMinutes,
          )
          context.stopService(Intent(context, PomodoroService::class.java))
        }
      } finally {
        pendingResult.finish()
      }
    }
  }

  companion object {
    const val ACTION_POMODORO_COMPLETE = "com.example.action.POMODORO_ALARM_COMPLETE"
  }
}
