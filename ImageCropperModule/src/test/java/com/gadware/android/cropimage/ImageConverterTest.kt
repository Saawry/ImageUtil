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

    @Test
    fun testProportionalDimensionCalculation() {
        // 16:9 ratio: 1920 x 1080 with maxDimension 800 -> 800 x 450
        val (w16_9, h16_9) = ImageHelper.calculateProportionalDimensions(1920, 1080, 800)
        assertEquals(800, w16_9)
        assertEquals(450, h16_9)

        // 2:1 ratio: 2000 x 1000 with maxDimension 800 -> 800 x 400
        val (w2_1, h2_1) = ImageHelper.calculateProportionalDimensions(2000, 1000, 800)
        assertEquals(800, w2_1)
        assertEquals(400, h2_1)

        // 4:3 ratio: 1600 x 1200 with maxDimension 800 -> 800 x 600
        val (w4_3, h4_3) = ImageHelper.calculateProportionalDimensions(1600, 1200, 800)
        assertEquals(800, w4_3)
        assertEquals(600, h4_3)

        // Portrait 9:16 ratio: 1080 x 1920 with maxDimension 800 -> 450 x 800
        val (w9_16, h9_16) = ImageHelper.calculateProportionalDimensions(1080, 1920, 800)
        assertEquals(450, w9_16)
        assertEquals(800, h9_16)

        // 1:1 ratio: 1000 x 1000 with maxDimension 500 -> 500 x 500
        val (w1_1, h1_1) = ImageHelper.calculateProportionalDimensions(1000, 1000, 500)
        assertEquals(500, w1_1)
        assertEquals(500, h1_1)

        // Smaller than maxDimension: 400 x 300 with maxDimension 800 -> stays 400 x 300
        val (wSmall, hSmall) = ImageHelper.calculateProportionalDimensions(400, 300, 800)
        assertEquals(400, wSmall)
        assertEquals(300, hSmall)
    }
}
