package team.swyp.sdu.utils

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import timber.log.Timber

/**
 * 카메라 실행을 위한 데이터 클래스
 */
data class CameraLaunchConfig(
    val cameraLauncher: ActivityResultLauncher<Uri>,
    val permissionLauncher: ActivityResultLauncher<String>,
    val imageUri: Uri?,
    val onImageCaptured: (Uri) -> Unit
)

/**
 * 카메라 권한이 있는지 확인
 */
fun hasCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}

/**
 * 카메라 실행 로직 (권한 체크 포함)
 * 
 * @param context Context
 * @param config CameraLaunchConfig (카메라 런처, 권한 런처, 이미지 URI, 콜백)
 * @return 카메라 실행 성공 여부 (권한이 없으면 false, 권한 요청 시작하면 true)
 */
fun launchCameraWithPermission(
    context: Context,
    config: CameraLaunchConfig
): Boolean {
    return if (hasCameraPermission(context)) {
        // 권한 있음 - 카메라 실행
        try {
            config.imageUri?.let { uri ->
                Timber.d("카메라 실행: $uri")
                config.cameraLauncher.launch(uri)
                true
            } ?: run {
                Timber.e("카메라 이미지 URI가 null입니다")
                false
            }
        } catch (e: SecurityException) {
            Timber.e(e, "카메라 실행 중 SecurityException - 권한 재요청")
            // 권한이 런타임에 취소되었을 수 있음 - 재요청
            config.permissionLauncher.launch(android.Manifest.permission.CAMERA)
            true
        } catch (e: Exception) {
            Timber.e(e, "카메라 실행 중 예외 발생")
            false
        }
    } else {
        // 권한 없음 - 권한 요청
        Timber.d("카메라 권한 없음 - 권한 요청")
        config.permissionLauncher.launch(android.Manifest.permission.CAMERA)
        true
    }
}

/**
 * 카메라 권한 요청 후 승인 시 카메라 실행
 * 
 * @param context Context
 * @param cameraLauncher 카메라 실행 Launcher
 * @param imageUri 카메라 촬영 결과를 저장할 URI (null이면 권한 승인 후 재생성 시도)
 * @param createUri URI 생성 함수 (imageUri가 null일 때 사용)
 * @param isGranted 권한 승인 여부
 */
fun handleCameraPermissionResult(
    context: Context,
    cameraLauncher: ActivityResultLauncher<Uri>,
    imageUri: Uri?,
    createUri: () -> Uri?,
    isGranted: Boolean
) {
    if (isGranted) {
        Timber.d("카메라 권한 승인됨 - 카메라 실행")
        // 권한 승인 후 카메라 실행
        val uri = imageUri ?: run {
            // URI가 null이면 권한 승인 후 재생성 시도
            Timber.d("이미지 URI가 null이므로 권한 승인 후 재생성 시도")
            createUri()
        }
        
        uri?.let { finalUri ->
            try {
                Timber.d("카메라 실행: $finalUri")
                cameraLauncher.launch(finalUri)
            } catch (e: SecurityException) {
                Timber.e(e, "카메라 실행 중 SecurityException 발생")
            } catch (e: Exception) {
                Timber.e(e, "카메라 실행 중 예외 발생")
            }
        } ?: run {
            Timber.e("카메라 권한 승인되었으나 이미지 URI 생성 실패")
        }
    } else {
        Timber.d("카메라 권한 거부됨")
    }
}


