package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CadenceState
import com.example.model.PomodoroPhase
import com.example.ui.CadenceUiState
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PomodoroDialSection(
  uiState: CadenceUiState,
  onSelectPhase: (PomodoroPhase) -> Unit,
  onToggleStartPause: () -> Unit,
  onReset: () -> Unit,
  onSkip: () -> Unit,
  onOpenDurationSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state = uiState.state
  val accentColor by
    animateColorAsState(
      targetValue =
        when (state.phase) {
          PomodoroPhase.FOCUS -> MaterialTheme.colorScheme.primary
          PomodoroPhase.SHORT_BREAK,
          PomodoroPhase.LONG_BREAK -> MaterialTheme.colorScheme.secondary
        },
      animationSpec = tween(durationMillis = 300),
      label = "pomodoro_accent",
    )

  val onAccentColor by
    animateColorAsState(
      targetValue =
        when (state.phase) {
          PomodoroPhase.FOCUS -> MaterialTheme.colorScheme.onPrimary
          PomodoroPhase.SHORT_BREAK,
          PomodoroPhase.LONG_BREAK -> MaterialTheme.colorScheme.onSecondary
        },
      animationSpec = tween(durationMillis = 300),
      label = "pomodoro_on_accent",
    )

  val containerAccentColor by
    animateColorAsState(
      targetValue =
        when (state.phase) {
          PomodoroPhase.FOCUS -> MaterialTheme.colorScheme.primaryContainer
          PomodoroPhase.SHORT_BREAK,
          PomodoroPhase.LONG_BREAK -> MaterialTheme.colorScheme.secondaryContainer
        },
      animationSpec = tween(durationMillis = 300),
      label = "pomodoro_container_accent",
    )

  val animatedProgress by
    animateFloatAsState(
      targetValue = uiState.progressFraction,
      animationSpec = spring(stiffness = 300f),
      label = "pomodoro_progress",
    )

  val isPausedMidway =
    !state.isRunning && uiState.displayedRemainingMillis < state.totalPhaseMillis

  AnimatedContent(
    targetState = state.isRunning,
    modifier = modifier.fillMaxWidth(),
    transitionSpec = {
      (fadeIn(tween(240)) + expandVertically(spring(stiffness = 380f), expandFrom = Alignment.Top))
        .togetherWith(
          fadeOut(tween(180)) + shrinkVertically(spring(stiffness = 380f), shrinkTowards = Alignment.Top)
        )
    },
    label = "pomodoro_collapse_transition",
  ) { isRunning ->
    if (isRunning) {
      CollapsedRunningTimer(
        phase = state.phase,
        remainingMillis = uiState.displayedRemainingMillis,
        progress = animatedProgress,
        accentColor = accentColor,
        containerAccentColor = containerAccentColor,
        onPause = onToggleStartPause,
        onReset = onReset,
      )
    } else {
      ExpandedPomodoroTimer(
        uiState = uiState,
        animatedProgress = animatedProgress,
        isPausedMidway = isPausedMidway,
        accentColor = accentColor,
        onAccentColor = onAccentColor,
        onSelectPhase = onSelectPhase,
        onToggleStartPause = onToggleStartPause,
        onReset = onReset,
        onSkip = onSkip,
        onOpenDurationSettings = onOpenDurationSettings,
      )
    }
  }
}

