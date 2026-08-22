package com.gadware.android.cropimage

import org.junit.Assert.assertNotNull
import org.junit.Test

class ImageConverterTest {

    @Test
    fun testImageHelperInstance() {
        // Verify ImageHelper singleton is available and loaded
        assertNotNull(ImageHelper)
    }
}
