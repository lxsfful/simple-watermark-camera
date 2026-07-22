package com.lx.simplewatermarkcamera.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import androidx.exifinterface.media.ExifInterface
import com.lx.simplewatermarkcamera.R
import com.lx.simplewatermarkcamera.domain.CaptureMetadata
import com.lx.simplewatermarkcamera.domain.LogoPosition
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
        val alphaFactor = config.opacityLevel.alpha
        val alphaInt = (255 * alphaFactor).roundToInt().coerceIn(0, 255)

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
            alpha = alphaInt
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (13f * scale).coerceAtLeast(10f)
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            setShadowLayer(4f, 1.5f, 1.5f, Color.argb((220 * alphaFactor).roundToInt(), 0, 0, 0))
            alpha = alphaInt
        }

        val logoBitmap = if (hasLogo) decodeLogo(config) else null
        val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = alphaInt
            config.logoColorOption.hexColor?.let { hex ->
                colorFilter = PorterDuffColorFilter(Color.parseColor(hex), PorterDuff.Mode.SRC_IN)
            }
        }

        val logoPosition = config.logoPosition
        var startY = card.top.toFloat() + padding

        // 绘制 Logo：文字上方 (Above)
        if (logoBitmap != null && logoPosition == LogoPosition.Above) {
            val maxLogoHeight = 48f * scale
            val aspect = logoBitmap.width.toFloat() / logoBitmap.height.toFloat()
            val drawHeight = maxLogoHeight
            val drawWidth = drawHeight * aspect
            val logoRect = RectF(
                card.left.toFloat() + padding,
                startY,
                card.left.toFloat() + padding + drawWidth,
                startY + drawHeight
            )
            canvas.drawBitmap(logoBitmap, null, logoRect, logoPaint)
            startY += drawHeight + (8f * scale)
        }

        val logoLeft = card.left.toFloat() + padding
        var textLeft = card.left.toFloat() + padding

        // 绘制 Logo：文字左侧 (Left)
        if (logoBitmap != null && logoPosition == LogoPosition.Left) {
            val maxLogoHeight = 54f * scale
            val aspect = logoBitmap.width.toFloat() / logoBitmap.height.toFloat()
            val drawHeight = maxLogoHeight
            val drawWidth = drawHeight * aspect
            val logoRect = RectF(
                logoLeft,
                startY,
                logoLeft + drawWidth,
                startY + drawHeight
            )
            canvas.drawBitmap(logoBitmap, null, logoRect, logoPaint)
            textLeft = logoLeft + drawWidth + (12f * scale)
        }

        var y = startY - titlePaint.fontMetrics.ascent
        val maxTextWidth = (card.right.toFloat() - padding - textLeft).coerceAtLeast(1f)
        val titleText = content.title

        // 绘制标题紫色 Tag
        val titleWidth = titlePaint.measureText(titleText)
        val tagPaddingX = 14f * scale
        val tagPaddingY = 6f * scale
        val tagRect = RectF(
            textLeft - 4f,
            startY - 2f,
            textLeft + titleWidth + tagPaddingX,
            startY + titlePaint.fontSpacing + tagPaddingY
        )
        val tagBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#7A36D6")
            alpha = alphaInt
        }
        canvas.drawRoundRect(tagRect, 8f * scale, 8f * scale, tagBgPaint)
        canvas.drawText(titleText, textLeft + tagPaddingX / 3f, y + tagPaddingY / 2f, titlePaint)

        y += titlePaint.fontSpacing * 1.45f
        content.lines.forEach { line ->
            if (y < card.bottom.toFloat() + padding + (60f * scale)) {
                canvas.drawText(ellipsize(line.text, linePaint, maxTextWidth), textLeft, y, linePaint)
                y += linePaint.fontSpacing * 1.15f
            }
        }

        // 绘制 Logo：文字下方 (Below)
        if (logoBitmap != null && logoPosition == LogoPosition.Below) {
            val maxLogoHeight = 44f * scale
            val aspect = logoBitmap.width.toFloat() / logoBitmap.height.toFloat()
            val drawHeight = maxLogoHeight
            val drawWidth = drawHeight * aspect
            val logoRect = RectF(
                textLeft,
                y + (6f * scale),
                textLeft + drawWidth,
                y + (6f * scale) + drawHeight
            )
            canvas.drawBitmap(logoBitmap, null, logoRect, logoPaint)
        }

        logoBitmap?.recycle()

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
