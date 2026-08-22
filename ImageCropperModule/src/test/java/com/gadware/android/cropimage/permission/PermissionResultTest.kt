package com.gadware.android.cropimage.permission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionResultTest {

    @Test
    fun testAllGrantedResult() {
        val permissions = listOf("android.permission.CAMERA", "android.permission.READ_MEDIA_IMAGES")
        val result: PermissionResult = PermissionResult.AllGranted(permissions)

        assertTrue(result.isAllGranted)
        assertFalse(result.hasDenied)
        assertFalse(result.hasPermanentlyDenied)

        var grantedCalled = false
        result.ifAllGranted { grantedList ->
            grantedCalled = true
            assertEquals(permissions, grantedList)
        }
        assertTrue(grantedCalled)

        var deniedCalled = false
        result.ifDenied { _, _ -> deniedCalled = true }
        assertFalse(deniedCalled)
    }

    @Test
    fun testDeniedResult() {
        val denied = listOf("android.permission.CAMERA")
        val granted = listOf("android.permission.READ_MEDIA_IMAGES")
        val result: PermissionResult = PermissionResult.Denied(denied, granted)

        assertFalse(result.isAllGranted)
        assertTrue(result.hasDenied)
        assertFalse(result.hasPermanentlyDenied)

        var deniedCalled = false
        result.ifDenied { deniedList, grantedList ->
            deniedCalled = true
            assertEquals(denied, deniedList)
            assertEquals(granted, grantedList)
        }
        assertTrue(deniedCalled)
    }

    @Test
    fun testPermanentlyDeniedResult() {
        val permanentlyDenied = listOf("android.permission.CAMERA")
        val denied = listOf("android.permission.CAMERA", "android.permission.READ_CONTACTS")
        val granted = listOf("android.permission.READ_MEDIA_IMAGES")
        val result: PermissionResult = PermissionResult.PermanentlyDenied(permanentlyDenied, denied, granted)

        assertFalse(result.isAllGranted)
        assertTrue(result.hasDenied)
        assertTrue(result.hasPermanentlyDenied)

        var permanentlyDeniedCalled = false
        result.ifPermanentlyDenied { permDenied, den, gr ->
            permanentlyDeniedCalled = true
            assertEquals(permanentlyDenied, permDenied)
            assertEquals(denied, den)
            assertEquals(granted, gr)
        }
        assertTrue(permanentlyDeniedCalled)
    }
}
