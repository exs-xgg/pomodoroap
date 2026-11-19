package com.example.background

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.PomodoroPhase

object NotificationHelper {

  const val CHANNEL_POMODORO_ONGOING = "pomodoro_ongoing"
  const val CHANNEL_POMODORO_ALERTS = "pomodoro_alerts"
  const val CHANNEL_WATER_REMINDERS = "water_reminders"

  const val NOTIF_ID_POMODORO_ONGOING = 1001
  const val NOTIF_ID_POMODORO_ALERT = 1002
  const val NOTIF_ID_WATER_REMINDER = 2001

  fun createChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(NotificationManager::class.java) ?: return

    val ongoingChannel =
      NotificationChannel(
        CHANNEL_POMODORO_ONGOING,
        context.getString(R.string.channel_pomodoro_ongoing_name),
        NotificationManager.IMPORTANCE_LOW,
      ).apply {
        description = context.getString(R.string.channel_pomodoro_ongoing_desc)
        setShowBadge(false)
      }

    val alertChannel =
      NotificationChannel(
        CHANNEL_POMODORO_ALERTS,
        context.getString(R.string.channel_pomodoro_alerts_name),
        NotificationManager.IMPORTANCE_HIGH,
      ).apply {
        description = context.getString(R.string.channel_pomodoro_alerts_desc)
        enableVibration(true)
      }

    val waterChannel =
      NotificationChannel(
        CHANNEL_WATER_REMINDERS,
        context.getString(R.string.channel_water_name),
        NotificationManager.IMPORTANCE_HIGH,
      ).apply {
        description = context.getString(R.string.channel_water_desc)
        enableVibration(true)
      }

    manager.createNotificationChannels(listOf(ongoingChannel, alertChannel, waterChannel))
  }

  fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun buildOngoingPomodoroNotification(
    context: Context,
    phase: PomodoroPhase,
    endTimeWallMillis: Long,
  ): Notification {
    val titleRes =
      when (phase) {
        PomodoroPhase.FOCUS -> R.string.notif_focus_running_title
        PomodoroPhase.SHORT_BREAK -> R.string.notif_short_break_running_title
        PomodoroPhase.LONG_BREAK -> R.string.notif_long_break_running_title
      }

    val pauseIntent =
      PendingIntent.getService(
        context,
        11,
        Intent(context, PomodoroService::class.java).setAction(PomodoroService.ACTION_PAUSE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val resetIntent =
      PendingIntent.getService(
        context,
        12,
        Intent(context, PomodoroService::class.java).setAction(PomodoroService.ACTION_RESET),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    return NotificationCompat.Builder(context, CHANNEL_POMODORO_ONGOING)
      .setSmallIcon(R.drawable.ic_stat_timer)
      .setContentTitle(context.getString(titleRes))
      .setContentText(context.getString(R.string.notif_timer_running_text))
      .setContentIntent(openAppPendingIntent(context))
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setSilent(true)
      .setCategory(NotificationCompat.CATEGORY_PROGRESS)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setUsesChronometer(true)
      .setChronometerCountDown(true)
      .setWhen(endTimeWallMillis)
      .addAction(0, context.getString(R.string.notif_action_pause), pauseIntent)
      .addAction(0, context.getString(R.string.notif_action_stop), resetIntent)
      .build()
  }

  @SuppressLint("MissingPermission")
  fun showPomodoroCompletedNotification(
    context: Context,
    finishedPhase: PomodoroPhase,
    nextPhase: PomodoroPhase,
    nextPhaseMinutes: Int,
  ) {
    if (!hasNotificationPermission(context)) return

    val title: String
    val body: String
    val actionLabel: String

    if (finishedPhase == PomodoroPhase.FOCUS) {
      title = context.getString(R.string.notif_focus_done_title)
      body = context.getString(R.string.notif_focus_done_body, nextPhaseMinutes)
      actionLabel = context.getString(R.string.notif_action_start_break)
    } else {
      title = context.getString(R.string.notif_break_done_title)
      body = context.getString(R.string.notif_break_done_body, nextPhaseMinutes)
      actionLabel = context.getString(R.string.notif_action_start_focus)
    }

    val startNextIntent =
      Intent(context, PomodoroService::class.java).apply {
        action = PomodoroService.ACTION_START
        putExtra(PomodoroService.EXTRA_DISMISS_ALERT, true)
        putExtra(PomodoroService.EXTRA_PHASE_NAME, nextPhase.name)
      }
    val startNextPendingIntent =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        PendingIntent.getForegroundService(
          context,
          13,
          startNextIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
      } else {
        PendingIntent.getService(
          context,
          13,
          startNextIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
      }

    val notification =
      NotificationCompat.Builder(context, CHANNEL_POMODORO_ALERTS)
        .setSmallIcon(R.drawable.ic_stat_timer)
        .setContentTitle(title)
        .setContentText(body)
        .setContentIntent(openAppPendingIntent(context))
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .addAction(0, actionLabel, startNextPendingIntent)
        .build()

    NotificationManagerCompat.from(context).notify(NOTIF_ID_POMODORO_ALERT, notification)
  }

  @SuppressLint("MissingPermission")
  fun showWaterReminderNotification(context: Context) {
    if (!hasNotificationPermission(context)) return

    val logWaterPendingIntent =
      PendingIntent.getBroadcast(
        context,
        21,
        Intent(context, WaterReminderReceiver::class.java)
          .setAction(WaterReminderReceiver.ACTION_LOG_WATER),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val notification =
      NotificationCompat.Builder(context, CHANNEL_WATER_REMINDERS)
        .setSmallIcon(R.drawable.ic_stat_water)
        .setContentTitle(context.getString(R.string.notif_water_title))
        .setContentText(context.getString(R.string.notif_water_body))
        .setContentIntent(openAppPendingIntent(context))
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .addAction(0, context.getString(R.string.notif_action_drank_water), logWaterPendingIntent)
        .build()

    NotificationManagerCompat.from(context).notify(NOTIF_ID_WATER_REMINDER, notification)
  }

  fun cancelNotification(context: Context, id: Int) {
    NotificationManagerCompat.from(context).cancel(id)
  }

  private fun openAppPendingIntent(context: Context): PendingIntent {
    val intent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }
    return PendingIntent.getActivity(
      context,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }
}
