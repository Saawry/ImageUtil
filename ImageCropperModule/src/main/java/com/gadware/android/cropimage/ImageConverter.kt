package com.gadware.android.cropimage

import android.graphics.Canvas
import android.graphics.Color
import android.view.View


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

object ImageHelper {
    @JvmStatic
    fun getMaxSizeBitmap(image: Bitmap): Bitmap { //int maxSize is 300 here, output 300*300=90000
        var width = image.getWidth()
        var height = image.getHeight()

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
    fun toByteArray(bitmap: Bitmap): ByteArray? {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }


    @JvmStatic
    fun toBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    @JvmStatic
    fun loadBitmapFromView(view: View, width: Int, height: Int): Bitmap {
        val returnedBitmap =
            createBitmap(view.width, view.height)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) bgDrawable.draw(canvas)
        else canvas.drawColor(Color.WHITE)
        view.draw(canvas)

        //        Log.e("width", "=" + width);
//        Log.e("height","="+height);
        return returnedBitmap
    }
    fun convertImageUriToByteArray(context: Context, imageUri: Uri): ByteArray {
        lateinit var data: ByteArray

        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos) // Adjust format and quality as needed
            data = baos.toByteArray()
            inputStream?.close() // Close the input stream
            baos.close() // Close the output stream
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return data
    }
}

