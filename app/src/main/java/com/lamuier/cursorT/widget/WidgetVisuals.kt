package com.lamuier.cursorT.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.TypedValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal object WidgetVisuals {
    /** Match Stats-style cards: pronounced, soft rounded corners. */
    private const val CORNER_RADIUS_DP = 32f
    private const val DONUT_STROKE_DP = 12f
    private const val CHIP_RADIUS_DP = 999f
    private const val GLASS_ALPHA_DARK = 0xE3
    private const val GLASS_ALPHA_LIGHT = 0xF2

    fun glassSurface(color: Int): Int {
        val luminance =
            (0.299f * android.graphics.Color.red(color) +
                0.587f * android.graphics.Color.green(color) +
                0.114f * android.graphics.Color.blue(color)) / 255f
        val alpha = if (luminance < 0.5f) GLASS_ALPHA_DARK else GLASS_ALPHA_LIGHT
        return android.graphics.Color.argb(
            alpha,
            android.graphics.Color.red(color),
            android.graphics.Color.green(color),
            android.graphics.Color.blue(color),
        )
    }

    fun surfaceBitmap(
        context: Context,
        widgetId: Int,
        color: Int,
        fallbackWidthDp: Int,
        fallbackHeightDp: Int,
    ): Bitmap {
        val (widthPx, heightPx) = widgetSizePx(context, widgetId, fallbackWidthDp, fallbackHeightDp)
        return roundRectBitmap(widthPx, heightPx, color, dp(context, CORNER_RADIUS_DP))
    }

    fun donutBitmap(
        context: Context,
        sizeDp: Int,
        progress: Int,
        progressColor: Int,
        trackColor: Int,
        strokeDp: Float = DONUT_STROKE_DP,
    ): Bitmap {
        val size = dp(context, sizeDp.toFloat()).roundToInt().coerceAtLeast(1)
        val stroke = dp(context, strokeDp)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val inset = stroke / 2f + dp(context, 1f)
        val oval = RectF(inset, inset, size - inset, size - inset)
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            color = trackColor
        }
        val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            color = progressColor
        }
        canvas.drawArc(oval, -90f, 360f, false, trackPaint)
        val sweep = (progress.coerceIn(0, 100) / 100f) * 360f
        if (sweep > 0.5f) {
            canvas.drawArc(oval, -90f, sweep, false, progressPaint)
        }
        return bitmap
    }

    fun chipBitmap(
        context: Context,
        color: Int,
        widthDp: Int,
        heightDp: Int,
    ): Bitmap {
        val width = dp(context, widthDp.toFloat()).roundToInt().coerceAtLeast(1)
        val height = dp(context, heightDp.toFloat()).roundToInt().coerceAtLeast(1)
        return roundRectBitmap(width, height, color, dp(context, CHIP_RADIUS_DP))
    }

    fun refreshIconBitmap(
        context: Context,
        color: Int,
        sizeDp: Int = 18,
    ): Bitmap {
        val size = dp(context, sizeDp.toFloat()).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(context, 1.8f)
            strokeCap = Paint.Cap.ROUND
            this.color = color
        }
        val inset = dp(context, 3f)
        val oval = RectF(inset, inset, size - inset, size - inset)
        canvas.drawArc(oval, 40f, 260f, false, paint)
        val tipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            this.color = color
        }
        val cx = size / 2f + dp(context, 4.5f)
        val cy = inset + dp(context, 1.5f)
        canvas.drawCircle(cx, cy, dp(context, 1.6f), tipPaint)
        return bitmap
    }

    private fun roundRectBitmap(
        widthPx: Int,
        heightPx: Int,
        color: Int,
        radiusPx: Float,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val radius = min(radiusPx, min(widthPx, heightPx) / 2f)
        canvas.drawRoundRect(
            RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat()),
            radius,
            radius,
            paint,
        )
        return bitmap
    }

    private fun widgetSizePx(
        context: Context,
        widgetId: Int,
        fallbackWidthDp: Int,
        fallbackHeightDp: Int,
    ): Pair<Int, Int> {
        val density = context.resources.displayMetrics.density
        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(widgetId)
        val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        // Prefer the mid-point of min/max so corner radius isn't crushed when
        // launchers report oversized MAX_* bounds and the bitmap is fitXY-scaled.
        val widthDp = averageSizeDp(minW, maxW, fallbackWidthDp)
        val heightDp = averageSizeDp(minH, maxH, fallbackHeightDp)
        return Pair(
            (widthDp * density).roundToInt().coerceAtLeast(1),
            (heightDp * density).roundToInt().coerceAtLeast(1),
        )
    }

    private fun averageSizeDp(minDp: Int, maxDp: Int, fallbackDp: Int): Int {
        val candidates = listOf(minDp, maxDp).filter { it > 0 }
        if (candidates.isEmpty()) return fallbackDp
        return max(candidates.average().roundToInt(), fallbackDp)
    }

    private fun dp(context: Context, value: Float): Float =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            context.resources.displayMetrics,
        )
}
