package com.example.background

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object WaterReminderScheduler {

  private const val WATER_REQUEST_CODE = 3001
  private const val POMODORO_BACKUP_REQUEST_CODE = 3002

  fun scheduleNextWaterReminder(context: Context, intervalMinutes: Int): Long {
    val triggerAtMillis = System.currentTimeMillis() + intervalMinutes * 60_000L
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return triggerAtMillis
    val pendingIntent = waterPendingIntent(context)
    alarmManager.cancel(pendingIntent)
    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    return triggerAtMillis
  }

  fun cancelWaterReminder(context: Context) {
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
    alarmManager.cancel(waterPendingIntent(context))
  }

  fun schedulePomodoroBackupAlarm(context: Context, triggerAtWallMillis: Long) {
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
    val pendingIntent = pomodoroBackupPendingIntent(context)
    alarmManager.cancel(pendingIntent)
    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtWallMillis, pendingIntent)
  }

  fun cancelPomodoroBackupAlarm(context: Context) {
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
    alarmManager.cancel(pomodoroBackupPendingIntent(context))
  }

  private fun waterPendingIntent(context: Context): PendingIntent {
    val intent =
      Intent(context, WaterReminderReceiver::class.java)
        .setAction(WaterReminderReceiver.ACTION_WATER_ALARM)
    return PendingIntent.getBroadcast(
      context,
      WATER_REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun pomodoroBackupPendingIntent(context: Context): PendingIntent {
    val intent =
      Intent(context, PomodoroAlarmReceiver::class.java)
        .setAction(PomodoroAlarmReceiver.ACTION_POMODORO_COMPLETE)
    return PendingIntent.getBroadcast(
      context,
      POMODORO_BACKUP_REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }
}
