package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationSettingsSheet(
  focusMinutes: Int,
  shortBreakMinutes: Int,
  longBreakMinutes: Int,
  onSave: (focus: Int, shortBreak: Int, longBreak: Int) -> Unit,
  onDismiss: () -> Unit,
) {
  var focus by remember(focusMinutes) { mutableIntStateOf(focusMinutes) }
  var shortBreak by remember(shortBreakMinutes) { mutableIntStateOf(shortBreakMinutes) }
  var longBreak by remember(longBreakMinutes) { mutableIntStateOf(longBreakMinutes) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = stringResource(R.string.pomodoro_settings_title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )

      DurationStepperRow(
        label = stringResource(R.string.focus_duration_label),
        minutes = focus,
        minMinutes = 5,
        maxMinutes = 90,
        step = 5,
        onValueChange = { focus = it },
      )

      DurationStepperRow(
        label = stringResource(R.string.short_break_duration_label),
        minutes = shortBreak,
        minMinutes = 1,
        maxMinutes = 30,
        step = 1,
        onValueChange = { shortBreak = it },
      )

      DurationStepperRow(
        label = stringResource(R.string.long_break_duration_label),
        minutes = longBreak,
        minMinutes = 5,
        maxMinutes = 60,
        step = 5,
        onValueChange = { longBreak = it },
      )

      Spacer(modifier = Modifier.height(4.dp))

      Button(
        onClick = {
          onSave(focus, shortBreak, longBreak)
          onDismiss()
        },
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_durations_button"),
        shape = RoundedCornerShape(999.dp),
      ) {
        Text(
          text = stringResource(R.string.done),
          style = MaterialTheme.typography.titleMedium,
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomWaterIntervalSheet(
  currentIntervalMinutes: Int,
  onSave: (Int) -> Unit,
  onDismiss: () -> Unit,
) {
  var minutes by remember(currentIntervalMinutes) { mutableIntStateOf(currentIntervalMinutes) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = stringResource(R.string.water_custom_sheet_title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )

      DurationStepperRow(
        label = stringResource(R.string.water_title),
        minutes = minutes,
        minMinutes = 5,
        maxMinutes = 180,
        step = 5,
        onValueChange = { minutes = it },
      )

      Button(
        onClick = {
          onSave(minutes)
          onDismiss()
        },
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_water_interval_button"),
        shape = RoundedCornerShape(999.dp),
      ) {
        Text(
          text = stringResource(R.string.done),
          style = MaterialTheme.typography.titleMedium,
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun DurationStepperRow(
  label: String,
  minutes: Int,
  minMinutes: Int,
  maxMinutes: Int,
  step: Int,
  onValueChange: (Int) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurface,
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
      FilledTonalIconButton(
        onClick = { onValueChange((minutes - step).coerceAtLeast(minMinutes)) },
        enabled = minutes > minMinutes,
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Icon(imageVector = Icons.Rounded.Remove, contentDescription = "Decrease $label")
      }

      Spacer(modifier = Modifier.width(12.dp))

      Text(
        text = stringResource(R.string.minutes_unit, minutes),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )

      Spacer(modifier = Modifier.width(12.dp))

      FilledTonalIconButton(
        onClick = { onValueChange((minutes + step).coerceAtMost(maxMinutes)) },
        enabled = minutes < maxMinutes,
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Icon(imageVector = Icons.Rounded.Add, contentDescription = "Increase $label")
      }
    }
  }
}