@Composable
private fun CollapsedRunningTimer(
  phase: PomodoroPhase,
  remainingMillis: Long,
  progress: Float,
  accentColor: Color,
  containerAccentColor: Color,
  onPause: () -> Unit,
  onReset: () -> Unit,
) {
  val phaseLabelRes =
    when (phase) {
      PomodoroPhase.FOCUS -> R.string.phase_focus
      PomodoroPhase.SHORT_BREAK -> R.string.phase_short_break
      PomodoroPhase.LONG_BREAK -> R.string.phase_long_break
    }
  val trackColor = MaterialTheme.colorScheme.outlineVariant

  Surface(
    modifier = Modifier.fillMaxWidth().testTag("pomodoro_collapsed_card"),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    tonalElevation = 1.dp,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        // Compact mini progress ring
        Box(
          modifier = Modifier.size(44.dp),
          contentAlignment = Alignment.Center,
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 4.dp.toPx()
            val radius = (size.minDimension - strokePx) / 2f
            drawCircle(
              color = trackColor,
              radius = radius,
              style = Stroke(width = strokePx),
            )
            drawArc(
              color = accentColor,
              startAngle = -90f,
              sweepAngle = 360f * progress,
              useCenter = false,
              topLeft = Offset(strokePx / 2f, strokePx / 2f),
              size = Size(size.width - strokePx, size.height - strokePx),
              style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
          }
          Box(
            modifier =
              Modifier.size(8.dp)
                .clip(CircleShape)
                .background(accentColor)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
          Text(
            text = stringResource(phaseLabelRes).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
          )
          Text(
            text = formatCountdown(remainingMillis),
            style = MaterialTheme.typography.headlineMedium.copy(
              fontFamily = MaterialTheme.typography.displayLarge.fontFamily,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("pomodoro_time_text"),
          )
        }
      }

      // Simplified controls: Pause + Reset only
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilledTonalIconButton(
          onClick = onReset,
          modifier = Modifier.testTag("pomodoro_reset_button").size(48.dp),
          colors =
            IconButtonDefaults.filledTonalIconButtonColors(
              containerColor = MaterialTheme.colorScheme.surfaceVariant,
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
          Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = stringResource(R.string.action_reset),
          )
        }

        FilledTonalIconButton(
          onClick = onPause,
          modifier = Modifier.testTag("pomodoro_primary_button").size(48.dp),
          colors =
            IconButtonDefaults.filledTonalIconButtonColors(
              containerColor = containerAccentColor,
              contentColor = accentColor,
            ),
        ) {
          Icon(
            imageVector = Icons.Rounded.Pause,
            contentDescription = stringResource(R.string.action_pause),
          )
        }
      }
    }
  }
}

