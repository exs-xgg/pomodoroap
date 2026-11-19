package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.background.NotificationHelper
import com.example.data.CadenceRepository
import com.example.model.CadenceState
import com.example.model.PomodoroPhase
import com.example.ui.CadenceUiState
import com.example.ui.components.PomodoroDialSection
import com.example.ui.theme.CadenceTheme
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CadenceLogicTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun `notification channels are registered on startup`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    NotificationHelper.createChannels(context)
    val manager = context.getSystemService(NotificationManager::class.java)
    assertNotNull(manager.getNotificationChannel(NotificationHelper.CHANNEL_POMODORO_ONGOING))
    assertNotNull(manager.getNotificationChannel(NotificationHelper.CHANNEL_POMODORO_ALERTS))
    assertNotNull(manager.getNotificationChannel(NotificationHelper.CHANNEL_WATER_REMINDERS))
  }

  @Test
  fun `pomodoro phase advances through 4 focus sessions to long break`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = CadenceRepository(context)
    repo.resetTimer(PomodoroPhase.FOCUS)

    repeat(3) {
      val (finished, next) = repo.advancePhaseAfterCompletion(countAsCompletedFocus = true)
      assertEquals(PomodoroPhase.FOCUS, finished)
      assertEquals(PomodoroPhase.SHORT_BREAK, next)

      val (finishedBreak, nextFocus) = repo.advancePhaseAfterCompletion(countAsCompletedFocus = false)
      assertEquals(PomodoroPhase.SHORT_BREAK, finishedBreak)
      assertEquals(PomodoroPhase.FOCUS, nextFocus)
    }

    val (fourthFocus, longBreak) = repo.advancePhaseAfterCompletion(countAsCompletedFocus = true)
    assertEquals(PomodoroPhase.FOCUS, fourthFocus)
    assertEquals(PomodoroPhase.LONG_BREAK, longBreak)
    assertEquals(0, repo.currentState().completedFocusInCycle)
  }

  @Test
  fun `water reminder state and daily glasses update cleanly`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = CadenceRepository(context)

    repo.setWaterInterval(30, nextTriggerWallMillis = 123_456L)
    repo.setWaterReminder(enabled = true, nextTriggerWallMillis = 123_456L)
    repo.incrementWaterGlass()
    repo.incrementWaterGlass()

    val state = repo.currentState()
    assertTrue(state.waterEnabled)
    assertEquals(30, state.waterIntervalMinutes)
    assertEquals(2, state.waterGlassesToday)

    repo.setWaterReminder(enabled = false, nextTriggerWallMillis = 0L)
    assertFalse(repo.currentState().waterEnabled)
    assertEquals(CadenceState.DEFAULT_FOCUS_MINUTES, repo.currentState().focusMinutes)
  }

  @Test
  fun `pomodoro dial collapses to simplified controls when timer starts`() {
    var running by mutableStateOf(false)
    composeRule.setContent {
      CadenceTheme {
        PomodoroDialSection(
          uiState = CadenceUiState(state = CadenceState(isRunning = running)),
          onSelectPhase = {},
          onToggleStartPause = { running = !running },
          onReset = { running = false },
          onSkip = {},
          onOpenDurationSettings = {},
        )
      }
    }

    composeRule.onNodeWithTag("pomodoro_expanded_section").assertIsDisplayed()
    composeRule.onNodeWithTag("pomodoro_skip_button").assertIsDisplayed()
    composeRule.onNodeWithTag("pomodoro_collapsed_card").assertDoesNotExist()

    composeRule.onNodeWithTag("pomodoro_primary_button").performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag("pomodoro_collapsed_card").assertIsDisplayed()
    composeRule.onNodeWithTag("pomodoro_primary_button").assertIsDisplayed()
    composeRule.onNodeWithTag("pomodoro_reset_button").assertIsDisplayed()
    composeRule.onNodeWithTag("pomodoro_expanded_section").assertDoesNotExist()
    composeRule.onNodeWithTag("pomodoro_skip_button").assertDoesNotExist()
  }
}
