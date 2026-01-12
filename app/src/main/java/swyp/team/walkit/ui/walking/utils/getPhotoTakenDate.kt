package swyp.team.walkit.ui.walking.utils

import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.*

/**
 * 사진의 촬영 시간을 안전하게 가져옵니다.
 * 1. Exif TAG_DATETIME_ORIGINAL
 * 2. Exif TAG_DATETIME
 * 3. MediaStore.Images.Media.DATE_TAKEN (fallback)
 * 4. 파일 수정 시간 (최후의 수단)
 */
fun getPhotoTakenDate(context: Context, photoUri: Uri): Date? {
    // 1. Exif 확인
    try {
        context.contentResolver.openInputStream(photoUri)?.use { inputStream ->
            val exif = ExifInterface(inputStream)
            val dateStr =
                exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)

            if (!dateStr.isNullOrEmpty()) {
                // Exif 날짜 형식: "yyyy:MM:dd HH:mm:ss" (예: "2024:01:09 17:30:00")
                val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
                try {
                    val parsedDate = sdf.parse(dateStr)
                    if (parsedDate != null) {
                        return parsedDate
                    }
                } catch (e: Exception) {
                    // 날짜 형식 파싱 실패 - 다음 방법 시도
                    android.util.Log.w("getPhotoTakenDate", "Exif 날짜 파싱 실패: $dateStr", e)
                }
            }
        }
    } catch (t: Throwable) {
        android.util.Log.w("getPhotoTakenDate", "Exif 읽기 실패", t)
    }

    // 2. MediaStore DATE_TAKEN fallback
    try {
        val projection = arrayOf(MediaStore.Images.Media.DATE_TAKEN)
        context.contentResolver.query(photoUri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val dateTaken = cursor.getLong(columnIndex)
                if (dateTaken > 0) {
                    return Date(dateTaken)
                }
            }
        }
    } catch (t: Throwable) {
        android.util.Log.w("getPhotoTakenDate", "MediaStore 조회 실패", t)
    }

    // 3. 파일 수정 시간 (최후의 수단 - 신뢰도 낮음)
    try {
        val file = java.io.File(photoUri.path ?: return null)
        if (file.exists()) {
            val lastModified = file.lastModified()
            if (lastModified > 0) {
                return Date(lastModified)
            }
        }
    } catch (t: Throwable) {
        android.util.Log.w("getPhotoTakenDate", "파일 수정 시간 조회 실패", t)
    }

    // 4. 모두 실패하면 null 반환
    return null
}

/**
 * 업로드 가능 여부 확인
 * @param cutoffTime 제한 시간 이전 사진은 업로드 불가
 * @return true: 업로드 가능, false: 업로드 불가 (시간 정보 없음 또는 cutoffTime 이전)
 */
fun canUploadPhoto(context: Context, photoUri: Uri, cutoffTime: Date): Boolean {
    val photoDate = getPhotoTakenDate(context, photoUri)
    
    // 시간 정보를 가져올 수 없는 경우
    if (photoDate == null) {
        // ⚠️ 예외 케이스: Exif 정보가 없는 경우
        // 실제로는 산책 중 촬영한 사진일 수도 있지만, 검증 불가능하므로 false 반환
        // 사용자에게 스낵바로 알림 표시됨
        return false
    }
    
    // 촬영 시간이 산책 시작 시간 이후인지 확인
    // after()는 엄격한 비교이므로, 정확히 같은 시간이면 false
    // 산책 시작 직후 촬영한 사진도 허용하기 위해 >= 비교 사용
    return photoDate.time >= cutoffTime.time
}
