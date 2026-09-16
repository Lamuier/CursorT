package com.lamuier.cursorT

import android.app.Application
import android.content.Context
import com.lamuier.cursorT.util.AppLocale
import com.lamuier.cursorT.widget.CursorTWidgetUpdater

class CursorTApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        CursorTWidgetUpdater.ensurePeriodicRefresh(this)
    }
}
