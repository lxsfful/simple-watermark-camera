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
import com.lx.simplewatermarkcamera.R
import com.lx.simplewatermarkcamera.domain.CaptureMetadata
import com.lx.simplewatermarkcamera.domain.LogoSource
import com.lx.simplewatermarkcamera.domain.WatermarkComposer
import com.lx.simplewatermarkcamera.domain.WatermarkConfig
import com.lx.simplewatermarkcamera.domain.WatermarkLayoutCalculator
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

class WatermarkRenderer(private val context: Context? = null) {
    fun renderJpeg(source: File, config: WatermarkConfig, metadata: CaptureMetadata): ByteArray {
        val bitmap = decodeAndOrientMutable(source)
        val canvas = Canvas(bitmap)
        val content = WatermarkComposer.compose(config, metadata)
        val fontScale = config.fontSizeLevel.scale
        val hasLogo = config.showLogo && config.logoSource != LogoSource.None
        val card = WatermarkLayoutCalculator.bottomCard(
            imageWidth = bitmap.width,
            imageHeight = bitmap.height,
            lineCount = content.lines.size,
            hasLogo = hasLogo,
            fontScale = fontScale,
        )
        val scale = (bitmap.width / 1080f) * fontScale
        val padding = (12f * scale).coerceAtLeast(10f)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (16f * scale).coerceAtLeast(12f)
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (13f * scale).coerceAtLeast(10f)
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            setShadowLayer(4f, 1.5f, 1.5f, Color.argb(220, 0, 0, 0))
        }

        val logoBitmap = if (hasLogo) decodeLogo(config) else null
        val logoSize = if (logoBitmap != null) (card.height - padding * 2).roundToInt().coerceAtLeast(1) else 0
        val logoLeft = card.left.toFloat() + padding
        val textLeft = if (logoBitmap != null) logoLeft + logoSize.toFloat() + padding else card.left.toFloat() + padding

        logoBitmap?.let { logo ->
            val dest = Rect(
                logoLeft.roundToInt(),
                (card.top.toFloat() + padding).roundToInt(),
                logoLeft.roundToInt() + logoSize,
                (card.top.toFloat() + padding).roundToInt() + logoSize
            )
            canvas.drawBitmap(logo, null, dest, Paint(Paint.ANTI_ALIAS_FLAG))
            logo.recycle()
        }

        var y = card.top.toFloat() + padding - titlePaint.fontMetrics.ascent
        val maxTextWidth = (card.right.toFloat() - padding - textLeft).coerceAtLeast(1f)
        val titleText = content.title

        // 绘制标题紫色 Tag
        val titleWidth = titlePaint.measureText(titleText)
        val tagPaddingX = 14f * scale
        val tagPaddingY = 6f * scale
        val tagRect = RectF(
            textLeft - 4f,
            card.top.toFloat() + padding - 2f,
            textLeft + titleWidth + tagPaddingX,
            card.top.toFloat() + padding + titlePaint.fontSpacing + tagPaddingY
        )
        val tagBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#7A36D6")
        }
        canvas.drawRoundRect(tagRect, 8f * scale, 8f * scale, tagBgPaint)
        canvas.drawText(titleText, textLeft + tagPaddingX / 3f, y + tagPaddingY / 2f, titlePaint)

        y += titlePaint.fontSpacing * 1.45f
        content.lines.forEach { line ->
            if (y < card.bottom.toFloat() + padding) {
                canvas.drawText(ellipsize(line.text, linePaint, maxTextWidth), textLeft, y, linePaint)
                y += linePaint.fontSpacing * 1.15f
            }
        }
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 94, output)
            bitmap.recycle()
            output.toByteArray()
        }
    }

    private fun decodeAndOrientMutable(source: File): Bitmap {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(source.absolutePath, options)
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(options.outWidth, options.outHeight, maxLongEdge = 3000)
            inMutable = true
        }
        val decoded = requireNotNull(BitmapFactory.decodeFile(source.absolutePath, decodeOptions)) {
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
        if (matrix.isIdentity) {
            return if (decoded.isMutable) decoded else decoded.copy(Bitmap.Config.ARGB_8888, true).also { decoded.recycle() }
        }
        val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        decoded.recycle()
        return if (rotated.isMutable) rotated else rotated.copy(Bitmap.Config.ARGB_8888, true).also { rotated.recycle() }
    }

    private fun decodeLogo(config: WatermarkConfig): Bitmap? = runCatching {
        when (config.logoSource) {
            LogoSource.None -> null
            LogoSource.Company -> context?.let {
                BitmapFactory.decodeResource(it.resources, R.drawable.ic_jinghu_logo)
            }
            LogoSource.Custom -> config.logoPath?.let { path ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, options)
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = calculateSampleSize(options.outWidth, options.outHeight, maxLongEdge = 512)
                }
                BitmapFactory.decodeFile(path, decodeOptions)
            }
        }
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

    internal fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (maxWidth <= 0f) return ""
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        val ellipsisWidth = paint.measureText(ellipsis)
        val availableWidth = maxWidth - ellipsisWidth
        if (availableWidth <= 0f) return ellipsis
        val count = paint.breakText(text, true, availableWidth, null)
        return if (count in 1..text.length) {
            text.substring(0, count) + ellipsis
        } else {
            ellipsis
        }
    }
}
