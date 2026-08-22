package com.gadware.android.cropimage.permission

import android.Manifest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionGroupTest {

    @Test
    fun testCameraGroup() {
        val group = PermissionGroup.Camera
        assertArrayEquals(arrayOf(Manifest.permission.CAMERA), group.getManifestPermissions())
    }

    @Test
    fun testPhoneCallGroup() {
        val group = PermissionGroup.PhoneCall
        assertArrayEquals(arrayOf(Manifest.permission.CALL_PHONE), group.getManifestPermissions())
    }

    @Test
    fun testContactsGroup() {
        val readOnly = PermissionGroup.Contacts(writeAccess = false)
        assertArrayEquals(arrayOf(Manifest.permission.READ_CONTACTS), readOnly.getManifestPermissions())

        val readWrite = PermissionGroup.Contacts(writeAccess = true)
        assertArrayEquals(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS), readWrite.getManifestPermissions())
    }

    @Test
    fun testLocationGroup() {
        val fine = PermissionGroup.Location(fineLocation = true)
        assertArrayEquals(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), fine.getManifestPermissions())

        val coarse = PermissionGroup.Location(fineLocation = false)
        assertArrayEquals(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), coarse.getManifestPermissions())
    }

    @Test
    fun testAudioRecordGroup() {
        val group = PermissionGroup.AudioRecord
        assertArrayEquals(arrayOf(Manifest.permission.RECORD_AUDIO), group.getManifestPermissions())
    }

    @Test
    fun testCustomGroup() {
        val custom = PermissionGroup.Custom("custom.permission.A", "custom.permission.B")
        assertArrayEquals(arrayOf("custom.permission.A", "custom.permission.B"), custom.getManifestPermissions())
    }

    @Test
    fun testImagePickerGroupResolution() {
        val imagePicker = PermissionGroup.ImagePicker(includeCamera = true)
        val permissions = imagePicker.getManifestPermissions()
        assertTrue(permissions.contains(Manifest.permission.CAMERA))
        assertTrue(permissions.any {
            it == Manifest.permission.READ_MEDIA_IMAGES || it == Manifest.permission.READ_EXTERNAL_STORAGE
        })
    }
}
