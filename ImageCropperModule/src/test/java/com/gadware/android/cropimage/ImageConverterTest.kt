package com.gadware.android.cropimage

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageConverterTest {

    @Test
    fun testImageHelperInstance() {
        // Verify ImageHelper singleton is available and loaded
        assertNotNull(ImageHelper)
    }

    @Test
    fun testDimensionResolutionRules() {
        // Dimension resolution logic verification:
        // Default: 200 x 200
        // Single param: 50 -> 50 x 50
        // Both params: 300, 400 -> 300 x 400
        val method = ImageHelper::class.java.getDeclaredMethod(
            "resolveDimensions",
            Int::class.javaObjectType,
            Int::class.javaObjectType
        )
        method.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val defaultDims = method.invoke(ImageHelper, null, null) as Pair<Int, Int>
        assertEquals(200, defaultDims.first)
        assertEquals(200, defaultDims.second)

        @Suppress("UNCHECKED_CAST")
        val widthOnlyDims = method.invoke(ImageHelper, 50, null) as Pair<Int, Int>
        assertEquals(50, widthOnlyDims.first)
        assertEquals(50, widthOnlyDims.second)

        @Suppress("UNCHECKED_CAST")
        val heightOnlyDims = method.invoke(ImageHelper, null, 75) as Pair<Int, Int>
        assertEquals(75, heightOnlyDims.first)
        assertEquals(75, heightOnlyDims.second)

        @Suppress("UNCHECKED_CAST")
        val bothDims = method.invoke(ImageHelper, 300, 400) as Pair<Int, Int>
        assertEquals(300, bothDims.first)
        assertEquals(400, bothDims.second)
    }
}
