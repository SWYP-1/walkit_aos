# Core Rules for Modern Android API Compliance (2025)

이 문서는 Android 15(API 35)를 포함한 최신 안드로이드 버전 대응을 위한 필수 개발 규칙입니다.

## 1. Target SDK Requirements
- **New apps**: Android 15 (API level 35) 필수 (2025년 8월 31일 기준).
- **Existing apps**: 최소 Android 14 (API level 34).
- Android 15는 API 24 미만 타겟 앱 설치를 차단합니다 (`INSTALL_FAILED_DEPRECATED_SDK_VERSION`).

## 2. Android 15 (API 35) - 필수 변경 사항

### 🚨 Edge-to-Edge Enforcement (CRITICAL)
- 모든 앱은 기본적으로 Edge-to-Edge로 렌더링됩니다.
- **Views**: `WindowCompat.setDecorFitsSystemWindows(window, false)` 사용 및 Inset 직접 처리.
- **Compose**: `Scaffold`를 사용하여 시스템 바 패딩 자동 처리. 커스텀 컴포저블은 `WindowInsets.safeDrawing` 사용.
- `setNavigationBarColor()`, `setStatusBarColor()`는 **DEPRECATED** 되었습니다.

### 🔒 Foreground Service 변경
- **dataSync & mediaProcessing**: 24시간 내 누적 6시간 타임아웃 제한. `onTimeout`에서 반드시 `stopSelf()` 호출 필수.
- **BOOT_COMPLETED 제한**: `dataSync`, `camera`, `mediaPlayback` 등은 부팅 완료 리시버에서 직접 시작 불가. `WorkManager` 사용 권장.

### 📵 Do Not Disturb (DND) 변경
- 글로벌 DND 상태 직접 변경 불가. `AutomaticZenRule`을 기여하는 방식으로 변경됨.

### 🔐 보안 변경
- `PendingIntent`는 기본적으로 백그라운드 활동 시작을 차단합니다. 명시적 허용(`MODE_BACKGROUND_ACTIVITY_START_ALLOWED`)이 필요할 때만 설정하십시오.

## 3. Android 14 (API 34) - 주요 변경 사항
- **FGS Type Mandatory**: 매니페스트에 `foregroundServiceType` 선언 필수. 시작 시에도 매칭되는 타입 지정 필수.
- **Selected Photos Access**: `READ_MEDIA_VISUAL_USER_SELECTED` 권한을 통한 부분 미디어 접근 지원.
- **Context-registered Receiver**: `registerReceiver` 시 `RECEIVER_EXPORTED` 또는 `RECEIVER_NOT_EXPORTED` 플래그 명시 필수.

## 4. Android 12/13 - 주요 변경 사항
- **PendingIntent Mutability**: `FLAG_IMMUTABLE` 또는 `FLAG_MUTABLE` 명시 필수.
- **Exact Alarms**: `SCHEDULE_EXACT_ALARM` 권한 확인 및 `canScheduleExactAlarms()` 체크 필수.
- **Notification Permission**: `POST_NOTIFICATIONS` 런타임 권한 요청 필수.

## 5. General Best Practices
- `build.gradle.kts`에 명확한 API 레벨 명시.
- `Build.VERSION.SDK_INT`를 사용한 버전 분기 처리.
- `FileProvider`를 통한 `content://` URI 사용 (raw `file://` 사용 금지).
