package com.example.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.CadenceApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WaterReminderReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent?) {
    val pendingResult = goAsync()
    val app = context.applicationContext as CadenceApplication
    CoroutineScope(Dispatchers.IO).launch {
      try {
        NotificationHelper.createChannels(context)
        when (intent?.action) {
          ACTION_WATER_ALARM -> {
            val state = app.repository.currentState()
            if (state.waterEnabled) {
              NotificationHelper.showWaterReminderNotification(context)
              val nextTrigger =
                WaterReminderScheduler.scheduleNextWaterReminder(
                  context,
                  state.waterIntervalMinutes,
                )
              app.repository.updateNextWaterWallMillis(nextTrigger)
            }
          }
          ACTION_LOG_WATER -> {
            app.repository.incrementWaterGlass()
            NotificationHelper.cancelNotification(
              context,
              NotificationHelper.NOTIF_ID_WATER_REMINDER,
            )
          }
        }
      } finally {
        pendingResult.finish()
      }
    }
  }

  companion object {
    const val ACTION_WATER_ALARM = "com.example.action.WATER_REMINDER_ALARM"
    const val ACTION_LOG_WATER = "com.example.action.LOG_WATER_FROM_NOTIF"
  }
}
