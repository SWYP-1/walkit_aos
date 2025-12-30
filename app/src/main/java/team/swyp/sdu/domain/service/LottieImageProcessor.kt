package team.swyp.sdu.domain.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.model.CosmeticItem
import team.swyp.sdu.domain.model.EquipSlot
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

    /**
     * 착용된 아이템들을 기반으로 Lottie JSON의 모든 슬롯 asset을 업데이트
     */
    suspend fun updateAssetsForWornItems(
        baseLottieJson: JSONObject,
        wornItemsByPosition: Map<EquipSlot, Int>,
        cosmeticItems: List<CosmeticItem>,
        character: Character
    ): JSONObject {
        Timber.d("🎨 LottieImageProcessor.updateAssetsForWornItems 시작")
        Timber.d("👤 캐릭터: ${character.nickName}")
        Timber.d("🧷 착용 상태: $wornItemsByPosition")
        Timber.d("📦 코스메틱 아이템 수: ${cosmeticItems.size}")

        return withContext(Dispatchers.IO) {
            try {
                var modifiedJson = baseLottieJson

                // 각 슬롯별 이미지 설정 생성 및 적용
                EquipSlot.entries.forEach { slot ->
                    Timber.d("🔍 슬롯 처리 시작: $slot")

                    val assetId = getAssetIdForSlot(slot)
                    val imageUrl = getImageUrlForSlot(slot, wornItemsByPosition, cosmeticItems, character)

                    Timber.d("📋 슬롯 $slot - assetId: $assetId, imageUrl: $imageUrl")

                    if (!imageUrl.isNullOrEmpty()) {
                        Timber.d("✅ Lottie asset 교체 실행: slot=${slot}, assetId=$assetId")
                        modifiedJson = replaceAssetWithImageUrl(modifiedJson, assetId, imageUrl)
                        Timber.d("✅ 슬롯 $slot asset 교체 완료")
                    } else {
                        Timber.d("⚠️ 슬롯 $slot 건너뜀 - imageUrl 없음")
                    }
                }

                Timber.d("🎉 모든 슬롯 asset 교체 완료")
                modifiedJson
            } catch (e: Exception) {
                Timber.e(e, "❌ Lottie asset들 교체 실패")
                baseLottieJson // 실패 시 원본 반환
            }
        }
    }

    /**
     * 슬롯별 asset ID 매핑
     */
    private fun getAssetIdForSlot(slot: EquipSlot): String {
        return when (slot) {
            EquipSlot.HEAD -> "head_ribbon"
            EquipSlot.BODY -> "body_cloth"
            EquipSlot.FEET -> "feet_shoes"
        }
    }

    /**
     * 슬롯별 이미지 URL 결정 (착용된 아이템 우선, 없으면 캐릭터 기본값)
     */
    private fun getImageUrlForSlot(
        slot: EquipSlot,
        wornItemsByPosition: Map<EquipSlot, Int>,
        cosmeticItems: List<CosmeticItem>,
        character: Character
    ): String? {
        Timber.d("🔍 getImageUrlForSlot: slot=$slot")

        val wornItemId = wornItemsByPosition[slot]
        Timber.d("🎯 슬롯 $slot 착용 아이템 ID: $wornItemId")

        return if (wornItemId != null) {
            // 착용된 아이템이 있으면 해당 아이템의 이미지
            val cosmeticItem = cosmeticItems.find { it.itemId == wornItemId }
            val imageUrl = cosmeticItem?.imageName
            Timber.d("🧷 착용 아이템 이미지: $imageUrl (item: ${cosmeticItem?.name})")
            imageUrl
        } else {
            // 착용된 아이템 없으면 캐릭터 기본값
            val defaultImageUrl = when (slot) {
                EquipSlot.HEAD -> character.headImageName
                EquipSlot.BODY -> character.bodyImageName
                EquipSlot.FEET -> character.feetImageName
            }
            Timber.d("🏠 캐릭터 기본 이미지: $defaultImageUrl")
            defaultImageUrl
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

