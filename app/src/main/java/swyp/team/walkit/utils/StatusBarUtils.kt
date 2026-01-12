package swyp.team.walkit.utils

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import swyp.team.walkit.R

/**
 * 상태바 설정 유틸리티
 * 화면별로 상태바 색상과 스타일을 커스터마이징할 수 있습니다.
 */

/**
 * 상태바 설정 옵션
 */
data class StatusBarConfig(
    val statusBarColor: Int? = null, // null이면 투명
    val isLightStatusBar: Boolean = true, // 상태바 아이콘 색상 (true = 검정, false = 흰색)
    val isEdgeToEdge: Boolean = false, // Edge-to-edge 모드 활성화 여부
    val navigationBarColor: Int? = null, // null이면 기본값
    val isLightNavigationBar: Boolean = true, // 네비게이션 바 아이콘 색상
)

/**
 * 기본 상태바 설정 (흰색 배경, 검정 아이콘)
 */
val DefaultStatusBarConfig = StatusBarConfig(
    statusBarColor = android.graphics.Color.WHITE,
    isLightStatusBar = true,
    isEdgeToEdge = false,
    navigationBarColor = android.graphics.Color.WHITE,
    isLightNavigationBar = true,
)

/**
 * 투명 상태바 설정 (투명 배경, 검정 아이콘)
 * Edge-to-edge 모드로 전체 시스템 바가 투명하게 처리됨
 */
val TransparentStatusBarConfig = StatusBarConfig(
    statusBarColor = android.graphics.Color.TRANSPARENT,
    isLightStatusBar = true,
    isEdgeToEdge = true,
    navigationBarColor = android.graphics.Color.TRANSPARENT,
    isLightNavigationBar = true,
)

/**
 * 상태바만 투명하게 설정 (바텀 네비게이션 바는 기본값 유지)
 * 바텀 네비게이션 바가 있는 화면에서 사용
 * Edge-to-edge를 활성화하여 상태바를 투명하게 하되, 네비게이션 바는 명시적으로 흰색으로 설정하여 유지
 */
val TransparentStatusBarOnlyConfig = StatusBarConfig(
    statusBarColor = android.graphics.Color.TRANSPARENT,
    isLightStatusBar = true,
    isEdgeToEdge = true, // Edge-to-edge 활성화하여 상태바 투명 처리
    navigationBarColor = android.graphics.Color.WHITE, // 네비게이션 바는 명시적으로 흰색으로 설정하여 유지
    isLightNavigationBar = true,
)

/**
 * 다크 상태바 설정 (검정 배경, 흰색 아이콘)
 */
val DarkStatusBarConfig = StatusBarConfig(
    statusBarColor = android.graphics.Color.BLACK,
    isLightStatusBar = false,
    isEdgeToEdge = false,
    navigationBarColor = android.graphics.Color.BLACK,
    isLightNavigationBar = false,
)

/**
 * Window에 상태바 설정 적용
 */
fun Window.applyStatusBarConfig(config: StatusBarConfig) {
    // Edge-to-edge 설정
    // isEdgeToEdge가 true면 상태바 영역을 콘텐츠가 사용할 수 있도록 함
    WindowCompat.setDecorFitsSystemWindows(this, !config.isEdgeToEdge)

    // 상태바 색상 설정 (항상 명시적으로 설정)
    statusBarColor = config.statusBarColor ?: android.graphics.Color.TRANSPARENT

    // 네비게이션 바 색상 설정
    // config.navigationBarColor가 null이면 변경하지 않음 (기존 값 유지)
    // 명시적으로 설정된 경우에만 변경
    if (config.navigationBarColor != null) {
        navigationBarColor = config.navigationBarColor
    }
    // null이면 네비게이션 바 색상은 변경하지 않음 (MainActivity의 기본값 유지)

    // Android Q 이상에서 contrast 강제 비활성화 (투명 상태바를 위해)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        isStatusBarContrastEnforced = false
        // 네비게이션 바는 명시적으로 설정된 경우에만 contrast 비활성화
        if (config.navigationBarColor != null) {
            isNavigationBarContrastEnforced = false
        }
    }

    // WindowInsetsController로 아이콘 색상 설정
    WindowInsetsControllerCompat(this, decorView).apply {
        isAppearanceLightStatusBars = config.isLightStatusBar
        // 네비게이션 바 아이콘 색상은 명시적으로 설정된 경우에만 변경
        if (config.navigationBarColor != null) {
            isAppearanceLightNavigationBars = config.isLightNavigationBar
        }
    }
}

/**
 * Compose에서 상태바 설정을 적용하는 Composable
 * 
 * 사용 예시:
 * ```
 * SetStatusBarConfig(config = TransparentStatusBarConfig)
 * ```
 */
@Composable
fun SetStatusBarConfig(
    config: StatusBarConfig,
    onDispose: StatusBarConfig = DefaultStatusBarConfig
) {
    val view = LocalView.current

    SideEffect {
        val window = (view.context as? Activity)?.window
        window?.applyStatusBarConfig(config)
    }

    DisposableEffect(Unit) {
        onDispose {
            val window = (view.context as? Activity)?.window
            window?.applyStatusBarConfig(onDispose)
        }
    }
}
