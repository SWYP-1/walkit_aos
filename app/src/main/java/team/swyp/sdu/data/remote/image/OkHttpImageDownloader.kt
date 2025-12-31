package team.swyp.sdu.data.remote.image

import okhttp3.OkHttpClient
import okhttp3.Request
import team.swyp.sdu.domain.service.ImageDownloader
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * OkHttp를 사용하여 이미지를 다운로드하는 구현체
 */
@Singleton
class OkHttpImageDownloader @Inject constructor(
    @Named("image") private val okHttpClient: OkHttpClient
) : ImageDownloader {

    override suspend fun downloadPngImage(imageUrl: String): ByteArray =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(imageUrl)
                    .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("이미지 다운로드 실패: HTTP ${response.code}")
                    }

                    response.body?.bytes()
                        ?: throw IllegalStateException("응답 본문이 비어있습니다")
                }
            } catch (e: Exception) {
                Timber.e(e, "이미지 다운로드 실패: $imageUrl")
                throw e
            }
        }



    companion object {
        private const val TAG = "OkHttpImageDownloader"
    }
}


