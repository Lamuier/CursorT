package com.lamuier.cursorT.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 开机 / 应用更新后恢复常驻用量监控通知与周期刷新任务。
 */
class UsageNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            -> {
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        CursorUsageNotificationCoordinator
                            .get(context.applicationContext)
                            .refreshFromCache()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
