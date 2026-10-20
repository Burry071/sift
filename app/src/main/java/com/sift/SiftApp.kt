package com.sift

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sift.engine.ModelRuntime
import com.sift.service.FilingWorker
import java.util.concurrent.TimeUnit

class SiftApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ModelRuntime.start(this)

        // The sloth's schedule: whenever the phone is charging and idle —
        // typically overnight — drain the drawer.
        val overnight = PeriodicWorkRequestBuilder<FilingWorker>(6, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresCharging(true)
                    .setRequiresDeviceIdle(true)
                    .build()
            )
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            FilingWorker.OVERNIGHT_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            overnight,
        )
    }

    companion object {
        // FILE NOW: same code path as the night job, minus the waiting.
        fun fileNow(context: android.content.Context) {
            val request = OneTimeWorkRequestBuilder<FilingWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                FilingWorker.UNIQUE_NAME,
                ExistingWorkPolicy.KEEP,
                request,
            )
        }
    }
}
