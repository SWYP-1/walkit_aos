package swyp.team.walkit.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.OutputStream
import java.net.URL

suspend fun downloadImage(
    context: Context,
    path: String,
    fileName: String
): File = withContext(Dispatchers.IO) {
    val outputFile = File(context.cacheDir, fileName)

    when {
        path.startsWith("http://") || path.startsWith("https://") -> {
            saveServerImageToGallery(context,path, fileName)
        }

        else -> {
            saveImageToGallery(context,path, fileName)
        }
    }

    outputFile
}

private fun downloadFromUrl(url: String, outputFile: File) {
    val client = OkHttpClient()

    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw IOException("Failed to download image: $response")
        }

        response.body?.byteStream()?.use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}

private fun copyLocalFile(path: String, outputFile: File) {
    val sourceFile = File(
        if (path.startsWith("file://")) {
            Uri.parse(path).path!!
        } else {
            path
        }
    )

    sourceFile.inputStream().use { input ->
        outputFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}


fun saveImageToGallery(context: Context, path: String, fileName: String) {
    val sourceFile = File(
        if (path.startsWith("file://")) Uri.parse(path).path!! else path
    )

    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyApp") // 갤러리 내 앱 폴더
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    if (uri != null) {
        resolver.openOutputStream(uri)?.use { output: OutputStream ->
            sourceFile.inputStream().use { input ->
                input.copyTo(output)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
    }
}

suspend fun saveServerImageToGallery(
    context: Context,
    imageUrl: String,
    fileName: String
) {
    withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyApp")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("Failed to create new MediaStore record")

            resolver.openOutputStream(uri)?.use { output: OutputStream ->
                // 서버 이미지 다운로드
                URL(imageUrl).openStream().use { input ->
                    input.copyTo(output)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
        } catch (e: Exception) {
            throw e // Compose에서 try/catch로 상태 처리 가능
        }
    }
}

