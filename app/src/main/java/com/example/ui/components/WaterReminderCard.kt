package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.CadenceUiState

private val PRESET_INTERVALS = listOf(15, 30, 45, 60, 90)

@Composable
fun WaterReminderCard(
  uiState: CadenceUiState,
  onToggleReminder: (Boolean) -> Unit,
  onSelectInterval: (Int) -> Unit,
  onOpenCustomInterval: () -> Unit,
  onLogGlass: () -> Unit,
  onTestAlert: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val state = uiState.state
  val waterAccent = MaterialTheme.colorScheme.tertiary
  val waterContainer = MaterialTheme.colorScheme.tertiaryContainer
  val onWaterContainer = MaterialTheme.colorScheme.onTertiaryContainer

  val subtitle =
    if (!state.waterEnabled) {
      stringResource(R.string.water_paused)
    } else {
      val minutesLeft = ((uiState.waterRemainingMillis + 59_999L) / 60_000L).toInt()
      if (minutesLeft <= 1) {
        stringResource(R.string.water_next_soon)
      } else {
        stringResource(R.string.water_next_in_minutes, minutesLeft)
      }
    }

  val switchCd = stringResource(R.string.water_toggle_cd)

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    tonalElevation = 1.dp,
  ) {
    Column(modifier = Modifier.padding(20.dp)) {
      // Header row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier =
              Modifier.size(44.dp)
                .clip(CircleShape)
                .background(
                  if (state.waterEnabled) waterContainer
                  else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.WaterDrop,
              contentDescription = null,
              tint =
                if (state.waterEnabled) waterAccent
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(22.dp),
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column {
            Text(
              text = stringResource(R.string.water_title),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text = subtitle,
              style = MaterialTheme.typography.bodyMedium,
              color =
                if (state.waterEnabled) waterAccent
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.testTag("water_status_text"),
            )
          }
        }

        Switch(
          checked = state.waterEnabled,
          onCheckedChange = onToggleReminder,
          modifier =
            Modifier.testTag("water_reminder_switch")
              .minimumInteractiveComponentSize()
              .semantics { contentDescription = switchCd },
          colors =
            SwitchDefaults.colors(
              checkedThumbColor = MaterialTheme.colorScheme.onTertiary,
              checkedTrackColor = waterAccent,
              uncheckedThumbColor = MaterialTheme.colorScheme.outline,
              uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
              uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Interval chips row
      val isCustomInterval = state.waterIntervalMinutes !in PRESET_INTERVALS
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        PRESET_INTERVALS.forEach { minutes ->
          val selected = state.waterIntervalMinutes == minutes
          IntervalChip(
            label = stringResource(R.string.water_interval_minutes_fmt, minutes),
            selected = selected,
            accentColor = waterAccent,
            onAccentColor = MaterialTheme.colorScheme.onTertiary,
            onClick = { onSelectInterval(minutes) },
            modifier = Modifier.testTag("water_interval_$minutes"),
          )
        }

        IntervalChip(
          label =
            if (isCustomInterval)
              stringResource(R.string.water_interval_minutes_fmt, state.waterIntervalMinutes)
            else stringResource(R.string.water_custom_interval),
          selected = isCustomInterval,
          accentColor = waterAccent,
          onAccentColor = MaterialTheme.colorScheme.onTertiary,
          onClick = onOpenCustomInterval,
          modifier = Modifier.testTag("water_interval_custom"),
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
      Spacer(modifier = Modifier.height(12.dp))

      // Daily glass counter + quick log & test notification
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          val glassLabel =
            if (state.waterGlassesToday == 1) {
              stringResource(R.string.water_glasses_today_one, state.waterGlassesToday)
            } else {
              stringResource(R.string.water_glasses_today_other, state.waterGlassesToday)
            }
          Text(
            text = glassLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("water_glasses_count"),
          )

          Spacer(modifier = Modifier.height(6.dp))

          Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            val totalDots = 8
            for (i in 0 until totalDots) {
              val filled = i < state.waterGlassesToday
              Box(
                modifier =
                  Modifier.size(7.dp)
                    .clip(CircleShape)
                    .background(
                      if (filled) waterAccent
                      else MaterialTheme.colorScheme.outlineVariant
                    )
              )
            }
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          TextButton(
            onClick = onTestAlert,
            modifier =
              Modifier.testTag("water_test_button").minimumInteractiveComponentSize(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.NotificationsActive,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = stringResource(R.string.water_test_notification),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          FilledTonalButton(
            onClick = onLogGlass,
            modifier =
              Modifier.testTag("water_log_glass_button").minimumInteractiveComponentSize(),
            shape = RoundedCornerShape(999.dp),
            colors =
              ButtonDefaults.filledTonalButtonColors(
                containerColor = waterContainer,
                contentColor = onWaterContainer,
              ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Add,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = stringResource(R.string.water_log_glass),
              style = MaterialTheme.typography.labelLarge,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun IntervalChip(
  label: String,
  selected: Boolean,
  accentColor: androidx.compose.ui.graphics.Color,
  onAccentColor: androidx.compose.ui.graphics.Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val bg by
    animateColorAsState(
      targetValue = if (selected) accentColor else MaterialTheme.colorScheme.surfaceVariant,
      animationSpec = tween(180),
      label = "interval_chip_bg",
    )
  val fg by
    animateColorAsState(
      targetValue = if (selected) onAccentColor else MaterialTheme.colorScheme.onSurfaceVariant,
      animationSpec = tween(180),
      label = "interval_chip_fg",
    )

  Box(
    modifier =
      modifier
        .minimumInteractiveComponentSize()
        .clip(RoundedCornerShape(999.dp))
        .background(bg)
        .clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = fg,
    )
  }
}
