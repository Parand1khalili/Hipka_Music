package com.hipka.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HipkaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // تنظیمات اولیه برنامه در اینجا قرار می‌گیرد
    }
}