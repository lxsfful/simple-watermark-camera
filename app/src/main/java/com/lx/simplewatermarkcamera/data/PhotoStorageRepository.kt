package com.lx.simplewatermarkcamera.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.lx.simplewatermarkcamera.domain.LocationSnapshot
import com.lx.simplewatermarkcamera.domain.PhotoFileNameGenerator
import java.io.File
import java.time.Instant

import kotlin.math.roundToInt

data class SavedPhotoResult(
    val watermarkedUri: Uri,
    val originalUri: Uri?,
)

class PhotoStorageRepository(private val context: Context) {
    fun createTempCaptureFile(): File {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        return File(dir, PhotoFileNameGenerator.generate(Instant.now(), "TEMP"))
    }

    fun cleanupOldTempFiles(maxAgeMs: Long = 24 * 60 * 60 * 1000L) {
        val dir = File(context.cacheDir, "captures")
        if (!dir.exists() || !dir.isDirectory) return
        val now = System.currentTimeMillis()
        dir.listFiles()?.forEach { file ->
            if (now - file.lastModified() > maxAgeMs) {
                file.delete()
            }
        }
    }

    fun saveJpeg(
        bytes: ByteArray,
        fileName: String,
        subFolder: String,
        locationForExif: LocationSnapshot? = null,
    ): Uri {
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
            if (locationForExif != null) {
                writeGpsExif(uri, locationForExif)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
            }
            runCatching {
                MediaScannerConnection.scanFile(context, arrayOf(uri.path), arrayOf("image/jpeg"), null)
            }
            return uri
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    fun delete(uri: Uri) {
        context.contentResolver.delete(uri, null, null)
    }

    private fun writeGpsExif(uri: Uri, location: LocationSnapshot) {
        runCatching {
            resolverOpenFileDescriptor(uri)?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                exif.setGpsInfo(location)
                exif.saveAttributes()
            }
        }
    }

    private fun resolverOpenFileDescriptor(uri: Uri) =
        context.contentResolver.openFileDescriptor(uri, "rw")

    companion object {
        fun ExifInterface.setGpsInfo(location: LocationSnapshot) {
            val lat = location.latitude
            val latRef = if (lat >= 0) "N" else "S"
            setAttribute(ExifInterface.TAG_GPS_LATITUDE, decToDms(Math.abs(lat)))
            setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latRef)

            val lon = location.longitude
            val lonRef = if (lon >= 0) "E" else "W"
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE, decToDms(Math.abs(lon)))
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, lonRef)

            location.altitudeMeters?.let { altitude ->
                val altRef = if (altitude >= 0) "0" else "1"
                val altRatio = "${(Math.abs(altitude) * 1000).toInt()}/1000"
                setAttribute(ExifInterface.TAG_GPS_ALTITUDE, altRatio)
                setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, altRef)
            }
        }

        fun decToDms(decimal: Double): String {
            val deg = decimal.toInt()
            val minFull = (decimal - deg) * 60
            val min = minFull.toInt()
            val sec = ((minFull - min) * 60 * 1000).roundToInt()
            return "$deg/1,$min/1,$sec/1000"
        }
    }
}
