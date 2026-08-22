package com.gadware.android.cropimage.permission

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Utility object for checking permission statuses and directing users to system settings.
 */
object PermissionChecker {

    /**
     * Checks if a single permission is granted.
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if all specified permissions are granted.
     */
    fun hasPermissions(context: Context, vararg permissions: String): Boolean {
        return permissions.all { hasPermission(context, it) }
    }

    /**
     * Checks if all specified permissions in the collection are granted.
     */
    fun hasPermissions(context: Context, permissions: Collection<String>): Boolean {
        return permissions.all { hasPermission(context, it) }
    }

    /**
     * Checks if all permissions in the specified [PermissionGroup] are granted.
     */
    fun hasGroup(context: Context, group: PermissionGroup): Boolean {
        val permissions = group.getManifestPermissions()
        return permissions.isEmpty() || hasPermissions(context, *permissions)
    }

    /**
     * Checks whether a permission has been permanently denied ("Don't ask again" selected).
     * Note: This should be evaluated after a permission request result is returned.
     */
    fun isPermanentlyDenied(activity: Activity, permission: String): Boolean {
        val isGranted = hasPermission(activity, permission)
        if (isGranted) return false
        val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        return !shouldShowRationale
    }

    /**
     * Launches the system App Settings screen for this application.
     *
     * @return true if the intent was successfully started, false otherwise.
     */
    fun openAppSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
