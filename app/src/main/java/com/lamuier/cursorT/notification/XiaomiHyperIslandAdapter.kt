package com.lamuier.cursorT.notification

import android.app.Notification
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Bundle
import com.lamuier.cursorT.R
import org.json.JSONObject

/**
 * Adds Xiaomi HyperIsland OS3 extras while retaining the normal Android notification.
 *
 * 与 ScheduleTimeline 真机验证过的做法一致：build() 之后直接往 notification.extras
 * 注入 miui.focus.* payload（param_v2 / protocol=3，对齐 HyperIsland-ToolKit 模板）。
 * 胶囊左侧为图标 + 用量标题，右侧为计费周期重置倒计时（timerType=-1）。
 */
object XiaomiHyperIslandAdapter {
    private const val BUSINESS_NAME = "cursor_pulse"
    private const val PICTURE_REF = "miui.focus.pic_cursor_pulse"
    private const val PARAM_KEY = "miui.focus.param"
    // Keep the payload protocol aligned with HyperIsland-ToolKit's OS3 template.
    private const val PARAM_V2_PROTOCOL = 3

    fun applyIfSupported(
        context: Context,
        notification: Notification,
        title: String,
        content: String,
        islandTitle: String,
        islandContent: String,
        timerSuffix: String,
        cycleEndMillis: Long?,
    ): Boolean {
        if (!XiaomiHyperIslandCapabilityReader.isSupported(context)) {
            return false
        }

        return runCatching {
            val appContext = context.applicationContext
            notification.extras.putAll(buildResourceBundle(appContext))
            notification.extras.putString(
                PARAM_KEY,
                buildJsonParam(
                    title = title,
                    content = content,
                    islandTitle = islandTitle,
                    islandContent = islandContent,
                    timerSuffix = timerSuffix,
                    cycleEndMillis = cycleEndMillis,
                    nowMillis = System.currentTimeMillis(),
                ),
            )
            true
        }.getOrDefault(false)
    }

    private fun buildResourceBundle(context: Context): Bundle = Bundle().apply {
        // Standard template mode expects image and action bundles under these keys.
        // Only register pictures the payload actually references; unreferenced
        // entries are dead weight the system never reads. The island renders this
        // icon on a dark capsule, so use the colored app icon — the monochrome
        // status-bar icon reads as a white blob.
        putBundle("miui.focus.actions", Bundle())
        putBundle("miui.focus.pics", Bundle().apply {
            val icon = Icon.createWithResource(context, R.drawable.ic_island)
            putParcelable(PICTURE_REF, icon)
        })
    }

    /**
     * Minimal ParamV2 payload matching HyperIsland-ToolKit's standard template output.
     * The timer counts down to the billing-cycle reset (timerType=-1): timerWhen =
     * cycle end timestamp, timerTotal / timerSystemCurrent = payload build time.
     */
    internal fun buildJsonParam(
        title: String,
        content: String,
        islandTitle: String,
        islandContent: String,
        timerSuffix: String,
        cycleEndMillis: Long?,
        nowMillis: Long = System.currentTimeMillis(),
    ): String {
        // 计费周期结束时间缺失时跳过倒计时，超级岛仍展示左侧图标 + 用量胶囊。
        val timer = if (cycleEndMillis != null) JSONObject().apply {
            put("timerType", -1)
            put("timerWhen", cycleEndMillis)
            put("timerTotal", nowMillis)
            put("timerSystemCurrent", nowMillis)
        } else null
        val picInfo = JSONObject().apply {
            put("type", 1)
            put("pic", PICTURE_REF)
            put("loop", false)
            put("autoplay", false)
            put("number", 0)
        }
        val paramIsland = JSONObject().apply {
            put("islandProperty", 1)
            put("islandPriority", 2)
            put("islandOrder", false)
            put("dismissIsland", false)
            put("maxSize", false)
            put("needCloseAnimation", true)
            put("bigIslandArea", JSONObject().apply {
                // Official capsule template "图文组件1 + 等宽数字文本组件":
                // A area (left of the camera cutout) = icon + title + trailing note;
                // B area (right) = equal-width timer digits + suffix text.
                put("imageTextInfoLeft", JSONObject().apply {
                    put("type", 1)
                    put("picInfo", picInfo)
                    put("textInfo", JSONObject().apply {
                        put("title", islandTitle)
                        if (islandContent.isNotEmpty()) {
                            put("content", islandContent)
                        }
                        put("showHighlightColor", false)
                    })
                })
                put("sameWidthDigitInfo", JSONObject().apply {
                    timer?.let { put("timerInfo", it) }
                    if (timerSuffix.isNotEmpty()) {
                        put("content", timerSuffix)
                    }
                    put("showHighlightColor", true)
                    put("turnAnim", false)
                })
            })
            put("smallIslandArea", JSONObject().apply {
                put("picInfo", picInfo)
            })
        }
        val baseInfo = JSONObject().apply {
            put("type", 1)
            put("title", title)
            put("subTitle", islandContent)
            put("content", content)
            put("picFunction", PICTURE_REF)
        }
        val paramV2 = JSONObject().apply {
            put("protocol", PARAM_V2_PROTOCOL)
            put("business", BUSINESS_NAME)
            put("updatable", true)
            put("ticker", title)
            put("enableFloat", false)
            put("isShowNotification", true)
            put("islandFirstFloat", false)
            put("param_island", paramIsland)
            put("baseInfo", baseInfo)
        }
        return JSONObject().apply {
            put("param_v2", paramV2)
            put("isShowNotification", true)
        }.toString()
    }
}
