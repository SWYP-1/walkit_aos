package team.swyp.sdu.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

suspend fun downloadImage(
    context: Context,
    path: String,
    fileName: String
): File = withContext(Dispatchers.IO) {
    val outputFile = File(context.cacheDir, fileName)

    when {
        path.startsWith("http://") || path.startsWith("https://") -> {
            downloadFromUrl(path, outputFile)
        }

        else -> {
            copyLocalFile(path, outputFile)
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
