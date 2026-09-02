package app.shotlist.health.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import app.shotlist.health.api.HealthGateway

/** Result of one user-initiated Health Connect permission prompt. */
internal enum class HealthPermissionDecision {
    GRANTED,
    DENIED,
    MANAGE_ACCESS_REQUIRED,
}

/**
 * Owns the permission prompt policy without ever requesting on app startup.
 * UI must recheck [hasReadPermission] from onResume so external revocation is
 * reflected immediately.
 */
internal class HealthPermissionController(
    context: Context,
    private val gateway: HealthGateway,
    cancelStore: PermissionCancelStore = SharedPreferencesCancelStore(context),
) {
    val requiredPermissions: Set<String> = HealthConnectGateway.READ_PERMISSIONS
    private val promptPolicy = HealthPermissionPromptPolicy(requiredPermissions, cancelStore)

    fun requestContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    suspend fun hasReadPermission(): Boolean = gateway.hasReadPermission()

    fun recordPromptResult(grantedPermissions: Set<String>): HealthPermissionDecision =
        promptPolicy.recordResult(grantedPermissions)

    fun manageAccessIntent(): Intent =
        Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)

    fun installOrUpdateIntent(): Intent = Intent(Intent.ACTION_VIEW).apply {
        setPackage("com.android.vending")
        data = Uri.parse(
            "market://details?id=${HealthConnectGateway.PROVIDER_PACKAGE}" +
                "&url=healthconnect%3A%2F%2Fonboarding",
        )
        putExtra("overlay", true)
        putExtra("callerId", appContext.packageName)
    }

    private val appContext = context.applicationContext
}

internal class HealthPermissionPromptPolicy(
    private val requiredPermissions: Set<String>,
    private val cancelStore: PermissionCancelStore,
) {
    fun recordResult(grantedPermissions: Set<String>): HealthPermissionDecision {
        if (grantedPermissions.containsAll(requiredPermissions)) {
            cancelStore.cancelCount = 0
            return HealthPermissionDecision.GRANTED
        }
        cancelStore.cancelCount += 1
        return if (cancelStore.cancelCount >= MAX_CANCELS) {
            HealthPermissionDecision.MANAGE_ACCESS_REQUIRED
        } else {
            HealthPermissionDecision.DENIED
        }
    }

    companion object {
        private const val MAX_CANCELS = 2
    }
}

internal interface PermissionCancelStore {
    var cancelCount: Int
}

private class SharedPreferencesCancelStore(context: Context) : PermissionCancelStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        "health_connect_permission",
        Context.MODE_PRIVATE,
    )

    override var cancelCount: Int
        get() = preferences.getInt(KEY_CANCEL_COUNT, 0)
        set(value) {
            preferences.edit().putInt(KEY_CANCEL_COUNT, value.coerceAtLeast(0)).apply()
        }

    companion object {
        private const val KEY_CANCEL_COUNT = "cancel_count"
    }
}
