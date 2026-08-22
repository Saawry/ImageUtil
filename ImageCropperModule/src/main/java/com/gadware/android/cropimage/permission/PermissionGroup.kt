package com.gadware.android.cropimage.permission

import android.Manifest
import android.os.Build

/**
 * Represents high-level permission categories that automatically resolve
 * to the appropriate Android OS manifest permissions based on the device's API level.
 */
sealed class PermissionGroup {

    /**
     * Permissions required for selecting or capturing images (Gallery and Camera).
     *
     * - Android 14+ (API 34+): [Manifest.permission.READ_MEDIA_IMAGES] (+ [Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] if needed) and optionally [Manifest.permission.CAMERA]
     * - Android 13 (API 33): [Manifest.permission.READ_MEDIA_IMAGES] and optionally [Manifest.permission.CAMERA]
     * - Android 6–12 (API 23–32): [Manifest.permission.READ_EXTERNAL_STORAGE] and optionally [Manifest.permission.CAMERA]
     */
    data class ImagePicker(
        val includeCamera: Boolean = true,
        val includeVisualUserSelected: Boolean = false
    ) : PermissionGroup() {
        override fun getManifestPermissions(): Array<String> {
            val permissions = mutableListOf<String>()
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                    permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                    if (includeVisualUserSelected) {
                        permissions.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                }
                else -> {
                    permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            if (includeCamera) {
                permissions.add(Manifest.permission.CAMERA)
            }
            return permissions.toTypedArray()
        }
    }

    /**
     * Camera permission for capturing photos/video or scanning barcodes.
     */
    object Camera : PermissionGroup() {
        override fun getManifestPermissions(): Array<String> = arrayOf(Manifest.permission.CAMERA)
    }

    /**
     * Storage and media read/write permissions.
     */
    data class Storage(
        val writeAccess: Boolean = false,
        val includeImages: Boolean = true,
        val includeVideo: Boolean = false,
        val includeAudio: Boolean = false
    ) : PermissionGroup() {
        override fun getManifestPermissions(): Array<String> {
            val permissions = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (includeImages) permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
                if (includeVideo) permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
                if (includeAudio) permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (writeAccess && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            return permissions.toTypedArray()
        }
    }

    /**
     * Notification posting permission.
     * - API 33+: [Manifest.permission.POST_NOTIFICATIONS]
     * - API < 33: No runtime permission needed (returns empty array).
     */
    object Notifications : PermissionGroup() {
        override fun getManifestPermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                emptyArray()
            }
        }
    }

    /**
     * Contact read and write permissions.
     */
    data class Contacts(val writeAccess: Boolean = false) : PermissionGroup() {
        override fun getManifestPermissions(): Array<String> {
            return if (writeAccess) {
                arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
            } else {
                arrayOf(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    /**
     * Phone call permission.
     */
    object PhoneCall : PermissionGroup() {
        override fun getManifestPermissions(): Array<String> = arrayOf(Manifest.permission.CALL_PHONE)
    }

    /**
     * Location permissions.
     */
    data class Location(val fineLocation: Boolean = true) : PermissionGroup() {
        override fun getManifestPermissions(): Array<String> {
            return if (fineLocation) {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            } else {
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
    }

    /**
     * Audio recording permission.
     */
    object AudioRecord : PermissionGroup() {
        override fun getManifestPermissions(): Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)
    }

    /**
     * Custom collection of manifest permissions.
     */
    data class Custom(val permissions: List<String>) : PermissionGroup() {
        constructor(vararg permissions: String) : this(permissions.toList())
        override fun getManifestPermissions(): Array<String> = permissions.toTypedArray()
    }

    /**
     * Resolves and returns the array of Android manifest permission strings for this group.
     */
    abstract fun getManifestPermissions(): Array<String>
}
