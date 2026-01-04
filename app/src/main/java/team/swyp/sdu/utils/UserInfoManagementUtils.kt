package team.swyp.sdu.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import timber.log.Timber

/**
 * 프로필 이미지 상태 관리 클래스
 */
data class ProfileImageState(
    val originalImageName: String? = null,     // 서버에서 받아온 기존 이미지 이름
    val selectedImageUri: Uri? = null,         // 사용자가 새로 선택한 이미지 URI
    val displayUrl: String? = null             // 현재 표시중인 이미지 URL
) {
    /**
     * 현재 표시할 이미지 URL 반환
     * 우선순위: displayUrl > selectedImageUri > originalImageName
     */
    val currentDisplayUrl: String?
        get() = displayUrl ?: selectedImageUri?.toString() ?: originalImageName

    /**
     * 변경사항이 있는지 확인
     */
    fun hasChanges(): Boolean {
        return selectedImageUri != null
    }

    companion object {
        fun fromUserInput(imageName: String?, selectedImageUriString: String?): ProfileImageState {
            val selectedImageUri = selectedImageUriString?.let { uriString ->
                try {
                    Uri.parse(uriString)
                } catch (t: Throwable) {
                    Timber.e(t, "Invalid URI string: $uriString")
                    null
                }
            }

            return ProfileImageState(
                originalImageName = imageName,
                selectedImageUri = selectedImageUri,
                displayUrl = selectedImageUri?.toString() ?: imageName // 로컬 선택 이미지를 우선 표시
            )
        }
    }
}

/**
 * UserInfoManagementScreen에서 사용하는 유틸리티 함수들
 */

/**
 * 카메라 촬영용 임시 이미지 URI 생성
 */
fun createCameraImageUri(context: Context): Uri? {
    return try {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "camera_image_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/*")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Camera")
            }
        }
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    } catch (t: Throwable) {
        Timber.e(t, "카메라 이미지 URI 생성 실패")
        null
    }
}

/**
 * 생년월일 문자열을 YYYY-MM-DD 형식으로 포맷팅
 */
fun formatBirthDate(year: String, month: String, day: String): String {
    return try {
        val yearInt = year.toIntOrNull() ?: return ""
        val monthInt = month.toIntOrNull() ?: return ""
        val dayInt = day.toIntOrNull() ?: return ""

        String.format("%04d-%02d-%02d", yearInt, monthInt, dayInt)
    } catch (t: Throwable) {
        Timber.e(t, "생년월일 포맷팅 실패")
        ""
    }
}

/**
 * 생년월일 문자열을 연/월/일로 파싱
 * @return Triple<year, month, day> 또는 null
 */
fun parseBirthDate(birthDate: String): Triple<String, String, String>? {
    return try {
        val parts = birthDate.split("-")
        if (parts.size == 3) {
            Triple(parts[0], parts[1], parts[2])
        } else {
            null
        }
    } catch (t: Throwable) {
        Timber.e(t, "생년월일 파싱 실패")
        null
    }
}

/**
 * 사용자 정보 변경사항 확인
 */
fun hasUserInfoChanges(
    originalName: String,
    currentName: String,
    originalNickname: String,
    currentNickname: String,
    originalBirthDate: String,
    currentBirthYear: String,
    currentBirthMonth: String,
    currentBirthDay: String,
    originalImageUrl: String?,
    currentImageUrl: String?
): Boolean {
    val currentBirthDate = formatBirthDate(currentBirthYear, currentBirthMonth, currentBirthDay)

    return currentName != originalName ||
            currentNickname != originalNickname ||
            currentBirthDate != originalBirthDate ||
            currentImageUrl != originalImageUrl
}
