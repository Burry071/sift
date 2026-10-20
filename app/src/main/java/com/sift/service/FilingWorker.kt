package com.sift.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.sift.R
import com.sift.data.Db
import com.sift.engine.ModelRuntime
import com.sift.engine.SiftEngine
import com.sift.pipeline.SiftPipeline

// Drains the UNFILED queue one snippet at a time. Enqueued two ways:
// the overnight periodic job (charging + idle constraints) and FILE NOW
// (no constraints). One DB update per snippet, so partial progress
// survives interruptions; an engine failure leaves the rest UNFILED
// for the next night.
class FilingWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val status = SiftEngine.modelStatus.value
        if (status != SiftEngine.ModelStatus.READY) {
            // LOADING: come back when the model is in. MISSING: nothing to do.
            return if (status == SiftEngine.ModelStatus.LOADING) Result.retry() else Result.failure()
        }
        val engine = SiftEngine.engineProvider() ?: return Result.failure()

        try {
            setForeground(foregroundInfo())
        } catch (t: Throwable) {
            // Foreground promotion can be refused; the queue still drains
            // until the background time limit, and resumes next night.
        }

        SiftEngine.filing.value = true
        try {
            val dao = Db.get(applicationContext).dao()
            val pipeline = SiftPipeline(engine)
            while (true) {
                val next = dao.nextUnfiled() ?: break
                val filed = pipeline.process(next.text) // throws on engine failure
                dao.file(next.id, filed.category, filed.title, filed.titleIsFallback, System.currentTimeMillis())
            }
            return Result.success()
        } catch (t: Throwable) {
            return Result.retry()
        } finally {
            SiftEngine.filing.value = false
        }
    }

    private fun foregroundInfo(): ForegroundInfo {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Filing", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val notification: Notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Sift")
            .setContentText("Filing your drawer while you sleep.")
            .setOngoing(true)
            .build()
        return if (Build.VERSION.SDK_INT >= 29) {
            ForegroundInfo(NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val UNIQUE_NAME = "filing"
        const val OVERNIGHT_NAME = "overnight-filing"
        private const val CHANNEL_ID = "sift_filing"
        private const val NOTIFICATION_ID = 10
        private const val FOREGROUND_SERVICE_TYPE_DATA_SYNC = 1 // android.content.pm.ServiceInfo.DATA_SYNC
    }
}
