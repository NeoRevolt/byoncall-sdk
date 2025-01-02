package com.dartmedia.byoncallsdkclient.utils.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.bumptech.glide.load.Key
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

class WhiteBackgroundTransformation : BitmapTransformation() {

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("WhiteBackgroundTransformation".toByteArray(Key.CHARSET))
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        // Create a new bitmap with the same size as the input
        val whiteBackgroundBitmap = pool.get(outWidth, outHeight, Bitmap.Config.ARGB_8888)

        // Set up the canvas to draw the white background and the original image
        val canvas = Canvas(whiteBackgroundBitmap)
        val paint = Paint()

        // Fill the background with white
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, outWidth.toFloat(), outHeight.toFloat(), paint)

        // Draw the original image on top of the white background
        canvas.drawBitmap(toTransform, 0f, 0f, null)

        return whiteBackgroundBitmap
    }
}
