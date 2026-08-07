package com.lamuier.cursorusage.notification

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 15 分钟周期任务：保证后台 / 无小组件场景时常驻用量通知不过期。
 *
 * 注意：本任务只负责「定时触发刷新」，真正的通知构建与缓存读取都收敛在
 * [CursorUsageNotificationCoordinator.refreshFromCache]。这里刻意不在 Job 里直接读盘，
 * 而是把工作派发到 IO 协程并在完成后调用 [JobService.jobFinished]，避免在主线程
 * 阻塞（[CursorUsageNotificationCoordinator.refreshFromCache] 会同步读取本地缓存）。
 *
 * setPersisted(true) 让任务跨重启存活，因此宿主必须声明 RECEIVE_BOOT_COMPLETED。
 */
object NotificationRefreshScheduler {
    private const val JOB_ID = 4242
    private const val PERIOD_MS = 15L * 60 * 1000

    fun schedule(context: Context) {
        val appContext = context.applicationContext
        val scheduler =
            appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        if (scheduler.getPendingJob(JOB_ID) != null) return

        val component = ComponentName(appContext, NotificationRefreshJob::class.java)
        val builder = JobInfo.Builder(JOB_ID, component)
            .setPersisted(true)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPeriodic(PERIOD_MS)

        // 周期任务遇到低电/充电策略没必要中断，保持默认即可；仅作显式标注便于阅读。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setRequiresBatteryNotLow(false)
        }
        runCatching { scheduler.schedule(builder.build()) }
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        val scheduler =
            appContext.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        scheduler.cancel(JOB_ID)
    }
}

class NotificationRefreshJob : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                CursorUsageNotificationCoordinator
                    .get(applicationContext)
                    .refreshFromCache()
            } finally {
                jobFinished(params, false)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean = false
}
