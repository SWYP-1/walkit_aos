package swyp.team.walkit.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import swyp.team.walkit.data.model.LocationPoint
import kotlin.math.max
import kotlin.math.min

/**
 * 경로 썸네일 이미지 컴포넌트
 *
 * 정적 이미지로 경로를 표시합니다.
 * Canvas를 사용하여 경로를 그립니다.
 */
@Composable
fun RouteThumbnail(
    locations: List<LocationPoint>,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 200.dp,
) {
    val thumbnailBitmap =
        remember(locations) {
            generateCenterhumbnail(locations)
        }

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            thumbnailBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "경로 썸네일",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

/**
 * 경로 썸네일 비트맵 생성
 */
private fun generateRouteThumbnail(locations: List<LocationPoint>): Bitmap? {
    if (locations.size < 2) return null

    // 썸네일 크기
    val width = 800
    val height = 400
    val padding = 40f

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // 배경 그리기
    canvas.drawColor(Color.parseColor("#F5F5F5"))

    // 경계 계산
    var minLat = locations[0].latitude
    var maxLat = locations[0].latitude
    var minLon = locations[0].longitude
    var maxLon = locations[0].longitude

    locations.forEach { location ->
        minLat = min(minLat, location.latitude)
        maxLat = max(maxLat, location.latitude)
        minLon = min(minLon, location.longitude)
        maxLon = max(maxLon, location.longitude)
    }

    val latRange = maxLat - minLat
    val lonRange = maxLon - minLon

    if (latRange == 0.0 || lonRange == 0.0) return null

    // 좌표를 화면 좌표로 변환하는 함수
    fun latLonToXY(
        lat: Double,
        lon: Double,
    ): Pair<Float, Float> {
        val x = padding + ((lon - minLon) / lonRange) * (width - padding * 2)
        val y = height - padding - ((lat - minLat) / latRange) * (height - padding * 2)
        return Pair(x.toFloat(), y.toFloat())
    }

    // 경로 그리기
    val path = Path()
    val firstPoint = latLonToXY(locations[0].latitude, locations[0].longitude)
    path.moveTo(firstPoint.first, firstPoint.second)

    locations.drop(1).forEach { location ->
        val point = latLonToXY(location.latitude, location.longitude)
        path.lineTo(point.first, point.second)
    }

    // 경로 선 그리기
    val pathPaint =
        Paint().apply {
            color = Color.parseColor("#4285F4")
            style = Paint.Style.STROKE
            strokeWidth = 8f
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    canvas.drawPath(path, pathPaint)

    // 시작점 마커 (초록색 원)
    val startPoint = latLonToXY(locations.first().latitude, locations.first().longitude)
    val startMarkerPaint =
        Paint().apply {
            color = Color.parseColor("#FFFFFF")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
    canvas.drawCircle(startPoint.first, startPoint.second, 12f, startMarkerPaint)

    // 끝점 마커 (빨간색 원) - 시작점과 다른 경우에만
    if (locations.size > 1) {
        val lastLocation = locations.last()
        if (lastLocation.latitude != locations.first().latitude ||
            lastLocation.longitude != locations.first().longitude
        ) {
            val endPoint = latLonToXY(lastLocation.latitude, lastLocation.longitude)
            val endMarkerPaint =
                Paint().apply {
                    color = Color.parseColor("#FFFFFF")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
            canvas.drawCircle(endPoint.first, endPoint.second, 12f, endMarkerPaint)
        }
    }

    return bitmap
}
private fun generateCenterhumbnail(locations: List<LocationPoint>): Bitmap? {
    if (locations.size < 2) return null

    val width = 800
    val height = 400
    val padding = 40f

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    canvas.drawColor(Color.parseColor("#F5F5F5"))

    // 경계 계산
    var minLat = locations[0].latitude
    var maxLat = locations[0].latitude
    var minLon = locations[0].longitude
    var maxLon = locations[0].longitude

    locations.forEach { location ->
        minLat = min(minLat, location.latitude)
        maxLat = max(maxLat, location.latitude)
        minLon = min(minLon, location.longitude)
        maxLon = max(maxLon, location.longitude)
    }

    val latRange = maxLat - minLat
    val lonRange = maxLon - minLon
    if (latRange == 0.0 || lonRange == 0.0) return null

    // 스케일 계산
    val pathWidth = width - padding * 2
    val pathHeight = height - padding * 2

    val lonScale = pathWidth / lonRange
    val latScale = pathHeight / latRange

    // 중앙 오프셋
    val xOffset = (width - (lonRange * lonScale)) / 2
    val yOffset = (height - (latRange * latScale)) / 2

    // 좌표 변환 함수 (중앙 정렬)
    fun latLonToXY(lat: Double, lon: Double): Pair<Float, Float> {
        val x = xOffset + ((lon - minLon) * lonScale)
        val y = height - yOffset - ((lat - minLat) * latScale)
        return Pair(x.toFloat(), y.toFloat())
    }

    // 경로 그리기
    val path = Path()
    val firstPoint = latLonToXY(locations[0].latitude, locations[0].longitude)
    path.moveTo(firstPoint.first, firstPoint.second)

    locations.drop(1).forEach { location ->
        val point = latLonToXY(location.latitude, location.longitude)
        path.lineTo(point.first, point.second)
    }

    val pathPaint = Paint().apply {
        color = Color.parseColor("#FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    canvas.drawPath(path, pathPaint)

    // 시작점 마커
    val startPoint = latLonToXY(locations.first().latitude, locations.first().longitude)
    canvas.drawCircle(startPoint.first, startPoint.second, 12f, Paint().apply {
        color = Color.parseColor("#FFFFFF")
        style = Paint.Style.FILL
        isAntiAlias = true
    })

    // 끝점 마커
    val lastLocation = locations.last()
    if (lastLocation.latitude != locations.first().latitude ||
        lastLocation.longitude != locations.first().longitude
    ) {
        val endPoint = latLonToXY(lastLocation.latitude, lastLocation.longitude)
        canvas.drawCircle(endPoint.first, endPoint.second, 12f, Paint().apply {
            color = Color.parseColor("#F44336")
            style = Paint.Style.FILL
            isAntiAlias = true
        })
    }

    return bitmap
}

