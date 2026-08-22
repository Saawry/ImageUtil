package com.gadware.android.cropimage.permission

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment

/**
 * Registers and returns a [PermissionHelper] attached to this [ComponentActivity].
 * Must be invoked during initialization / onCreate before STARTED lifecycle state.
 */
fun ComponentActivity.registerPermissionHelper(): PermissionHelper {
    return PermissionHelper(this)
}

/**
 * Registers and returns a [PermissionHelper] attached to this [Fragment].
 * Must be invoked during initialization / onAttach / onCreate before STARTED lifecycle state.
 */
fun Fragment.registerPermissionHelper(): PermissionHelper {
    return PermissionHelper(this)
}

/**
 * Checks if this context has the specified manifest permission granted.
 */
fun Context.hasPermission(permission: String): Boolean {
    return PermissionChecker.hasPermission(this, permission)
}

/**
 * Checks if this context has all specified manifest permissions granted.
 */
fun Context.hasPermissions(vararg permissions: String): Boolean {
    return PermissionChecker.hasPermissions(this, *permissions)
}

/**
 * Checks if this context has all permissions in the specified [PermissionGroup] granted.
 */
fun Context.hasPermissionGroup(group: PermissionGroup): Boolean {
    return PermissionChecker.hasGroup(this, group)
}

/**
 * Opens the application details settings screen for this app.
 */
fun Context.openAppSettings(): Boolean {
    return PermissionChecker.openAppSettings(this)
}
