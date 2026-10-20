package com.qingyi.translate.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.ui.geometry.Rect
import androidx.exifinterface.media.ExifInterface

object BitmapUtils {

    /** Loads, downsamples and orientation-corrects an image from a content or file Uri. */
    fun load(context: Context, uri: Uri, maxSide: Int = 2048): Bitmap? {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching {
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = runCatching {
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }.getOrNull() ?: return null

        val orientation = runCatching {
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val rotated = applyOrientation(decoded, orientation)

        val longest = maxOf(rotated.width, rotated.height)
        return if (longest > maxSide) {
            val ratio = maxSide.toFloat() / longest
            Bitmap.createScaledBitmap(
                rotated,
                (rotated.width * ratio).toInt().coerceAtLeast(1),
                (rotated.height * ratio).toInt().coerceAtLeast(1),
                true,
            )
        } else {
            rotated
        }
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }.getOrDefault(bitmap)
    }

    /** Crops with a normalised selection (0..1 on both axes). */
    fun crop(bitmap: Bitmap, selection: Rect): Bitmap {
        val left = (selection.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val top = (selection.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val width = ((selection.right - selection.left) * bitmap.width)
            .toInt()
            .coerceAtLeast(1)
            .coerceAtMost(bitmap.width - left)
        val height = ((selection.bottom - selection.top) * bitmap.height)
            .toInt()
            .coerceAtLeast(1)
            .coerceAtMost(bitmap.height - top)
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }
}
