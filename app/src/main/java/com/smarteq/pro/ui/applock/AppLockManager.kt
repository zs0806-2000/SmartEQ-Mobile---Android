package com.smarteq.pro.ui.applock

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.smarteq.pro.data.repository.SettingsRepository
import com.smarteq.pro.data.security.SecureKeyStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * SmartyQ's App Lock. Supports Biometric / device credential
 * or in-app numeric PIN (salted SHA-256).
 */
class AppLockManager(
    private val secureKeyStore: SecureKeyStore,
    private val settingsRepository: SettingsRepository,
) {
    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked

    private var lastBackgroundedAtMs: Long = 0L

    fun canUseBiometrics(activity: FragmentActivity): Boolean {
        val manager = BiometricManager.from(activity)
        val result = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun promptBiometric(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                unlock()
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(com.smarteq.pro.R.string.lock_biometric_title))
            .setSubtitle(activity.getString(com.smarteq.pro.R.string.lock_biometric_subtitle))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }

    fun hasPinConfigured(): Boolean = secureKeyStore.getAppLockPinHash() != null

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPin(pin, salt)
        secureKeyStore.setAppLockPin(hash, Base64.getEncoder().encodeToString(salt))
    }

    fun clearPin() = secureKeyStore.setAppLockPin(null, null)

    fun verifyPin(pin: String): Boolean {
        val storedHash = secureKeyStore.getAppLockPinHash() ?: return false
        val storedSalt = secureKeyStore.getAppLockPinSalt() ?: return false
        val salt = Base64.getDecoder().decode(storedSalt)
        return hashPin(pin, salt) == storedHash
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hashed = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hashed)
    }

    fun unlock() {
        _isLocked.value = false
    }

    fun lockNow() {
        _isLocked.value = true
    }

    /** Call from Activity.onStop(). */
    fun onAppBackgrounded() {
        lastBackgroundedAtMs = System.currentTimeMillis()
    }

    /** Call from Activity.onStart(); re-locks if the configured grace period has elapsed. */
    suspend fun onAppForegrounded() {
        val lockEnabled = settingsRepository.appLockEnabled.first()
        if (!lockEnabled) {
            unlock()
            return
        }
        val timeoutSeconds = settingsRepository.appLockTimeoutSeconds.first()
        val elapsedSeconds = (System.currentTimeMillis() - lastBackgroundedAtMs) / 1000
        if (lastBackgroundedAtMs == 0L || elapsedSeconds >= timeoutSeconds) {
            lockNow()
        }
    }
}
