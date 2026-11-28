package com.f4f.arena.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object InAppUpdater {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun downloadAndInstall(
        context: Context,
        apkUrl: String,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val apkFile = File(updatesDir, "f4f-arena-update.apk")
            if (apkFile.exists()) apkFile.delete()

            val request = Request.Builder().url(apkUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Download failed (HTTP ${response.code})"))
                }
                val body = response.body ?: return@withContext Result.failure(Exception("Empty download response"))
                val total = body.contentLength()
                var downloaded = 0L
                val buf = ByteArray(16 * 1024)

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        while (true) {
                            val read = input.read(buf)
                            if (read == -1) break
                            output.write(buf, 0, read)
                            downloaded += read
                            if (total > 0) {
                                withContext(Dispatchers.Main) {
                                    onProgress((downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                                }
                            }
                        }
                    }
                }
            }

            withContext(Dispatchers.Main) {
                installApkFile(context, apkFile)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun installApkFile(context: Context, apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val permIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(permIntent)
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
