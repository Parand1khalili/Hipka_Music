package com.hipka.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HipkaApplication : Application(), Configuration.Provider {

    // بدون این WorkerFactory، ورکرهای @HiltWorker نمی‌توانند وابستگی‌هایشان (مثل SongDao) را تزریق کنند
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
