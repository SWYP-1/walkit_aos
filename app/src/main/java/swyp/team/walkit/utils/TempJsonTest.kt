package swyp.team.walkit.utils

import android.content.Context
import kotlinx.serialization.json.Json
import swyp.team.walkit.data.model.LocationPoint

fun loadLocationsFromJson(context: Context,fileName: String = "temp"): List<LocationPoint> {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

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