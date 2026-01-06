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
                val sdf = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
                return sdf.parse(dateStr)
            }
        }
    } catch (t: Throwable) {
        t.printStackTrace()
    }

    // 2. MediaStore DATE_TAKEN fallback
    val projection = arrayOf(MediaStore.Images.Media.DATE_TAKEN)
    context.contentResolver.query(photoUri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val dateTaken = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN))
            return Date(dateTaken)
        }
    }

    // 3. 모두 실패하면 null 반환
    return null
}

/**
 * 업로드 가능 여부 확인
 * @param cutoffTime 제한 시간 이전 사진은 업로드 불가
 */
fun canUploadPhoto(context: Context, photoUri: Uri, cutoffTime: Date): Boolean {
    val photoDate = getPhotoTakenDate(context, photoUri)
    return photoDate?.after(cutoffTime) ?: false
}
