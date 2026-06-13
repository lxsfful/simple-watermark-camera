package com.lx.simplewatermarkcamera.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.exifinterface.media.ExifInterface
import com.lx.simplewatermarkcamera.domain.WatermarkComposer
import com.lx.simplewatermarkcamera.domain.WatermarkConfig
import com.lx.simplewatermarkcamera.domain.CaptureMetadata
import com.lx.simplewatermarkcamera.domain.WatermarkLayoutCalculator
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

class WatermarkRenderer(private val context: Context) {
    fun renderJpeg(source: File, config: WatermarkConfig, metadata: CaptureMetadata): ByteArray {
        val bitmap = decodeCorrectlyOriented(source)
        val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        if (mutable != bitmap) bitmap.recycle()
        val canvas = Canvas(mutable)
        val content = WatermarkComposer.compose(config, metadata)
        val card = WatermarkLayoutCalculator.bottomCard(
            imageWidth = mutable.width,
            imageHeight = mutable.height,
            lineCount = content.lines.size,
            hasLogo = content.logoPath != null,
        )
        val scale = mutable.width / 1080f
        val padding = (24f * scale).coerceAtLeast(18f)
        val radius = (18f * scale).coerceAtLeast(12f)
        val cardRect = RectF(card.left.toFloat(), card.top.toFloat(), card.right.toFloat(), card.bottom.toFloat())
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(185, 0, 0, 0) }
        canvas.drawRoundRect(cardRect, radius, radius, bgPaint)

        val logoBitmap = content.logoPath?.let { path -> decodeLogo(path) }
        val logoSize = if (logoBitmap != null) (card.height - padding * 2).roundToInt().coerceAtLeast(1) else 0
        val logoLeft = card.left + padding
        val textLeft = if (logoBitmap != null) logoLeft + logoSize + padding else card.left + padding
        logoBitmap?.let { logo ->
            val dest = Rect(logoLeft.roundToInt(), (card.top + padding).roundToInt(), logoLeft.roundToInt() + logoSize, (card.top + padding).roundToInt() + logoSize)
            canvas.drawBitmap(logo, null, dest, Paint(Paint.ANTI_ALIAS_FLAG))
            logo.recycle()
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (32f * scale).coerceAtLeast(26f)
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            setShadowLayer(3f, 1f, 1f, Color.BLACK)
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(235, 245, 240)
            textSize = (23f * scale).coerceAtLeast(19f)
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }
        var y = card.top + padding - titlePaint.fontMetrics.ascent
        val maxTextWidth = (card.right - padding - textLeft).coerceAtLeast(1f)
        canvas.drawText(ellipsize(content.title, titlePaint, maxTextWidth), textLeft, y, titlePaint)
        y += titlePaint.fontSpacing * 0.98f
        content.lines.forEach { line ->
            if (y < card.bottom - padding) {
                canvas.drawText(ellipsize(line.text, linePaint, maxTextWidth), textLeft, y, linePaint)
                y += linePaint.fontSpacing * 0.98f
            }
        }
        return ByteArrayOutputStream().use { output ->
            mutable.compress(Bitmap.CompressFormat.JPEG, 94, output)
            mutable.recycle()
            output.toByteArray()
        }
    }

    private fun decodeCorrectlyOriented(source: File): Bitmap {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(source.absolutePath, options)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(options.outWidth, options.outHeight, maxLongEdge = 3000)
        }
        val bitmap = requireNotNull(BitmapFactory.decodeFile(source.absolutePath, decodeOptions)) {
            "无法解码照片"
        }
        val orientation = ExifInterface(source.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> preScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> preScale(1f, -1f)
            }
        }
        if (matrix.isIdentity) return bitmap
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()
        return rotated
    }

    private fun decodeLogo(path: String): Bitmap? = runCatching {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(options.outWidth, options.outHeight, maxLongEdge = 512)
        }
        BitmapFactory.decodeFile(path, decodeOptions)
    }.getOrNull()

    private fun calculateSampleSize(width: Int, height: Int, maxLongEdge: Int): Int {
        var sampleSize = 1
        var sampledWidth = width
        var sampledHeight = height
        while (sampledWidth / 2 >= maxLongEdge || sampledHeight / 2 >= maxLongEdge) {
            sampleSize *= 2
            sampledWidth /= 2
            sampledHeight /= 2
        }
        return sampleSize.coerceAtLeast(1)
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var candidate = text
        while (candidate.length > 1 && paint.measureText("$candidate…") > maxWidth) {
            candidate = candidate.dropLast(1)
        }
        return "$candidate…"
    }
}
