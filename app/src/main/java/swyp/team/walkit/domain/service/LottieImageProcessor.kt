package swyp.team.walkit.domain.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject
import swyp.team.walkit.domain.model.Character
import swyp.team.walkit.domain.model.CharacterPart

import swyp.team.walkit.domain.model.CosmeticItem
import swyp.team.walkit.domain.model.EquipSlot
import swyp.team.walkit.domain.model.Grade
import swyp.team.walkit.domain.model.WearState
import swyp.team.walkit.utils.LottieAssetSize
import swyp.team.walkit.utils.findAssetSize
import swyp.team.walkit.utils.replaceAssetP
import swyp.team.walkit.utils.toBase64DataUrl
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

/**
 * Lottie 아이템 교체 진행률 콜백 인터페이스
 */
interface LottieProgressCallback {
    fun onItemProgress(part: CharacterPart, assetId: String, completed: Boolean)
    fun onAllItemsCompleted(processedJson: String)
}

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
                val imageBytes = imageDownloader.downloadPngImage(imageUrl)

                // 2. asset 크기 확인 (없으면 기본 크기 사용)
                val assetSize = try {
                    lottieJson.findAssetSize(assetId)
                } catch (e: IllegalStateException) {
                    Timber.w("Asset 크기 정보 없음 (assetId: $assetId), 기본 크기 사용")
                    LottieAssetSize(256, 256)
                }

                // 3. 크기 조정
                val resizedBytes = resizePng(
                    bytes = imageBytes,
                    targetW = assetSize.width,
                    targetH = assetSize.height
                )

                // 4. Base64 Data URL로 변환
                val dataUrl = resizedBytes.toBase64DataUrl()

                // 5. Lottie JSON에서 asset 교체
                try {
                    lottieJson.replaceAssetP(assetId, dataUrl)
                } catch (e: IllegalArgumentException) {
                    Timber.w("Lottie asset을 찾을 수 없음: $assetId")
                    lottieJson
                }
            } catch (t: Throwable) {
                Timber.e(t, "Lottie asset 교체 실패: assetId=$assetId")
                lottieJson
            }
        }
    }

    /**
     * 변경된 슬롯만 선택적으로 업데이트 (CharacterPart 레벨로 최적화)
     */
    suspend fun updateAssetsForChangedSlots(
        baseLottieJson: JSONObject,
        wornItemsByPosition: Map<EquipSlot, WearState>,
        cosmeticItems: List<CosmeticItem>,
        character: Character,
        changedSlots: Set<EquipSlot>
    ): JSONObject {
        return withContext(Dispatchers.IO) {
            try {
                var modifiedJson = baseLottieJson

                // 변경된 슬롯만 처리
                changedSlots.forEach { slot ->
                    val wearState = wornItemsByPosition[slot]

                    // CharacterPart로 변환
                    val characterPart = when (slot) {
                        EquipSlot.HEAD -> CharacterPart.HEAD
                        EquipSlot.BODY -> CharacterPart.BODY
                        EquipSlot.FEET -> CharacterPart.FEET
                    }

                    // 해당 파트의 모든 asset ID들을 처리
                    characterPart.lottieAssetIds.forEach { assetId ->
                        val imageUrl = when (wearState) {
                            is WearState.Worn -> {
                                // 착용중인 코스메틱 아이템
                                val cosmeticItem =
                                    cosmeticItems.find { it.itemId == wearState.itemId }
                                if (cosmeticItem != null) {
                                    val targetAssetId =
                                        characterPart.getLottieAssetId(cosmeticItem.tags)
                                    if (assetId == targetAssetId) cosmeticItem.imageName else null
                                } else {
                                    Timber.w("❌ 착용중인 아이템을 찾을 수 없음: ${wearState.itemId}")
                                    null
                                }
                            }

                            WearState.Unworn -> {
                                // 미착용 상태 - 투명 PNG
                                if (slot == EquipSlot.FEET) {
                                    val defaultFeetImageUrl = when (character.grade) {
                                        Grade.SEED -> "https://kr.object.ncloudstorage.com/walkit-bucket/DEFAULT_SEED_FEET_IMAGE.png"
                                        Grade.SPROUT -> "https://kr.object.ncloudstorage.com/walkit-bucket/DEFAULT_SPROUT_FEET_IMAGE.png"
                                        Grade.TREE -> "https://kr.object.ncloudstorage.com/walkit-bucket/DEFAULT_TREE_FEET_IMAGE.png"
                                    }
                                    defaultFeetImageUrl
                                } else {
                                    null
                                }
                            }

                            WearState.Default -> {
                                // 캐릭터 기본값
                                when (slot) {
                                    EquipSlot.HEAD -> {
                                        val targetAssetId =
                                            CharacterPart.HEAD.getLottieAssetId(character.headImageTag)
                                        if (assetId == targetAssetId) character.headImageName else null
                                    }

                                    EquipSlot.BODY -> character.bodyImageName
                                    EquipSlot.FEET -> {
                                        // 캐릭터 등급에 따른 기본 FEET 이미지
                                        val defaultFeetImageUrl = when (character.grade) {
                                            Grade.SEED -> "https://kr.object.ncloudstorage.com/walkit-bucket/DEFAULT_SEED_FEET_IMAGE.png"
                                            Grade.SPROUT -> "https://kr.object.ncloudstorage.com/walkit-bucket/DEFAULT_SPROUT_FEET_IMAGE.png"
                                            Grade.TREE -> "https://kr.object.ncloudstorage.com/walkit-bucket/DEFAULT_TREE_FEET_IMAGE.png"
                                        }
                                        defaultFeetImageUrl
                                    }
                                }
                            }

                            null -> {
                                // 슬롯에 상태가 없음 - 투명 PNG
                                null
                            }
                        }

                        if (imageUrl != null && imageUrl.isNotEmpty()) {
                            modifiedJson = replaceAssetWithImageUrl(modifiedJson, assetId, imageUrl)
                        } else {
                            // 투명 PNG로 교체하여 stroke 제거
                            val transparentPng = createTransparentPng(256, 256)
                            modifiedJson =
                                replaceAssetWithByteArray(modifiedJson, assetId, transparentPng)
                        }
                    }
                }

                modifiedJson
            } catch (t: Throwable) {
                Timber.e(t, "❌ 변경된 슬롯 asset 교체 실패")
                baseLottieJson // 실패 시 원본 반환
            }
        }
    }

    /**
     * 캐릭터 기본 이미지들을 baseJson에 적용하여 기본 캐릭터 Lottie 생성
     */
    suspend fun applyCharacterDefaultsToBaseJson(
        baseLottieJson: JSONObject,
        character: Character
    ): JSONObject {
        return withContext(Dispatchers.IO) {
            try {
                var modifiedJson = baseLottieJson

                // 각 슬롯의 캐릭터 기본값 적용
                EquipSlot.values().forEach { slot ->

                    val characterPart = when (slot) {
                        EquipSlot.HEAD -> CharacterPart.HEAD
                        EquipSlot.BODY -> CharacterPart.BODY
                        EquipSlot.FEET -> CharacterPart.FEET
                    }

                    // 해당 파트의 모든 asset ID들을 처리
                    characterPart.lottieAssetIds.forEach { assetId ->
                        val imageUrl = when (slot) {
                            EquipSlot.HEAD -> {
                                // HEAD의 경우 tag에 따라 정확한 영역에 적용 (기본값: headtop)
                                val targetAssetId =
                                    CharacterPart.HEAD.getLottieAssetId(character.headImageTag)
                                val shouldApply = assetId == targetAssetId
                                if (shouldApply) character.headImageName else null
                            }

                            EquipSlot.BODY -> character.bodyImageName
                            EquipSlot.FEET -> character.feetImageName
                        }

                        if (imageUrl != null && imageUrl.isNotEmpty()) {
                            modifiedJson = replaceAssetWithImageUrl(modifiedJson, assetId, imageUrl)
                        } else {
                            // 투명 PNG로 교체하여 stroke 제거
                            val transparentPng = createTransparentPng(256, 256)
                            modifiedJson =
                                replaceAssetWithByteArray(modifiedJson, assetId, transparentPng)
                        }
                    }
                }

                modifiedJson
            } catch (t: Throwable) {
                Timber.e(t, "❌ 캐릭터 기본 이미지 적용 실패")
                baseLottieJson // 실패 시 원본 반환
            }
        }
    }

    /**
     * 기본 캐릭터 Lottie에 착용된 아이템들을 덮어씌워 최종 Lottie 생성
     */
    suspend fun updateAssetsForWornItems(
        baseCharacterJson: JSONObject,
        wornItemsByPosition: Map<EquipSlot, Int>,
        cosmeticItems: List<CosmeticItem>
    ): JSONObject {
        return withContext(Dispatchers.IO) {
            try {
                var modifiedJson = baseCharacterJson

                // 각 슬롯별 코스메틱 아이템 적용
                EquipSlot.values().forEach { slot ->

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
                        val imageUrl = if (cosmeticItem != null) {
                            // 코스메틱 아이템이 있으면 tags에 따라 적용
                            val targetAssetId = characterPart.getLottieAssetId(cosmeticItem.tags)
                            if (assetId == targetAssetId) cosmeticItem.imageName else null
                        } else {
                            // 코스메틱 아이템이 없으면 null (기본 캐릭터 이미지가 이미 적용되어 있음)
                            null
                        }

                        if (imageUrl != null && imageUrl.isNotEmpty()) {
                            modifiedJson = replaceAssetWithImageUrl(modifiedJson, assetId, imageUrl)
                        }
                        // 코스메틱 아이템이 없거나 해당 asset에 적용되지 않으면 기본 캐릭터 이미지가 유지됨
                    }
                }

                modifiedJson
            } catch (t: Throwable) {
                Timber.e(t, "❌ 코스메틱 아이템 적용 실패")
                baseCharacterJson // 실패 시 원본 반환
            }
        }
    }


    /**
     * 캐릭터 데이터의 각 파트를 Lottie JSON asset으로 교체
     * null이나 빈 값인 경우 투명 PNG로 교체
     */
    suspend fun updateCharacterPartsInLottie(
        baseLottieJson: JSONObject,
        character: Character,
        progressCallback: LottieProgressCallback? = null
    ): JSONObject {
        return withContext(Dispatchers.IO) {
            try {
                var modifiedJson = baseLottieJson

                // 총 작업 수 계산 (HEAD: 여러 asset, BODY/FEET: 각 1개)
                val totalTasks = CharacterPart.values().size + (CharacterPart.HEAD.lottieAssetIds.size - 1)
                var completedTasks = 0


                // 각 캐릭터 파트 처리
                CharacterPart.values().forEach { part ->
                    val imageName = getImageNameForPart(character, part)

                    Timber.d("🔄 파트 ${part.name} 처리: imageName=$imageName")

                    when (part) {
                        CharacterPart.HEAD -> {
                            // HEAD 파트는 tag를 고려해서 하나의 assetId에만 적용
                            // character.headImageTag를 우선 사용, 없으면 이미지 파일명에서 힌트 추출
                            val targetAssetId = character.headImageTag?.let {
                                CharacterPart.HEAD.getLottieAssetId(it)
                            } ?: run {
                                // headImageTag가 없으면 이미지 URL에서 힌트 추출 시도
                                "headtop"
                            }
                            part.lottieAssetIds.forEach { assetId ->
                                val shouldApplyImage =
                                    !imageName.isNullOrBlank() && assetId == targetAssetId
                                val shouldApplyTransparent =
                                    imageName.isNullOrBlank() || assetId != targetAssetId

                            if (shouldApplyImage) {
                                // 실제 이미지가 있고 target asset이면 이미지 적용
                                Timber.d("🎨 파트 ${part.name} asset ${assetId}: 이미지 '${imageName}'로 교체 시작")
                                modifiedJson =
                                    replaceAssetWithImageUrl(modifiedJson, assetId, imageName)
                            } else if (shouldApplyTransparent) {
                                // 이미지가 없거나 target asset이 아니면 투명 PNG로 교체
                                val transparentPng = createTransparentPng(256, 256)
                                modifiedJson =
                                    replaceAssetWithByteArray(
                                        modifiedJson,
                                        assetId,
                                        transparentPng
                                    )
                            }

                            // 진행률 업데이트
                            completedTasks++
                            progressCallback?.onItemProgress(part, assetId, true)
                            }
                        }

                        else -> {
                            // BODY, FEET 파트는 기존처럼 모든 assetId에 동일하게 적용
                            part.lottieAssetIds.forEach { assetId ->
                                try {
                                    if (!imageName.isNullOrBlank()) {
                                        // 실제 이미지가 있으면 다운로드하여 교체
                                        Timber.d("🎨 파트 ${part.name} asset ${assetId}: 이미지 '${imageName}'로 교체 시작")
                                        modifiedJson =
                                            replaceAssetWithImageUrl(modifiedJson, assetId, imageName)
                                    } else {
                                        // 이미지가 없으면 투명 PNG로 교체
                                        val transparentPng = createTransparentPng(256, 256)
                                        modifiedJson =
                                            replaceAssetWithByteArray(
                                                modifiedJson,
                                                assetId,
                                                transparentPng
                                            )
                                    }

                                    // 진행률 업데이트
                                    completedTasks++
                                    progressCallback?.onItemProgress(part, assetId, true)

                                } catch (e: Exception) {
                                    Timber.e(e, "파트 ${part.name} asset ${assetId} 처리 실패")
                                    progressCallback?.onItemProgress(part, assetId, false)
                                }
                            }
                        }
                    }
                }

                // 모든 작업 완료 콜백
                progressCallback?.onAllItemsCompleted(modifiedJson.toString())

                modifiedJson
            } catch (t: Throwable) {
                Timber.e(t, "❌ 캐릭터 파트 교체 실패")
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
            CharacterPart.FEET -> {
                // FEET의 경우 null이면 캐릭터 등급에 따른 기본 이미지 사용
                character.feetImageName ?: when (character.grade) {
                    Grade.SEED -> "https://kr.object.ncloudstorage.com/walkit-bucket/DEFAULT_SEED_FEET_IMAGE.png"
                    Grade.SPROUT -> "https://kr.object.ncloudstorage.com/walkit-bucket/DEFAULT_SPROUT_FEET_IMAGE.png"
                    Grade.TREE -> "https://kr.object.ncloudstorage.com/walkit-bucket/DEFAULT_TREE_FEET_IMAGE.png"
                }
            }
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
        } catch (t: Throwable) {
            Timber.e(t, "ByteArray asset 교체 실패: assetId=$assetId")
            throw t
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
        } catch (t: Throwable) {
            Timber.e(t, "PNG 크기 추출 실패")
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

        val output = ByteArrayOutputStream()
        val success = resized.compress(Bitmap.CompressFormat.PNG, 100, output)
        if (!success) {
            Timber.e("❌ PNG 압축 실패")
            throw IllegalStateException("PNG 압축 실패")
        }

        val resultBytes = output.toByteArray()

        original.recycle()
        resized.recycle()

        return resultBytes
    } catch (t: Throwable) {
        Timber.e(t, "❌ resizePng 실패: bytes=${bytes.size}, target=${targetW}x${targetH}")
        throw t
    }
}

enum class LottieImageSlot(val size: Int) {
    HEAD(48),
    BODY(512),
    ACCESSORY(128)
}

