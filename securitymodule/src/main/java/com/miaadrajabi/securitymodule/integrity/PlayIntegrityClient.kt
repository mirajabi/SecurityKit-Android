package com.miaadrajabi.securitymodule.integrity

import android.content.Context
import android.content.pm.PackageManager
// Play Integrity API imports are handled via reflection to avoid compile-time dependency
import com.miaadrajabi.securitymodule.SecurityFinding
import com.miaadrajabi.securitymodule.Severity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/**
 * Play Integrity API client with graceful fallback when Google Play Services is unavailable.
 * 
 * This client attempts to use Google Play Integrity API to verify device and app integrity.
 * If Google Play Services is not available, it returns appropriate fallback signals.
 */
object PlayIntegrityClient {

    @Serializable
    data class IntegrityResult(
        val deviceIntegrity: DeviceIntegrityStatus,
        val appIntegrity: AppIntegrityStatus,
        val accountDetails: AccountDetailsStatus,
        val token: String? = null,
        val error: String? = null
    )

    @Serializable
    enum class DeviceIntegrityStatus {
        MEETS_DEVICE_INTEGRITY,
        MEETS_BASIC_INTEGRITY,
        MEETS_STRONG_INTEGRITY,
        UNKNOWN,
        UNAVAILABLE
    }

    @Serializable
    enum class AppIntegrityStatus {
        MEETS_APP_INTEGRITY,
        UNKNOWN,
        UNAVAILABLE
    }

    @Serializable
    enum class AccountDetailsStatus {
        LICENSED,
        UNLICENSED,
        UNKNOWN,
        UNAVAILABLE
    }

    /**
     * Check Play Integrity with graceful fallback.
     * 
     * @param context Android context
     * @param nonce Unique nonce for this request
     * @return IntegrityResult with status or fallback information
     */
    suspend fun checkIntegrity(context: Context, nonce: String): IntegrityResult = withContext(Dispatchers.IO) {
        try {
            // Check if Google Play Services is available
            if (!isGooglePlayServicesAvailable(context)) {
                return@withContext IntegrityResult(
                    deviceIntegrity = DeviceIntegrityStatus.UNAVAILABLE,
                    appIntegrity = AppIntegrityStatus.UNAVAILABLE,
                    accountDetails = AccountDetailsStatus.UNAVAILABLE,
                    error = "Google Play Services not available"
                )
            }

            // Use reflection to access Play Integrity API
            val integrityManagerFactoryClass = Class.forName("com.google.android.play.core.integrity.IntegrityManagerFactory")
            val createMethod = integrityManagerFactoryClass.getMethod("create", Context::class.java)
            val integrityManager = createMethod.invoke(null, context)

            val requestBuilderClass = Class.forName("com.google.android.play.core.integrity.IntegrityTokenRequest\$Builder")
            @Suppress("DEPRECATION")
            val builder = requestBuilderClass.newInstance()
            val setNonceMethod = requestBuilderClass.getMethod("setNonce", String::class.java)
            setNonceMethod.invoke(builder, nonce)
            val buildMethod = requestBuilderClass.getMethod("build")
            val request = buildMethod.invoke(builder)

            val requestIntegrityTokenMethod = integrityManager.javaClass.getMethod("requestIntegrityToken", request.javaClass)
            val response = requestIntegrityTokenMethod.invoke(integrityManager, request)
            
            val tokenMethod = response.javaClass.getMethod("token")
            val token = tokenMethod.invoke(response) as String

            // Parse the token to extract integrity verdicts
            parseIntegrityToken(token)

        } catch (e: Exception) {
            IntegrityResult(
                deviceIntegrity = DeviceIntegrityStatus.UNKNOWN,
                appIntegrity = AppIntegrityStatus.UNKNOWN,
                accountDetails = AccountDetailsStatus.UNKNOWN,
                error = "Play Integrity check failed: ${e.message}"
            )
        }
    }

