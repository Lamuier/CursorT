package com.lamuier.cursorT.util

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.annotation.StringRes
import com.lamuier.cursorT.R
import java.util.Locale

/** 应用界面语言。默认跟随系统；无对应翻译时回退到中文资源。 */
enum class AppLanguage(val storageKey: String, @StringRes val labelRes: Int) {
    System("system", R.string.language_system),
    Chinese("zh", R.string.language_zh),
    English("en", R.string.language_en),
    ;

    companion object {
        fun fromStorage(value: String?): AppLanguage {
            val raw = value?.trim().orEmpty()
            if (raw.isEmpty() || raw.equals(System.storageKey, ignoreCase = true)) return System
            return entries.firstOrNull { it.storageKey.equals(raw, ignoreCase = true) } ?: System
        }
    }
}

object AppLocale {
    const val PREFERENCES_NAME = "cursor_pulse_dashboard_v1"
    const val KEY_LANGUAGE = "app_language"

    fun stored(context: Context): AppLanguage {
        val holder = runCatching { context.applicationContext }.getOrNull() ?: context
        return AppLanguage.fromStorage(
            holder.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE, AppLanguage.System.storageKey),
        )
    }

    fun wrap(context: Context): Context {
        val locale = localeFor(stored(context))
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }

    fun string(context: Context, @StringRes id: Int, vararg formatArgs: Any): String {
        val wrapped = wrap(context)
        return if (formatArgs.isEmpty()) {
            wrapped.getString(id)
        } else {
            wrapped.getString(id, *formatArgs)
        }
    }

    private fun localeFor(language: AppLanguage): Locale = when (language) {
        AppLanguage.Chinese -> Locale.SIMPLIFIED_CHINESE
        AppLanguage.English -> Locale.ENGLISH
        AppLanguage.System -> LocaleList.getDefault().get(0) ?: Locale.getDefault()
    }
}
