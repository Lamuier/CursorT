package com.lamuier.cursorT.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent

/**
 * 用 Chrome Custom Tabs 打开已校验的 Cursor / GitHub 页面。
 * Custom Tabs 走系统浏览器 Cookie，因此完整对话依赖用户已在 Chrome 登录 Cursor。
 */
object CursorCustomTabs {
    fun open(context: Context, url: String, toolbarColor: Int? = null): Boolean {
        if (!AgentTaskPresentation.isAllowedCustomTabUrl(url)) return false
        val uri = Uri.parse(url)
        return try {
            launchCustomTab(context, uri, toolbarColor)
            true
        } catch (_: ActivityNotFoundException) {
            openExternal(context, uri)
        } catch (_: Exception) {
            openExternal(context, uri)
        }
    }

    private fun launchCustomTab(context: Context, uri: Uri, toolbarColor: Int?) {
        val builder = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setShareState(CustomTabsIntent.SHARE_STATE_ON)
            .setUrlBarHidingEnabled(true)
            .setColorScheme(CustomTabsIntent.COLOR_SCHEME_SYSTEM)
        if (toolbarColor != null) {
            val colors = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(toolbarColor)
                .build()
            builder.setDefaultColorSchemeParams(colors)
        }
        val customTabs = builder.build()
        CustomTabsClient.getPackageName(context, null)?.let { packageName ->
            customTabs.intent.setPackage(packageName)
        }
        if (context !is Activity) {
            customTabs.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        customTabs.launchUrl(context, uri)
    }

    private fun openExternal(context: Context, uri: Uri): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
