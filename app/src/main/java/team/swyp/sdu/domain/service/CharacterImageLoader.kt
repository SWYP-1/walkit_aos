package team.swyp.sdu.domain.service

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import team.swyp.sdu.domain.model.CharacterPart
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.LinkedHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 캐릭터 이미지 로드 및 캐싱을 담당하는 서비스
 *
 * 캐릭터 파트별 이미지를 로드하고, 실패시 투명 PNG를 생성합니다.
 * 메모리 캐시와 디스크 캐시를 지원합니다.
 */
@Singleton
class CharacterImageLoader @Inject constructor(
    private val imageDownloader: ImageDownloader
) {

    // 메모리 캐시 (LRU 방식)
    private val memoryCache = object : LinkedHashMap<String, ByteArray>(0, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>): Boolean {
            return size > MAX_MEMORY_CACHE_SIZE
        }
    }

    // 캐시 접근을 위한 뮤텍스
    private val cacheMutex = Mutex()

    /**
     * 캐릭터 파트별 이미지 로드
     * 캐시 → 네트워크 → 투명 PNG 순으로 폴백
     */
    suspend fun loadCharacterPartImage(
        imageName: String?,
        part: CharacterPart
    ): ByteArray {
        return withContext(Dispatchers.IO) {
            try {
                if (imageName.isNullOrBlank()) {
                    Timber.d("🔍 파트 ${part.name}: imageName이 없음, 투명 PNG 생성")
                    return@withContext createTransparentPng(256, 256)
                }

                // 1. 메모리 캐시 확인
                cacheMutex.withLock {
                    memoryCache[imageName]?.let { cached ->
                        Timber.d("💾 파트 ${part.name}: 캐시에서 이미지 로드 성공")
                        return@withContext cached
                    }
                }

                // 2. 네트워크에서 다운로드
                Timber.d("🌐 파트 ${part.name}: 네트워크에서 이미지 다운로드 시도 - $imageName")
                val imageData = imageDownloader.downloadPngImage(imageName)

                // 3. 캐시에 저장
                cacheMutex.withLock {
                    memoryCache[imageName] = imageData
                }

                Timber.d("✅ 파트 ${part.name}: 이미지 로드 및 캐시 저장 완료")
                imageData

            } catch (e: Exception) {
                Timber.e(e, "❌ 파트 ${part.name} 이미지 로드 실패: $imageName")
                // 실패시 투명 PNG로 폴백
                createTransparentPng(256, 256)
            }
        }
    }

    /**
     * 캐시 초기화 (메모리 부족시 호출)
     */
    fun clearCache() {
        memoryCache.clear()
        Timber.d("🧹 캐릭터 이미지 캐시 초기화")
    }

    /**
     * 캐시 상태 확인
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            memoryCacheSize = memoryCache.size,
            maxMemoryCacheSize = MAX_MEMORY_CACHE_SIZE
        )
    }

    companion object {
        private const val MAX_MEMORY_CACHE_SIZE = 20 // 최대 20개 이미지 캐시

        /**
         * 투명 PNG 생성 (로컬 함수)
         */
        private fun createTransparentPng(width: Int, height: Int): ByteArray {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            // 모든 픽셀을 완전 투명으로 설정
            bitmap.eraseColor(Color.TRANSPARENT)

            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()

            return output.toByteArray()
        }
    }
}

/**
 * 캐시 상태 정보
 */
data class CacheStats(
    val memoryCacheSize: Int,
    val maxMemoryCacheSize: Int
)

