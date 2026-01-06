package swyp.team.walkit.utils

import android.util.Base64
import org.json.JSONObject
import timber.log.Timber

/**
 * Lottie 관련 확장 함수들
 */

/**
 * ByteArray를 Base64 Data URL로 변환
 * @return "data:image/png;base64,{base64String}" 형식의 Data URL
 */
fun ByteArray.toBase64DataUrl(): String {
    val base64String = Base64.encodeToString(this, Base64.NO_WRAP)
    return "data:image/png;base64,$base64String"
}

data class LottieAssetSize(
    val width: Int,
    val height: Int
)


/**
 * Lottie JSON에서 특정 asset의 p 값을 교체
 * @param assetId 교체할 asset의 id
 * @param dataUrl 교체할 데이터 URL (data:image/png;base64,... 형식)
 * @return 수정된 JSONObject
 * @throws IllegalArgumentException asset을 찾을 수 없거나 assets 배열이 없는 경우
 */
@Throws(IllegalArgumentException::class)
fun JSONObject.replaceAssetP(assetId: String, dataUrl: String): JSONObject {
    try {
        val assets = this.optJSONArray("assets")
            ?: throw IllegalArgumentException("Lottie JSON에 assets 배열이 없습니다")

        // assets 배열에서 해당 assetId를 찾기
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            Timber.d("Lottie asset: $asset")
            val currentAssetId = asset.optString("id", "")

            if (currentAssetId == assetId) {
                // p 값 교체
                asset.put("p", dataUrl)
                Timber.d("Lottie asset 교체 완료: $assetId")
                return this
            }
        }

        throw IllegalArgumentException("assetId '$assetId'를 찾을 수 없습니다")
    } catch (t: Throwable) {
        Timber.e(t, "Lottie asset 교체 실패: $assetId")
        throw t
    }
}
fun JSONObject.findAssetSize(assetId: String): LottieAssetSize {
    val assets = getJSONArray("assets")
    for (i in 0 until assets.length()) {
        val asset = assets.getJSONObject(i)
        if (asset.getString("id") == assetId) {
            return LottieAssetSize(
                width = asset.getInt("w"),
                height = asset.getInt("h")
            )
        }
    }
    error("assetId not found: $assetId")
}


