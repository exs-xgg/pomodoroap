package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.CustomWaterIntervalSheet
import com.example.ui.components.DurationSettingsSheet
import com.example.ui.components.PomodoroDialSection
import com.example.ui.components.WaterReminderCard
import kotlinx.coroutines.launch

@Composable
fun CadenceScreen(
  viewModel: CadenceViewModel,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  var showPomodoroSheet by remember { mutableStateOf(false) }
  var showWaterIntervalSheet by remember { mutableStateOf(false) }

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
      onResult = { viewModel.refreshNotificationPermission() },
    )

  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    viewModel.refreshNotificationPermission()
  }

  fun requestNotificationPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !uiState.hasNotificationPermission) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  val testSentMessage = stringResource(R.string.water_test_sent)

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      val isWideLayout = maxWidth >= 680.dp

      Column(
        modifier =
          Modifier.fillMaxSize()
            .widthIn(max = if (isWideLayout) 960.dp else 560.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // Minimal top wordmark header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = stringResource(R.string.brand_wordmark),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          val sessionText =
            stringResource(
              R.string.pomodoro_session_progress,
              (uiState.state.completedFocusInCycle + 1).coerceAtMost(4),
              4,
            )
          Text(
            text = sessionText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        // Subtle notification permission prompt when denied/ungranted on Android 13+
        AnimatedVisibility(
          visible = !uiState.hasNotificationPermission,
          enter = fadeIn(),
          exit = fadeOut(),
        ) {
          Surface(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f),
              ) {
                Icon(
                  imageVector = Icons.Rounded.NotificationsOff,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = stringResource(R.string.notifications_off_banner),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }

              TextButton(
                onClick = { requestNotificationPermissionIfNeeded() },
                modifier =
                  Modifier.testTag("enable_notifications_button").minimumInteractiveComponentSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
              ) {
                Text(
                  text = stringResource(R.string.enable),
                  style = MaterialTheme.typography.labelLarge,
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(if (isWideLayout) 32.dp else 20.dp))

        if (isWideLayout) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
              PomodoroDialSection(
                uiState = uiState,
                onSelectPhase = viewModel::selectPhase,
                onToggleStartPause = {
                  requestNotificationPermissionIfNeeded()
                  viewModel.toggleStartPausePomodoro()
                },
                onReset = viewModel::resetPomodoro,
                onSkip = viewModel::skipPhase,
                onOpenDurationSettings = { showPomodoroSheet = true },
              )
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
              WaterReminderCard(
                uiState = uiState,
                onToggleReminder = { enabled ->
                  if (enabled) requestNotificationPermissionIfNeeded()
                  viewModel.toggleWaterReminder(enabled)
                },
                onSelectInterval = viewModel::setWaterInterval,
                onOpenCustomInterval = { showWaterIntervalSheet = true },
                onLogGlass = viewModel::logWaterGlass,
                onTestAlert = {
                  requestNotificationPermissionIfNeeded()
                  viewModel.sendTestWaterNotification()
                  if (uiState.hasNotificationPermission) {
                    scope.launch { snackbarHostState.showSnackbar(testSentMessage) }
                  }
                },
              )
            }
          }
        } else {
          PomodoroDialSection(
            uiState = uiState,
            onSelectPhase = viewModel::selectPhase,
            onToggleStartPause = {
              requestNotificationPermissionIfNeeded()
              viewModel.toggleStartPausePomodoro()
            },
            onReset = viewModel::resetPomodoro,
            onSkip = viewModel::skipPhase,
            onOpenDurationSettings = { showPomodoroSheet = true },
          )

          Spacer(modifier = Modifier.height(28.dp))

          WaterReminderCard(
            uiState = uiState,
            onToggleReminder = { enabled ->
              if (enabled) requestNotificationPermissionIfNeeded()
              viewModel.toggleWaterReminder(enabled)
            },
            onSelectInterval = viewModel::setWaterInterval,
            onOpenCustomInterval = { showWaterIntervalSheet = true },
            onLogGlass = viewModel::logWaterGlass,
            onTestAlert = {
              requestNotificationPermissionIfNeeded()
              viewModel.sendTestWaterNotification()
              if (uiState.hasNotificationPermission) {
                scope.launch { snackbarHostState.showSnackbar(testSentMessage) }
              }
            },
          )
        }

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }

  if (showPomodoroSheet) {
    DurationSettingsSheet(
      focusMinutes = uiState.state.focusMinutes,
      shortBreakMinutes = uiState.state.shortBreakMinutes,
      longBreakMinutes = uiState.state.longBreakMinutes,
      onSave = viewModel::updatePomodoroDurations,
      onDismiss = { showPomodoroSheet = false },
    )
  }

  if (showWaterIntervalSheet) {
    CustomWaterIntervalSheet(
      currentIntervalMinutes = uiState.state.waterIntervalMinutes,
      onSave = viewModel::setWaterInterval,
      onDismiss = { showWaterIntervalSheet = false },
    )
  }
}
