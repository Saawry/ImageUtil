package com.gadware.android.cropimage.permission

import android.content.Context
import androidx.appcompat.app.AlertDialog

/**
 * Configuration for displaying rationale or settings redirect dialogs.
 */
data class PermissionDialogConfig(
    val title: String? = null,
    val message: String = "This feature requires permissions that have been denied. Please grant them to proceed.",
    val positiveButtonText: String = "Settings",
    val negativeButtonText: String = "Cancel",
    val isCancelable: Boolean = true,
    val onPositiveClicked: (() -> Unit)? = null,
    val onNegativeClicked: (() -> Unit)? = null
) {
    companion object {
        /**
         * Creates a standard dialog configuration for directing the user to system settings
         * when permissions are permanently denied.
         */
        fun createSettingsConfig(
            context: Context,
            title: String? = "Permission Required",
            message: String = "Permissions have been permanently denied. Please enable them in app settings to continue.",
            positiveButtonText: String = "Open Settings",
            negativeButtonText: String = "Cancel"
        ): PermissionDialogConfig {
            return PermissionDialogConfig(
                title = title,
                message = message,
                positiveButtonText = positiveButtonText,
                negativeButtonText = negativeButtonText,
                onPositiveClicked = {
                    PermissionChecker.openAppSettings(context)
                }
            )
        }

        /**
         * Displays an [AlertDialog] configured with the provided [PermissionDialogConfig].
         */
        fun showDialog(
            context: Context,
            config: PermissionDialogConfig,
            onDismiss: (() -> Unit)? = null
        ): AlertDialog {
            return AlertDialog.Builder(context)
                .apply {
                    config.title?.let { setTitle(it) }
                    setMessage(config.message)
                    setCancelable(config.isCancelable)
                    setPositiveButton(config.positiveButtonText) { dialog, _ ->
                        config.onPositiveClicked?.invoke()
                        dialog.dismiss()
                        onDismiss?.invoke()
                    }
                    setNegativeButton(config.negativeButtonText) { dialog, _ ->
                        config.onNegativeClicked?.invoke()
                        dialog.dismiss()
                        onDismiss?.invoke()
                    }
                }
                .create()
                .apply { show() }
        }
    }
}
