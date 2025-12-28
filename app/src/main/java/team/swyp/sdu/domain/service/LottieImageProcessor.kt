package team.swyp.sdu.domain.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import team.swyp.sdu.utils.replaceAssetP
import team.swyp.sdu.utils.toBase64DataUrl
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lottie 애니메이션에서 이미지 URL을 Base64 PNG로 변환하여 교체하는 서비스
 *
 * 이 클래스는 전체 플로우의 orchestration만 담당하며,
 * 실제 작업은 ImageDownloader와 extension 함수들을 통해 수행됩니다.
 */
@Singleton
class LottieImageProcessor @Inject constructor(
    private val imageDownloader: ImageDownloader
) {

    /**
     * Lottie JSON의 특정 asset을 이미지 URL에서 다운로드한 PNG로 교체
     *
     * @param lottieJson 원본 Lottie JSON 객체
     * @param assetId 교체할 asset의 id
     * @param imageUrl 다운로드할 PNG 이미지의 URL
     * @return 수정된 JSONObject
     */
    suspend fun replaceAssetWithImageUrl(
        lottieJson: JSONObject,
        assetId: String,
        imageUrl: String
    ): JSONObject {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 이미지 다운로드
                val imageBytes = imageDownloader.downloadPngImage(imageUrl)

                // 👉 2️⃣ ⭐ 여기서 크기 조정 ⭐
                val resizedBytes = resizePngForSlot(
                    bytes = imageBytes,
                    slot = LottieImageSlot.HEAD
                )

                // 2. Base64 Data URL로 변환
                val dataUrl = resizedBytes.toBase64DataUrl()

                // 3. Lottie JSON에서 asset 교체
                lottieJson. replaceAssetP(assetId, dataUrl)
            } catch (e: Exception) {
                Timber.e(e, "Lottie asset 이미지 교체 실패: assetId=$assetId, imageUrl=$imageUrl")
                throw e
            }
        }
    }

    companion object {
        private const val TAG = "LottieImageProcessor"
    }
}

fun resizePngForSlot(
    bytes: ByteArray,
    slot: LottieImageSlot
): ByteArray {
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: throw IllegalArgumentException("Bitmap 디코딩 실패")

    val resized = Bitmap.createScaledBitmap(
        bitmap,
        slot.size,
        slot.size,
        true
    )

    val output = ByteArrayOutputStream()
    resized.compress(Bitmap.CompressFormat.PNG, 100, output)

    bitmap.recycle()
    resized.recycle()

    return output.toByteArray()
}


enum class LottieImageSlot(val size: Int) {
    HEAD(48),
    BODY(512),
    ACCESSORY(128)
}
