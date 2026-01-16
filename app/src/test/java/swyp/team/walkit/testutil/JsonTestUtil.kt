package swyp.team.walkit.testutil

import android.content.Context
import kotlinx.serialization.json.Json
import swyp.team.walkit.data.model.LocationPoint

/**
 * JSON 파일 로드 및 파싱을 위한 테스트 유틸리티 클래스
 */
object JsonTestUtil {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * raw 리소스의 JSON 파일을 읽어서 List<LocationPoint>로 변환
     *
     * @param fileName raw 리소스의 파일명 (확장자 제외)
     * @return 위치 데이터 리스트, 실패 시 빈 리스트
     */
    fun loadLocationsFromJson(context: Context, fileName: String = "temp"): List<LocationPoint> {
        return try {
            // JSON 파일 읽기 (raw 리소스)
            val resourceId = context.resources.getIdentifier(fileName, "raw", context.packageName)
            if (resourceId == 0) {
                println("❌ $fileName.json 파일을 찾을 수 없습니다.")
                return emptyList()
            }

            val inputStream = context.resources.openRawResource(resourceId)
            val jsonString = inputStream.bufferedReader().use { it.readText() }

            // JSON 파싱
            json.decodeFromString<List<LocationPoint>>(jsonString)

        } catch (e: Exception) {
            println("❌ $fileName.json 로드 실패: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * JSON 문자열을 List<LocationPoint>로 파싱
     *
     * @param jsonString JSON 문자열
     * @return 파싱된 위치 데이터 리스트
     */
    fun parseLocationsFromJsonString(jsonString: String): List<LocationPoint> {
        return try {
            json.decodeFromString<List<LocationPoint>>(jsonString)
        } catch (e: Exception) {
            println("❌ JSON 파싱 실패: ${e.message}")
            emptyList()
        }
    }

    /**
     * 위치 데이터의 유효성을 검증
     *
     * @param locations 검증할 위치 데이터 리스트
     * @return 검증 결과 메시지
     */
    fun validateLocations(locations: List<LocationPoint>): String {
        if (locations.isEmpty()) {
            return "❌ 위치 데이터가 비어있습니다."
        }

        val first = locations.first()

        // 기본 필드 검증
        if (first.latitude == 0.0 && first.longitude == 0.0) {
            return "❌ 위도/경도가 0입니다. 유효하지 않은 데이터일 수 있습니다."
        }

        // 타임스탬프 검증
        val currentTime = System.currentTimeMillis()
        if (first.timestamp > currentTime) {
            return "⚠️ 타임스탬프가 미래 시간입니다."
        }

        // 서울 근처 범위 검증 (옵션)
        val isInSeoulArea = first.latitude in 37.0..38.0 && first.longitude in 126.0..128.0
        if (!isInSeoulArea) {
            return "⚠️ 서울 지역이 아닌 위치 데이터입니다."
        }

        return "✅ 위치 데이터 검증 통과 (${locations.size}개)"
    }
}


