package com.lx.simplewatermarkcamera.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.lx.simplewatermarkcamera.domain.PhotoFileNameGenerator
import java.io.File
import java.time.Instant

data class SavedPhotoResult(
    val watermarkedUri: Uri,
    val originalUri: Uri?,
)

class PhotoStorageRepository(private val context: Context) {
    fun createTempCaptureFile(): File {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        return File(dir, PhotoFileNameGenerator.generate(Instant.now(), "TEMP"))
    }

    fun saveJpeg(bytes: ByteArray, fileName: String, subFolder: String): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/简易水印相机/$subFolder")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)) {
            "无法创建相册文件"
        }
        try {
            resolver.openOutputStream(uri).use { output ->
                requireNotNull(output) { "无法写入相册文件" }
                output.write(bytes)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
            }
            return uri
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }
}
