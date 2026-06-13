package com.lx.simplewatermarkcamera.data

import android.content.Context
import android.net.Uri
import java.io.File

object LogoStore {
    fun copyToPrivateStorage(context: Context, source: Uri): String {
        val target = File(context.filesDir, "watermark-logo")
        context.contentResolver.openInputStream(source).use { input ->
            requireNotNull(input) { "无法读取 logo" }
            target.outputStream().use { output -> input.copyTo(output) }
        }
        return target.absolutePath
    }
}
