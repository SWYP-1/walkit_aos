package team.swyp.sdu.ui.walking

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Compose View를 캡처하는 유틸리티 함수
 * 
 * PixelCopy와 다른 방법으로, View.draw()를 사용하여 Canvas에 그리는 방식입니다.
 * 
 * 장점:
 * - Android 버전 제한 없음 (PixelCopy는 8.0+)
 * - 더 간단한 구현
 * - Compose View에서도 사용 가능
 * 
 * 단점:
 * - 하드웨어 가속된 View (GLSurfaceView 등)는 제대로 캡처되지 않을 수 있음
 * - 성능이 PixelCopy보다 느릴 수 있음
 */

/**
 * View를 Bitmap으로 변환 (View.draw() 방식)
 * 
 * @param view 캡처할 View
 * @return Bitmap (실패 시 null)
 */
fun View.toBitmap(): Bitmap? {
    return try {
        if (width == 0 || height == 0) {
            Timber.w("View 크기가 0입니다: ${width}x${height}")
            return null
        }
        
        val bitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(bitmap)
        draw(canvas)
        
        Timber.d("View.draw() 스냅샷 생성 완료: ${bitmap.width}x${bitmap.height}")
        bitmap
    } catch (e: Exception) {
        Timber.e(e, "View.draw() 스냅샷 생성 실패: ${e.message}")
        null
    }
}

/**
 * View를 파일로 저장
 * 
 * @param view 캡처할 View
 * @param context Context
 * @param fileName 파일명 (선택사항, 없으면 타임스탬프 사용)
 * @return 저장된 파일 경로 (실패 시 null)
 */
fun View.saveToFile(
    context: Context,
    fileName: String? = null
): String? {
    val bitmap = toBitmap() ?: return null
    
    return try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val finalFileName = fileName ?: "snapshot_${timestamp}.png"
        
        val fileDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val file = File(fileDir, finalFileName)
        
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        
        val absolutePath = file.absolutePath
        Timber.d("스냅샷 파일 저장 완료: $absolutePath")
        absolutePath
    } catch (e: Exception) {
        Timber.e(e, "스냅샷 파일 저장 실패: ${e.message}")
        null
    } finally {
        bitmap.recycle()
    }
}

/**
 * ViewGroup에서 특정 타입의 View 찾기
 */
fun <T : View> ViewGroup.findViewByType(type: Class<T>): T? {
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        if (type.isInstance(child)) {
            return type.cast(child)
        }
        if (child is ViewGroup) {
            val found = child.findViewByType(type)
            if (found != null) return found
        }
    }
    return null
}




