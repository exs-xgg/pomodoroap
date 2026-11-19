package com.example

import android.app.Application
import com.example.background.NotificationHelper
import com.example.data.CadenceRepository

class CadenceApplication : Application() {
  val repository: CadenceRepository by lazy { CadenceRepository(this) }

  override fun onCreate() {
    super.onCreate()
    NotificationHelper.createChannels(this)
  }
}
