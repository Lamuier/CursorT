package com.lamuier.cursorusage.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle

object SensitiveContent {
    private const val CLIP_LABEL = "Cursor Access Token"

    /**
     * 复制敏感文本（如 Access Token）到系统剪贴板。
     * Android 13+ 标记 EXTRA_IS_SENSITIVE，系统剪贴板预览仅显示
     * 「已复制敏感内容」占位，不再直接展示明文。
     */
    fun copyToClipboard(context: Context, text: String) {
        val clip = ClipData.newPlainText(CLIP_LABEL, text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
    }
}
