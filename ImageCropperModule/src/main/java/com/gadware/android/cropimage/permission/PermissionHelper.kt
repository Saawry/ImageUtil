package com.gadware.android.cropimage.permission

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment

/**
 * Lifecycle-safe universal permission request helper.
 *
 * Must be initialized during Activity or Fragment creation (before STARTED state).
 */
class PermissionHelper private constructor(
    private val activityProvider: () -> Activity?,
    private val launcherProvider: (ActivityResultContracts.RequestMultiplePermissions, (Map<String, Boolean>) -> Unit) -> ActivityResultLauncher<Array<String>>
) {
    private var onResultAction: ((PermissionResult) -> Unit)? = null
    private val launcher: ActivityResultLauncher<Array<String>>

    init {
        launcher = launcherProvider(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            handlePermissionResults(results)
        }
    }

    /**
     * Constructs a [PermissionHelper] attached to a [ComponentActivity].
     */
    constructor(activity: ComponentActivity) : this(
        activityProvider = { activity },
        launcherProvider = { contract, callback -> activity.registerForActivityResult(contract, callback) }
    )

    /**
     * Constructs a [PermissionHelper] attached to a [Fragment].
     */
    constructor(fragment: Fragment) : this(
        activityProvider = { fragment.activity },
        launcherProvider = { contract, callback -> fragment.registerForActivityResult(contract, callback) }
    )

    /**
     * Requests permissions for a specific [PermissionGroup].
     */
    fun request(group: PermissionGroup, onResult: (PermissionResult) -> Unit) {
        val permissions = group.getManifestPermissions()
        request(permissions.toList(), onResult)
    }

    /**
     * Requests multiple manifest permissions passed as varargs.
     */
    fun request(vararg permissions: String, onResult: (PermissionResult) -> Unit) {
        request(permissions.toList(), onResult)
    }

    /**
     * Requests a list of manifest permissions.
     */
    fun request(permissions: List<String>, onResult: (PermissionResult) -> Unit) {
        val activity = activityProvider()
        if (activity == null) {
            onResult(PermissionResult.Denied(permissions))
            return
        }

        // If no permissions required (e.g. notifications on API < 33) or already granted
        if (permissions.isEmpty() || permissions.all { PermissionChecker.hasPermission(activity, it) }) {
            onResult(PermissionResult.AllGranted(permissions))
            return
        }

        this.onResultAction = onResult
        launcher.launch(permissions.toTypedArray())
    }

    /**
     * Requests permissions with automatic rationale or settings dialog display if permanently denied.
     */
    fun requestWithDialog(
        group: PermissionGroup,
        dialogConfig: PermissionDialogConfig? = null,
        onResult: (PermissionResult) -> Unit
    ) {
        val activity = activityProvider()
        request(group) { result ->
            when (result) {
                is PermissionResult.AllGranted -> onResult(result)
                is PermissionResult.PermanentlyDenied -> {
                    if (activity != null) {
                        val config = dialogConfig ?: PermissionDialogConfig.createSettingsConfig(activity)
                        PermissionDialogConfig.showDialog(activity, config) {
                            onResult(result)
                        }
                    } else {
                        onResult(result)
                    }
                }
                is PermissionResult.Denied -> onResult(result)
            }
        }
    }

    private fun handlePermissionResults(results: Map<String, Boolean>) {
        val activity = activityProvider()
        val granted = results.filter { it.value }.keys.toList()
        val denied = results.filter { !it.value }.keys.toList()

        if (denied.isEmpty()) {
            onResultAction?.invoke(PermissionResult.AllGranted(granted))
            onResultAction = null
            return
        }

        val permanentlyDenied = if (activity != null) {
            denied.filter { permission ->
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            }
        } else {
            emptyList()
        }

        val result = if (permanentlyDenied.isNotEmpty()) {
            PermissionResult.PermanentlyDenied(
                permanentlyDeniedPermissions = permanentlyDenied,
                deniedPermissions = denied,
                grantedPermissions = granted
            )
        } else {
            PermissionResult.Denied(
                deniedPermissions = denied,
                grantedPermissions = granted
            )
        }

        onResultAction?.invoke(result)
        onResultAction = null
    }
}
