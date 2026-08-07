package com.lamuier.cursorusage.util

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object DeviceCredentialGate {
    private const val AUTHENTICATORS = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

    fun canAuthenticate(activity: FragmentActivity): Boolean {
        val manager = BiometricManager.from(activity)
        return when (manager.canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    fun availabilityMessage(activity: FragmentActivity): String {
        return when (BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> "可用"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                "请先在系统设置中添加指纹、面部或锁屏密码/图案"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                "当前设备无法进行生物识别或锁屏验证"
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                "系统安全状态异常，暂时无法验证"
            else -> "当前无法验证设备身份"
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCanceled: () -> Unit = {},
    ) {
        if (!canAuthenticate(activity)) {
            onError(availabilityMessage(activity))
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_CANCELED -> onCanceled()
                        else -> onError(errString.toString().ifBlank { "验证失败" })
                    }
                }

                override fun onAuthenticationFailed() {
                    // Keep the prompt open; system will show retry feedback.
                }
            },
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setAllowedAuthenticators(AUTHENTICATORS)
                } else {
                    @Suppress("DEPRECATION")
                    setDeviceCredentialAllowed(true)
                }
            }
            .build()

        prompt.authenticate(promptInfo)
    }
}
