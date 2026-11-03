package app.photowiget.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.photowiget.appContainer
import java.util.concurrent.TimeUnit

class RotationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        container.widgets.reconcile()
        container.widgets.update(container.repository.advanceDue())
        return Result.success()
    }
}

class RotationScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context)

    /** Runs the rotation every [periodMinutes], or stops it when nothing needs rotating. */
    fun sync(periodMinutes: Long?) {
        if (periodMinutes == null) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<RotationWorker>(periodMinutes, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    private companion object {
        const val WORK_NAME = "photo-rotation"
    }
}