    /**
     * Check if Google Play Services is available on the device.
     */
    private fun isGooglePlayServicesAvailable(context: Context): Boolean {
        return try {
            val packageManager = context.packageManager
            packageManager.getPackageInfo("com.google.android.gms", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Parse the integrity token to extract verdicts.
     * Note: In a real implementation, you would need to verify the token signature
     * and parse the JWT payload. This is a simplified version.
     */
    private fun parseIntegrityToken(token: String): IntegrityResult {
        // In a real implementation, you would:
        // 1. Verify the JWT signature using Google's public keys
        // 2. Parse the payload to extract deviceIntegrity, appIntegrity, accountDetails
        // 3. Return the appropriate status based on the verdicts
        
        // For now, return a placeholder that indicates the token was received
        return IntegrityResult(
            deviceIntegrity = DeviceIntegrityStatus.UNKNOWN,
            appIntegrity = AppIntegrityStatus.UNKNOWN,
            accountDetails = AccountDetailsStatus.UNKNOWN,
            token = token
        )
    }

    /**
     * Convert IntegrityResult to SecurityFinding for policy evaluation.
     */
    fun toSecurityFindings(result: IntegrityResult): List<SecurityFinding> {
        val findings = mutableListOf<SecurityFinding>()

        when (result.deviceIntegrity) {
            DeviceIntegrityStatus.UNAVAILABLE -> {
                findings.add(SecurityFinding(
                    id = "play_integrity_unavailable",
                    title = "Play Integrity API Unavailable",
                    severity = Severity.WARN,
                    metadata = mapOf("message" to "Google Play Services not available for device integrity verification")
                ))
            }
            DeviceIntegrityStatus.UNKNOWN -> {
                findings.add(SecurityFinding(
                    id = "play_integrity_unknown",
                    title = "Play Integrity Status Unknown",
                    severity = Severity.WARN,
                    metadata = mapOf("message" to "Could not determine device integrity status")
                ))
            }
            DeviceIntegrityStatus.MEETS_BASIC_INTEGRITY -> {
                findings.add(SecurityFinding(
                    id = "play_integrity_basic",
                    title = "Device Meets Basic Integrity",
                    severity = Severity.OK,
                    metadata = mapOf("message" to "Device passes basic integrity checks")
                ))
            }
            DeviceIntegrityStatus.MEETS_DEVICE_INTEGRITY -> {
                findings.add(SecurityFinding(
                    id = "play_integrity_device",
                    title = "Device Meets Device Integrity",
                    severity = Severity.OK,
                    metadata = mapOf("message" to "Device passes device integrity checks")
                ))
            }
            DeviceIntegrityStatus.MEETS_STRONG_INTEGRITY -> {
                findings.add(SecurityFinding(
                    id = "play_integrity_strong",
                    title = "Device Meets Strong Integrity",
                    severity = Severity.OK,
                    metadata = mapOf("message" to "Device passes strong integrity checks")
                ))
            }
        }

        when (result.appIntegrity) {
            AppIntegrityStatus.UNAVAILABLE -> {
                findings.add(SecurityFinding(
                    id = "play_app_integrity_unavailable",
                    title = "Play App Integrity Unavailable",
                    severity = Severity.WARN,
                    metadata = mapOf("message" to "Google Play Services not available for app integrity verification")
                ))
            }
            AppIntegrityStatus.UNKNOWN -> {
                findings.add(SecurityFinding(
                    id = "play_app_integrity_unknown",
                    title = "Play App Integrity Unknown",
                    severity = Severity.WARN,
                    metadata = mapOf("message" to "Could not determine app integrity status")
                ))
            }
            AppIntegrityStatus.MEETS_APP_INTEGRITY -> {
                findings.add(SecurityFinding(
                    id = "play_app_integrity_ok",
                    title = "App Meets Integrity Requirements",
                    severity = Severity.OK,
                    metadata = mapOf("message" to "App passes integrity verification")
                ))
            }
        }

        when (result.accountDetails) {
            AccountDetailsStatus.UNLICENSED -> {
                findings.add(SecurityFinding(
                    id = "play_account_unlicensed",
                    title = "Unlicensed App Installation",
                    severity = Severity.BLOCK,
                    metadata = mapOf("message" to "App was not installed through official channels")
                ))
            }
            AccountDetailsStatus.UNAVAILABLE -> {
                findings.add(SecurityFinding(
                    id = "play_account_unavailable",
                    title = "Play Account Status Unavailable",
                    severity = Severity.WARN,
                    metadata = mapOf("message" to "Could not verify app installation source")
                ))
            }
            AccountDetailsStatus.UNKNOWN -> {
                findings.add(SecurityFinding(
                    id = "play_account_unknown",
                    title = "Play Account Status Unknown",
                    severity = Severity.WARN,
                    metadata = mapOf("message" to "Could not determine app installation source")
                ))
            }
            AccountDetailsStatus.LICENSED -> {
                findings.add(SecurityFinding(
                    id = "play_account_licensed",
                    title = "Licensed App Installation",
                    severity = Severity.OK,
                    metadata = mapOf("message" to "App was installed through official channels")
                ))
            }
        }

        if (result.error != null) {
            findings.add(SecurityFinding(
                id = "play_integrity_error",
                title = "Play Integrity Error",
                severity = Severity.WARN,
                metadata = mapOf("error" to result.error)
            ))
        }

        return findings
    }
}
