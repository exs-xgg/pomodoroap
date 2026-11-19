package com.example.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.CadenceApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent?) {
    if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
    val pendingResult = goAsync()
    val app = context.applicationContext as CadenceApplication
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val state = app.repository.currentState()
        if (state.waterEnabled) {
          val nextTrigger =
            WaterReminderScheduler.scheduleNextWaterReminder(context, state.waterIntervalMinutes)
          app.repository.updateNextWaterWallMillis(nextTrigger)
        }
        if (state.isRunning) {
          app.repository.resetTimer(state.phase)
        }
      } finally {
        pendingResult.finish()
      }
    }
  }
}
