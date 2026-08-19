package com.lamuier.cursorT.notification

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings

data class XiaomiHyperIslandCapability(
    val isXiaomiDevice: Boolean,
    val protocol: Int,
    val islandFeatureEnabled: Boolean,
    val focusPermissionGranted: Boolean,
) {
    /**
     * HyperIsland candidate detection deliberately does not require Focus whitelist
     * permission. HyperOS versions in the wild may render the private extras even
     * when the compatibility provider reports false.
     *
     * 不再硬卡 [islandFeatureEnabled]（依赖反射读取 `persist.sys.feature.island`）。
     * 该属性在不少 HyperOS 设备上读不到或返回 false，会把本应支持的机型误判为不支持，
     * 导致超级岛 payload 永不注入、永远上不了岛。去掉该门槛后，只要是小米的非 1/2
     * 协议设备就尝试注入；payload 仅是 notification extras，不支持的机型会直接忽略，
     * 无副作用（与上方注释“兼容层报 false 真机仍可能渲染”一致）。
     */
    val isOs3Supported: Boolean
        get() = isXiaomiDevice &&
            protocol != 1 &&
            protocol != 2

    companion object {
        const val OS3_PROTOCOL = 3
    }
}

/** Reads Xiaomi's public compatibility signals without making the app Xiaomi-only. */
object XiaomiHyperIslandCapabilityReader {
    private const val PROTOCOL_SETTING = "notification_focus_protocol"
    private const val ISLAND_PROPERTY = "persist.sys.feature.island"
    private const val FOCUS_URI = "content://miui.statusbar.notification.public"

    fun inspect(context: Context): XiaomiHyperIslandCapability {
        val appContext = context.applicationContext
        val xiaomiDevice = isXiaomiDevice()
        return XiaomiHyperIslandCapability(
            isXiaomiDevice = xiaomiDevice,
            protocol = if (xiaomiDevice) readProtocol(appContext) else 0,
            islandFeatureEnabled = xiaomiDevice && readIslandFeature(),
            focusPermissionGranted = xiaomiDevice && canShowFocus(appContext),
        )
    }

    fun isSupported(context: Context): Boolean = inspect(context).isOs3Supported

    private fun isXiaomiDevice(): Boolean {
        return Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
            Build.MANUFACTURER.equals("Redmi", ignoreCase = true) ||
            Build.MANUFACTURER.equals("POCO", ignoreCase = true)
    }

    private fun readProtocol(context: Context): Int = runCatching {
        Settings.System.getInt(context.contentResolver, PROTOCOL_SETTING, 0)
    }.getOrDefault(0)

    @SuppressLint("PrivateApi")
    private fun readIslandFeature(): Boolean {
        return runCatching {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val getBoolean = systemProperties.getDeclaredMethod(
                "getBoolean",
                String::class.java,
                Boolean::class.javaPrimitiveType,
            )
            getBoolean.isAccessible = true
            getBoolean.invoke(null, ISLAND_PROPERTY, false) as Boolean
        }.getOrElse {
            runCatching {
                val systemProperties = Class.forName("android.os.SystemProperties")
                val get = systemProperties.getDeclaredMethod("get", String::class.java)
                get.isAccessible = true
                when ((get.invoke(null, ISLAND_PROPERTY) as? String)?.trim()?.lowercase()) {
                    "1", "true", "yes", "y", "on" -> true
                    else -> false
                }
            }.getOrDefault(false)
        }
    }

    private fun canShowFocus(context: Context): Boolean = runCatching {
        val result = context.contentResolver.call(
            Uri.parse(FOCUS_URI),
            "canShowFocus",
            null,
            Bundle().apply { putString("package", context.packageName) },
        )
        result?.getBoolean("canShowFocus", false) == true
    }.getOrDefault(false)
}
