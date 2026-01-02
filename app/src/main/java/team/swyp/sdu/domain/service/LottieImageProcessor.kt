package team.swyp.sdu.domain.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import team.swyp.sdu.domain.model.Character
import team.swyp.sdu.domain.model.CharacterPart
import team.swyp.sdu.domain.model.CosmeticItem
import team.swyp.sdu.domain.model.EquipSlot
import team.swyp.sdu.utils.LottieAssetSize
import team.swyp.sdu.utils.findAssetSize
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
        return withContext<JSONObject>(Dispatchers.IO) {
            try {
                // 1. 이미지 다운로드
                Timber.d("📥 이미지 다운로드 시작: $imageUrl")
                val imageBytes = imageDownloader.downloadPngImage(imageUrl)
                Timber.d("📥 이미지 다운로드 완료: ${imageBytes.size} bytes")

                // 2. asset 크기 확인 (없으면 기본 크기 사용)
                val assetSize = try {
                    val size = lottieJson.findAssetSize(assetId)
                    Timber.d("📏 Asset '$assetId' 크기 찾기 성공: ${size.width}x${size.height}")
                    size
                } catch (e: IllegalStateException) {
                    Timber.w("⚠️ Asset 크기 정보를 찾을 수 없음 (assetId: $assetId), 기본 크기 256x256 사용")
                    LottieAssetSize(256, 256)
                }

                // 👉 3️⃣ ⭐ 여기서 크기 조정 ⭐
                Timber.d("🔄 크기 조정 시작: ${imageBytes.size} bytes → ${assetSize.width}x${assetSize.height}")
                val resizedBytes = resizePng(
                    bytes = imageBytes,
                    targetW = assetSize.width,
                    targetH = assetSize.height
                )
                Timber.d("🔄 크기 조정 완료: ${resizedBytes.size} bytes")

                // 4. Base64 Data URL로 변환
                Timber.d("🔗 Base64 변환 시작: ${resizedBytes.size} bytes")
                val dataUrl = resizedBytes.toBase64DataUrl()
                Timber.d("🔗 Base64 변환 완료: ${dataUrl.length} chars, 형식: ${dataUrl.startsWith("data:image/png;base64,")}")

                // 5. Lottie JSON에서 asset 교체 (없으면 건너뜀)
                try {
                    Timber.d("🔄 Lottie asset 교체 시도: $assetId")
                    val resultJson = lottieJson.replaceAssetP(assetId, dataUrl)
                    Timber.d("✅ Lottie asset 교체 성공: $assetId")
                    resultJson // 수정된 JSON 반환
                } catch (e: IllegalArgumentException) {
                    Timber.w("⚠️ Lottie asset을 찾을 수 없음, 교체 건너뜀: $assetId")
                    // asset이 없으면 원본 JSON 반환
                    lottieJson
                }
            } catch (e: Exception) {
                Timber.e(e, "Lottie asset 이미지 교체 실패: assetId=$assetId, imageUrl=$imageUrl")
                // 실패 시 원본 JSON 반환 (예외 던지지 않음)
                lottieJson
            }
        }
    }

    /**
     * 변경된 슬롯만 선택적으로 업데이트 (CharacterPart 레벨로 최적화)
     */
    suspend fun updateAssetsForChangedSlots(
        baseLottieJson: JSONObject,
        wornItemsByPosition: Map<EquipSlot, Int>,
        cosmeticItems: List<CosmeticItem>,
        character: Character,
        changedSlots: Set<EquipSlot>
    ): JSONObject {
        Timber.d("🎯 LottieImageProcessor.updateAssetsForChangedSlots 시작")
        Timber.d("🔄 변경된 슬롯들: $changedSlots")

        return withContext(Dispatchers.IO) {
            try {
                var modifiedJson = baseLottieJson

                // 변경된 슬롯만 처리
                changedSlots.forEach { slot ->
                    Timber.d("🎯 슬롯 처리 시작: $slot")

                    val wornItemId = wornItemsByPosition[slot]
                    val cosmeticItem = cosmeticItems.find { it.itemId == wornItemId }

                    // CharacterPart로 변환
                    val characterPart = when (slot) {
                        EquipSlot.HEAD -> CharacterPart.HEAD
                        EquipSlot.BODY -> CharacterPart.BODY
                        EquipSlot.FEET -> CharacterPart.FEET
                    }

                    // 해당 파트의 모든 asset ID들을 처리
                    characterPart.lottieAssetIds.forEach { assetId ->
                        val imageUrl = getImageUrlForSlot(slot, wornItemsByPosition, cosmeticItems, character)
                        val finalAssetId = characterPart.getLottieAssetId(cosmeticItem?.tags)

                        Timber.d("📋 슬롯 $slot - assetId: $finalAssetId, imageUrl: $imageUrl")

                        if (imageUrl != null && imageUrl.isNotEmpty()) {
                            Timber.d("✅ Lottie asset 교체 실행: slot=${slot}, assetId=$finalAssetId")
                            modifiedJson = replaceAssetWithImageUrl(modifiedJson, finalAssetId, imageUrl)
                            Timber.d("✅ 슬롯 $slot asset $finalAssetId 교체 완료")
                        } else {
                            Timber.d("🔍 슬롯 $slot asset $finalAssetId 이미지 없음 - 투명 PNG로 교체")
                            // 투명 PNG로 교체하여 stroke 제거
                            val transparentPng = createTransparentPng(256, 256)
                            modifiedJson =
                                replaceAssetWithByteArray(modifiedJson, finalAssetId, transparentPng)
                            Timber.d("✅ 슬롯 $slot asset $finalAssetId 투명 PNG로 교체 완료")
                        }
                    }
                }

                Timber.d("🎉 변경된 슬롯 asset 교체 완료")
                modifiedJson
            } catch (e: Exception) {
                Timber.e(e, "❌ 변경된 슬롯 asset 교체 실패")
                baseLottieJson // 실패 시 원본 반환
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
        Timber.d("👤 캐릭터: ${character.nickName}, level: ${character.level}, grade: ${character.grade}")
        Timber.d("🧷 착용 상태: $wornItemsByPosition")
        Timber.d("📦 코스메틱 아이템 수: ${cosmeticItems.size}")
        val level = character.level

        // 🔍 baseJson 유효성 검증
        if (baseLottieJson.length() == 0) {
            Timber.e("❌ baseJson이 빈 객체입니다! Lottie 파일 로드 실패")
            return baseLottieJson // 빈 객체 그대로 반환
        }

        Timber.d("✅ baseJson 유효함, 길이: ${baseLottieJson.toString().length}")

        // 디버깅: baseJson의 assets 구조 확인
        try {
            val assets = baseLottieJson.optJSONArray("assets")
            if (assets != null) {
                Timber.d("📋 Base Lottie assets 개수: ${assets.length()}")
                for (i in 0 until minOf(assets.length(), 10)) {
                    val asset = assets.optJSONObject(i)
                    val id = asset?.optString("id", "unknown")
                    val w = asset?.optInt("w", 0)
                    val h = asset?.optInt("h", 0)
                    Timber.d("📋 Asset[$i]: id=$id, size=${w}x${h}")
                }
            } else {
                Timber.e("❌ Base Lottie에 assets 배열이 없음 - JSON 구조 문제")
                // assets가 없으면 다른 필드들도 확인
                val keys = baseLottieJson.keys()
                while (keys.hasNext()) {
                    Timber.d("📋 JSON 키: ${keys.next()}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Base Lottie assets 구조 확인 실패")
        }

        return withContext(Dispatchers.IO) {
            try {
                var modifiedJson = baseLottieJson

                // 각 슬롯별 이미지 설정 생성 및 적용
                EquipSlot.entries.forEach { slot ->
                    Timber.d("🔍 슬롯 처리 시작: $slot")

                    val wornItemId = wornItemsByPosition[slot]
                    val cosmeticItem = cosmeticItems.find { it.itemId == wornItemId }
                    val assetId = getAssetIdForSlot(slot, item = cosmeticItem)
                    val imageUrl =
                        getImageUrlForSlot(slot, wornItemsByPosition, cosmeticItems, character)

                    Timber.d("📋 슬롯 $slot - assetId: $assetId, imageUrl: $imageUrl")

                    if (imageUrl != null && imageUrl.isNotEmpty()) {
                        Timber.d("✅ Lottie asset 교체 실행: slot=${slot}, assetId=$assetId")
                        modifiedJson = replaceAssetWithImageUrl(modifiedJson, assetId, imageUrl)
                        Timber.d("✅ 슬롯 $slot asset 교체 완료")
                    } else {
                        Timber.d("🔍 슬롯 $slot 이미지 없음 - 투명 PNG로 교체")
                        // 투명 PNG로 교체하여 stroke 제거
                        val transparentPng = createTransparentPng(256, 256)
                        modifiedJson =
                            replaceAssetWithByteArray(modifiedJson, assetId, transparentPng)
                        Timber.d("✅ 슬롯 $slot 투명 PNG로 교체 완료")
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
     * 슬롯별 asset ID 매핑 (level 기반)
     */
    private fun getAssetIdForSlot(slot: EquipSlot, item: CosmeticItem?): String {
        val part = when (slot) {
            EquipSlot.HEAD -> CharacterPart.HEAD
            EquipSlot.BODY -> CharacterPart.BODY
            EquipSlot.FEET -> CharacterPart.FEET
        }
        return part.getLottieAssetId()
    }

    /**
     * 슬롯별 이미지 URL 결정 (착용된 아이템 우선, 없으면 캐릭터 기본값)
     * 캐릭터 기본 이미지가 없으면 투명 PNG로 교체하여 stroke 제거
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

            // 캐릭터 기본 이미지가 없으면 null을 반환하여 투명 PNG로 교체
            if (defaultImageUrl.isNullOrBlank()) {
                Timber.d("🔍 캐릭터 기본 이미지 없음 - 투명 PNG로 교체")
                null // null 반환 시 투명 PNG로 교체
            } else {
                defaultImageUrl
            }
        }
    }

    /**
     * 캐릭터 데이터의 각 파트를 Lottie JSON asset으로 교체
     * null이나 빈 값인 경우 투명 PNG로 교체
     */
    suspend fun updateCharacterPartsInLottie(
        baseLottieJson: JSONObject,
        character: Character
    ): JSONObject {
        Timber.d("🎭 LottieImageProcessor.updateCharacterPartsInLottie 시작")
        Timber.d("👤 캐릭터 파트: head=${character.headImageName}, body=${character.bodyImageName}, feet=${character.feetImageName}")

        return withContext(Dispatchers.IO) {
            try {
                var modifiedJson = baseLottieJson

                // 각 캐릭터 파트 처리
                CharacterPart.entries.forEach { part ->
                    val imageName = getImageNameForPart(character, part)
                    val assetId = part.getLottieAssetId()

                    Timber.d("🔄 파트 ${part.name} 처리: imageName=$imageName, assetId=$assetId")

                    if (!imageName.isNullOrBlank()) {
                        // 실제 이미지가 있으면 다운로드하여 교체
                        Timber.d("🎨 파트 ${part.name}: 이미지 '${imageName}'로 교체 시작")
                        modifiedJson = replaceAssetWithImageUrl(modifiedJson, assetId, imageName)
                        Timber.d("✅ 파트 ${part.name} 이미지 교체 완료")
                    } else {
                        // 이미지가 없으면 투명 PNG로 교체
                        val transparentPng = createTransparentPng(256, 256)
                        Timber.d("🔍 파트 ${part.name}: 투명 PNG 생성 (크기: ${transparentPng.size} bytes)")
                        modifiedJson =
                            replaceAssetWithByteArray(modifiedJson, assetId, transparentPng)
                        Timber.d("🔍 파트 ${part.name} 투명 PNG로 교체 완료")
                    }
                }

                Timber.d("🎉 모든 캐릭터 파트 교체 완료")
                modifiedJson
            } catch (e: Exception) {
                Timber.e(e, "❌ 캐릭터 파트 교체 실패")
                baseLottieJson // 실패 시 원본 반환
            }
        }
    }

    /**
     * 캐릭터 파트별 imageName 추출
     */
    private fun getImageNameForPart(character: Character, part: CharacterPart): String? {
        return when (part) {
            CharacterPart.HEAD -> character.headImageName
            CharacterPart.BODY -> character.bodyImageName
            CharacterPart.FEET -> character.feetImageName
        }
    }

    /**
     * Lottie JSON의 asset을 ByteArray 이미지 데이터로 직접 교체
     */
    private fun replaceAssetWithByteArray(
        lottieJson: JSONObject,
        assetId: String,
        imageBytes: ByteArray
    ): JSONObject {
        try {
            // 0. 디버깅: 모든 asset ID 로깅
            val assets = lottieJson.optJSONArray("assets")
            if (assets != null) {
                Timber.d("🔍 Lottie JSON의 모든 assets (${assets.length()}개):")
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val id = asset.optString("id", "no-id")
                    val w = asset.optInt("w", 0)
                    val h = asset.optInt("h", 0)
                    val hasP = asset.has("p")
                    Timber.d("  - Asset $i: id='$id', size=${w}x${h}, hasP=$hasP")
                    if (id == assetId) {
                        Timber.d("    🎯 찾은 asset! id='$id'")
                    }
                }
            } else {
                Timber.e("❌ Lottie JSON에 assets 배열이 없습니다!")
            }

            Timber.d("🎯 찾으려는 assetId: '$assetId'")

            // 1. asset 크기 확인
            val assetSize = lottieJson.findAssetSize(assetId)
            Timber.d("📏 Asset '$assetId' 크기: ${assetSize.width}x${assetSize.height}")

            // 2. 이미지 크기 조정 (필요시)
            val originalSize = getPngSize(imageBytes)
            Timber.d("🖼️ 원본 이미지 크기: ${originalSize?.width}x${originalSize?.height}")

            val resizedBytes = if (assetSize.width > 0 && assetSize.height > 0) {
                Timber.d("🔄 이미지 크기 조정: ${originalSize?.width}x${originalSize?.height} → ${assetSize.width}x${assetSize.height}")
                resizePng(imageBytes, assetSize.width, assetSize.height)
            } else {
                Timber.d("⚠️ Asset 크기를 알 수 없어 원본 이미지 사용")
                imageBytes
            }

            // 3. Base64 Data URL로 변환
            val dataUrl = resizedBytes.toBase64DataUrl()
            Timber.d("🔗 생성된 Data URL 길이: ${dataUrl.length}, 시작: ${dataUrl.take(50)}...")

            // 4. Lottie JSON에서 asset 교체
            return lottieJson.replaceAssetP(assetId, dataUrl)
        } catch (e: Exception) {
            Timber.e(e, "ByteArray asset 교체 실패: assetId=$assetId")
            throw e
        }
    }

    /**
     * PNG 바이트 배열에서 이미지 크기 추출
     */
    private fun getPngSize(pngBytes: ByteArray): LottieAssetSize? {
        return try {
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size, options)
            LottieAssetSize(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            Timber.e(e, "PNG 크기 추출 실패")
            null
        }
    }

    companion object {
        private const val TAG = "LottieImageProcessor"
    }
}

/**
 * 투명 PNG 생성 함수
 */
fun createTransparentPng(width: Int, height: Int): ByteArray {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    // 모든 픽셀을 완전 투명으로 설정
    bitmap.eraseColor(Color.TRANSPARENT)

    val output = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    bitmap.recycle()

    return output.toByteArray()
}

fun resizePng(bytes: ByteArray, targetW: Int, targetH: Int): ByteArray {
    try {
        Timber.d("🔄 resizePng 시작: ${bytes.size} bytes → ${targetW}x${targetH}")

        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (original == null) {
            Timber.e("❌ Bitmap 디코딩 실패: bytes 크기=${bytes.size}")
            throw IllegalArgumentException("Bitmap 디코딩 실패")
        }
        Timber.d("✅ Bitmap 디코딩 성공: ${original.width}x${original.height}")

        if (targetW <= 0 || targetH <= 0) {
            Timber.e("❌ 잘못된 타겟 크기: ${targetW}x${targetH}")
            throw IllegalArgumentException("잘못된 타겟 크기: ${targetW}x${targetH}")
        }

        val resized = Bitmap.createScaledBitmap(
            original,
            targetW,
            targetH,
            true
        )
        Timber.d("✅ 크기 조정 성공: ${resized.width}x${resized.height}")

        val output = ByteArrayOutputStream()
        val success = resized.compress(Bitmap.CompressFormat.PNG, 100, output)
        if (!success) {
            Timber.e("❌ PNG 압축 실패")
            throw IllegalStateException("PNG 압축 실패")
        }

        val resultBytes = output.toByteArray()
        Timber.d("✅ PNG 압축 성공: ${resultBytes.size} bytes")

        original.recycle()
        resized.recycle()

        return resultBytes
    } catch (e: Exception) {
        Timber.e(e, "❌ resizePng 실패: bytes=${bytes.size}, target=${targetW}x${targetH}")
        throw e
    }
}

enum class LottieImageSlot(val size: Int) {
    HEAD(48),
    BODY(512),
    ACCESSORY(128)
}