@Composable
private fun ExpandedPomodoroTimer(
  uiState: CadenceUiState,
  animatedProgress: Float,
  isPausedMidway: Boolean,
  accentColor: Color,
  onAccentColor: Color,
  onSelectPhase: (PomodoroPhase) -> Unit,
  onToggleStartPause: () -> Unit,
  onReset: () -> Unit,
  onSkip: () -> Unit,
  onOpenDurationSettings: () -> Unit,
) {
  val state = uiState.state

  Column(
    modifier = Modifier.fillMaxWidth().testTag("pomodoro_expanded_section"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Minimal phase selector + tune button
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
      ) {
        Row(
          modifier = Modifier.padding(4.dp),
          horizontalArrangement = Arrangement.spacedBy(2.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          PomodoroPhase.entries.forEach { phase ->
            val selected = state.phase == phase
            val pillBg by
              animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                animationSpec = tween(200),
                label = "phase_pill_bg",
              )
            val labelRes =
              when (phase) {
                PomodoroPhase.FOCUS -> R.string.phase_focus
                PomodoroPhase.SHORT_BREAK -> R.string.phase_short_break
                PomodoroPhase.LONG_BREAK -> R.string.phase_long_break
              }

            Box(
              modifier =
                Modifier.testTag("phase_pill_${phase.name.lowercase()}")
                  .minimumInteractiveComponentSize()
                  .clip(RoundedCornerShape(999.dp))
                  .background(pillBg)
                  .clickable(role = Role.Tab) { onSelectPhase(phase) }
                  .padding(horizontal = 16.dp, vertical = 10.dp),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelLarge,
                color =
                  if (selected) MaterialTheme.colorScheme.onSurface
                  else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      FilledTonalIconButton(
        onClick = onOpenDurationSettings,
        modifier = Modifier.testTag("pomodoro_settings_button").minimumInteractiveComponentSize(),
        colors =
          IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
          ),
      ) {
        Icon(
          imageVector = Icons.Rounded.Tune,
          contentDescription = stringResource(R.string.action_adjust_durations),
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Hero circular countdown dial
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val subtleTickColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

    Box(
      modifier =
        Modifier.sizeIn(minWidth = 220.dp, maxWidth = 276.dp)
          .fillMaxWidth(0.74f)
          .aspectRatio(1f)
          .clip(CircleShape)
          .clickable { onOpenDurationSettings() },
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val minDim = size.minDimension
        val ringStrokePx = 7.dp.toPx()
        val outerTickRadius = minDim / 2f - 4.dp.toPx()
        val ringRadius = outerTickRadius - 14.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)

        // 60 precision Swiss watchmaker ticks around outer perimeter
        for (i in 0 until 60) {
          val angleRad = (i * 6f - 90f) * (PI / 180.0)
          val isMajor = i % 5 == 0
          val tickLength = if (isMajor) 6.dp.toPx() else 3.dp.toPx()
          val tickStroke = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
          val start =
            Offset(
              x = center.x + ((outerTickRadius - tickLength) * cos(angleRad)).toFloat(),
              y = center.y + ((outerTickRadius - tickLength) * sin(angleRad)).toFloat(),
            )
          val end =
            Offset(
              x = center.x + (outerTickRadius * cos(angleRad)).toFloat(),
              y = center.y + (outerTickRadius * sin(angleRad)).toFloat(),
            )
          drawLine(
            color = if (isMajor) subtleTickColor else subtleTickColor.copy(alpha = 0.45f),
            start = start,
            end = end,
            strokeWidth = tickStroke,
            cap = StrokeCap.Round,
          )
        }

        // Track ring
        drawCircle(
          color = trackColor,
          radius = ringRadius,
          center = center,
          style = Stroke(width = ringStrokePx),
        )

        // Progress arc
        val topLeft = Offset(center.x - ringRadius, center.y - ringRadius)
        val arcSize = Size(ringRadius * 2f, ringRadius * 2f)
        drawArc(
          color = accentColor,
          startAngle = -90f,
          sweepAngle = 360f * animatedProgress,
          useCenter = false,
          topLeft = topLeft,
          size = arcSize,
          style = Stroke(width = ringStrokePx, cap = StrokeCap.Round),
        )
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = formatCountdown(uiState.displayedRemainingMillis),
          style = MaterialTheme.typography.displayLarge,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.testTag("pomodoro_time_text"),
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 4-session cycle progress dots
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          for (index in 0 until CadenceState.SESSIONS_PER_CYCLE) {
            val isCompleted = index < state.completedFocusInCycle
            val isCurrentFocus =
              index == state.completedFocusInCycle && state.phase == PomodoroPhase.FOCUS
            val dotColor =
              when {
                isCompleted -> accentColor
                isCurrentFocus && isPausedMidway -> accentColor.copy(alpha = 0.45f)
                else -> MaterialTheme.colorScheme.outlineVariant
              }
            Box(
              modifier =
                Modifier.size(if (isCurrentFocus) 9.dp else 7.dp)
                  .clip(CircleShape)
                  .background(dotColor)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Primary transport controls
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      FilledTonalIconButton(
        onClick = onReset,
        modifier = Modifier.testTag("pomodoro_reset_button").size(48.dp),
        colors =
          IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
          ),
      ) {
        Icon(
          imageVector = Icons.Rounded.Refresh,
          contentDescription = stringResource(R.string.action_reset),
        )
      }

      Button(
        onClick = onToggleStartPause,
        modifier =
          Modifier.testTag("pomodoro_primary_button")
            .height(56.dp)
            .minimumInteractiveComponentSize(),
        shape = RoundedCornerShape(999.dp),
        colors =
          ButtonDefaults.buttonColors(
            containerColor = accentColor,
            contentColor = onAccentColor,
          ),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.PlayArrow,
          contentDescription = null,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text =
            if (isPausedMidway) stringResource(R.string.action_resume)
            else stringResource(R.string.action_start),
          style = MaterialTheme.typography.titleMedium,
        )
      }

      FilledTonalIconButton(
        onClick = onSkip,
        modifier = Modifier.testTag("pomodoro_skip_button").size(48.dp),
        colors =
          IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
          ),
      ) {
        Icon(
          imageVector = Icons.Rounded.SkipNext,
          contentDescription = stringResource(R.string.action_skip),
        )
      }
    }
  }
}

private fun formatCountdown(millis: Long): String {
  val totalSeconds = if (millis <= 0L) 0L else (millis + 999L) / 1000L
  val minutes = totalSeconds / 60L
  val seconds = totalSeconds % 60L
  return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
