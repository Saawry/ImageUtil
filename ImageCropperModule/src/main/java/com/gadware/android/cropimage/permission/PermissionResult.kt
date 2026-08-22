package com.gadware.android.cropimage.permission

/**
 * Result representing the outcome of a permission request.
 */
sealed class PermissionResult {

    /**
     * All requested permissions were granted.
     */
    data class AllGranted(
        val permissions: List<String>
    ) : PermissionResult()

    /**
     * One or more permissions were denied, but the user can be prompted again
     * or shown a rationale dialog.
     */
    data class Denied(
        val deniedPermissions: List<String>,
        val grantedPermissions: List<String> = emptyList()
    ) : PermissionResult()

    /**
     * One or more permissions were permanently denied ("Don't ask again" selected
     * or denied multiple times). System settings redirection is typically required.
     */
    data class PermanentlyDenied(
        val permanentlyDeniedPermissions: List<String>,
        val deniedPermissions: List<String> = emptyList(),
        val grantedPermissions: List<String> = emptyList()
    ) : PermissionResult()

    /**
     * Returns true if all requested permissions were granted.
     */
    val isAllGranted: Boolean
        get() = this is AllGranted

    /**
     * Returns true if any permission was denied (either regular denial or permanently denied).
     */
    val hasDenied: Boolean
        get() = this is Denied || this is PermanentlyDenied

    /**
     * Returns true if any permission was permanently denied.
     */
    val hasPermanentlyDenied: Boolean
        get() = this is PermanentlyDenied

    /**
     * Executes [action] if all permissions are granted.
     */
    inline fun ifAllGranted(action: (List<String>) -> Unit): PermissionResult {
        if (this is AllGranted) action(permissions)
        return this
    }

    /**
     * Executes [action] if any permission is denied (regular denial).
     */
    inline fun ifDenied(action: (denied: List<String>, granted: List<String>) -> Unit): PermissionResult {
        if (this is Denied) action(deniedPermissions, grantedPermissions)
        return this
    }

    /**
     * Executes [action] if any permission is permanently denied.
     */
    inline fun ifPermanentlyDenied(action: (permanentlyDenied: List<String>, denied: List<String>, granted: List<String>) -> Unit): PermissionResult {
        if (this is PermanentlyDenied) action(permanentlyDeniedPermissions, deniedPermissions, grantedPermissions)
        return this
    }
}
