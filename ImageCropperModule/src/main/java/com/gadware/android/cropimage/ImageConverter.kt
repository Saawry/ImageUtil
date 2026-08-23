package com.gadware.android.cropimage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.view.View
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageHelper {

    /**
     * Universal converter: Converts a [Bitmap] to a [ByteArray] with optional dimensions,
     * compression format, and compression ratio/quality.
     *
     * @param bitmap The source Bitmap.
     * @param width Optional target width in pixels. If only width is provided (e.g. 50), dimension is 50px x 50px.
     * @param height Optional target height in pixels. If only height is provided (e.g. 50), dimension is 50px x 50px.
     *               If neither is provided, defaults to 200px x 200px.
     * @param format Compression format (PNG, JPEG, WEBP, etc.). Defaults to [Bitmap.CompressFormat.PNG].
     * @param ratio Compression ratio/quality from 0 to 100. Defaults to 50.
     * @return ByteArray containing the compressed image data, or null on error.
     */
    @JvmStatic
    @JvmOverloads
    fun toByteArray(
        bitmap: Bitmap,
        width: Int? = null,
        height: Int? = null,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        ratio: Int = 50
    ): ByteArray? {
        return try {
            val (targetW, targetH) = resolveDimensions(width, height)
            val scaledBitmap = if (bitmap.width == targetW && bitmap.height == targetH) {
                bitmap
            } else {
                Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
            }

            val outputStream = ByteArrayOutputStream()
            val clampedRatio = ratio.coerceIn(0, 100)
            scaledBitmap.compress(format, clampedRatio, outputStream)

            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            outputStream.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Universal converter: Converts an image [Uri] to a [ByteArray] with memory-safe
     * downsampling, compression format, ratio/quality, and dimensions.
     *
     * @param context Application or Activity context to access ContentResolver.
     * @param uri The Uri of the image to convert.
     * @param width Optional target width in pixels. If only width is provided (e.g. 50), dimension is 50px x 50px.
     * @param height Optional target height in pixels. If only height is provided (e.g. 50), dimension is 50px x 50px.
     *               If neither is provided, defaults to 200px x 200px.
     * @param format Compression format (PNG, JPEG, WEBP, etc.). Defaults to [Bitmap.CompressFormat.PNG].
     * @param ratio Compression ratio/quality from 0 to 100. Defaults to 50.
     * @return ByteArray containing the compressed image data, or null on error.
     */
    @JvmStatic
    @JvmOverloads
    fun toByteArray(
        context: Context,
        uri: Uri,
        width: Int? = null,
        height: Int? = null,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        ratio: Int = 50
    ): ByteArray? {
        return try {
            val (targetW, targetH) = resolveDimensions(width, height)
            val decodedBitmap = decodeSampledBitmapFromUri(context, uri, targetW, targetH) ?: return null
            val result = toByteArray(
                bitmap = decodedBitmap,
                width = targetW,
                height = targetH,
                format = format,
                ratio = ratio
            )
            decodedBitmap.recycle()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Universal converter accepting either [Bitmap] or [Uri] as input.
     *
     * @param image Input image as [Bitmap] or [Uri].
     * @param context Context required when [image] is a [Uri].
     * @param width Optional target width. Defaults to 200 if neither width nor height is passed.
     * @param height Optional target height. Defaults to 200 if neither width nor height is passed.
     * @param format Compression format. Defaults to [Bitmap.CompressFormat.PNG].
     * @param ratio Compression ratio/quality. Defaults to 50.
     */
    @JvmStatic
    @JvmOverloads
    fun toByteArrayUniversal(
        image: Any,
        context: Context? = null,
        width: Int? = null,
        height: Int? = null,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        ratio: Int = 50
    ): ByteArray? {
        return when (image) {
            is Bitmap -> toByteArray(image, width, height, format, ratio)
            is Uri -> {
                if (context == null) {
                    throw IllegalArgumentException("Context must not be null when converting Uri to ByteArray")
                }
                toByteArray(context, image, width, height, format, ratio)
            }
            else -> throw IllegalArgumentException("Unsupported image type: ${image::class.java.name}. Expected Bitmap or Uri.")
        }
    }

    @JvmStatic
    fun getMaxSizeBitmap(image: Bitmap): Bitmap { //int maxSize is 300 here, output 300*300=90000
        var width = image.width
        var height = image.height

        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = 300
            height = (width / bitmapRatio).toInt()
        } else {
            height = 300
            width = (height * bitmapRatio).toInt()
        }

        return image.scale(width, height)
    }

    @JvmStatic
    fun toBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    @JvmStatic
    fun loadBitmapFromView(view: View, width: Int, height: Int): Bitmap {
        val returnedBitmap = createBitmap(view.width, view.height)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) bgDrawable.draw(canvas)
        else canvas.drawColor(Color.WHITE)
        view.draw(canvas)
        return returnedBitmap
    }

    @JvmStatic
    fun convertImageUriToByteArray(context: Context, imageUri: Uri): ByteArray {
        return toByteArray(context, imageUri, format = Bitmap.CompressFormat.JPEG, ratio = 100) ?: ByteArray(0)
    }

    /**
     * Compresses and saves the provided [Bitmap] to a file [Uri].
     * If [destinationUri] is null, a temporary cache Uri is automatically generated.
     */
    @JvmStatic
    @JvmOverloads
    fun writeBitmapToUri(
        context: Context,
        bitmap: Bitmap,
        destinationUri: Uri? = null,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        quality: Int = 85
    ): Uri {
        return BitmapUtils.writeBitmapToUri(
            context = context,
            bitmap = bitmap,
            compressFormat = format,
            compressQuality = quality,
            customOutputUri = destinationUri
        )
    }

    /**
     * Efficiently decodes a sampled [Bitmap] from a [Uri] using inSampleSize calculation to prevent OOM errors.
     */
    @JvmStatic
    @JvmOverloads
    fun decodeSampledBitmap(
        context: Context,
        uri: Uri,
        reqWidth: Int = 200,
        reqHeight: Int = 200
    ): Bitmap? {
        return decodeSampledBitmapFromUri(context, uri, reqWidth, reqHeight)
    }

    /**
     * Resolves target dimensions according to the rules:
     * - If both width and height are provided, returns (width, height)
     * - If only width is provided (e.g. 50), dimension is 50px x 50px
     * - If only height is provided (e.g. 50), dimension is 50px x 50px
     * - If neither is provided, default dimension is 200px x 200px
     */
    private fun resolveDimensions(width: Int?, height: Int?): Pair<Int, Int> {
        val targetWidth: Int
        val targetHeight: Int
        when {
            width != null && height != null -> {
                targetWidth = width
                targetHeight = height
            }
            width != null -> {
                targetWidth = width
                targetHeight = width
            }
            height != null -> {
                targetWidth = height
                targetHeight = height
            }
            else -> {
                targetWidth = 200
                targetHeight = 200
            }
        }
        return Pair(max(1, targetWidth), max(1, targetHeight))
    }

    private fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        val resolver = context.contentResolver
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
