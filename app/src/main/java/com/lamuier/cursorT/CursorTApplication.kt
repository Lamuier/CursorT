package com.lamuier.cursorT

import android.app.Application
import android.content.Context
import com.lamuier.cursorT.util.AppLocale

class CursorTApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.wrap(base))
    }
}
