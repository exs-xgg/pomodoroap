package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.CadenceScreen
import com.example.ui.CadenceViewModel
import com.example.ui.theme.CadenceTheme

class MainActivity : ComponentActivity() {
  private val viewModel: CadenceViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      CadenceTheme {
        CadenceScreen(viewModel = viewModel)
      }
    }
  }
}
