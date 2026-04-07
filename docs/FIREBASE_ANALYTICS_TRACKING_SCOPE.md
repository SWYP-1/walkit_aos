# Firebase Analytics 추적 범위 정의

## 1. 원칙

### 1.1 "적당히" 추적하기

- **너무 많으면**: 노이즈 증가, 이벤트 비용, 분석 복잡도 상승
- **너무 적으면**: 핵심 퍼널·행동 파악 불가
- **산책 앱 특성**: 산책 관련 액션이 가장 중요 → 우선순위 부여

### 1.2 추적 우선순위

| 우선순위 | 설명 | 예시 |
|----------|------|------|
| **P0** | 반드시 추적 | 산책 시작/종료, 로그인 |
| **P1** | 추적 권장 | 산책 플로우 내 주요 버튼 |
| **P2** | 선택 추적 | 부가 기능 진입점 |

---

## 2. 추적 대상 요약

### 2.1 전체 이벤트 수 제한

- **screen_view**: 약 25개 화면 (기존 문서 기준)
- **커스텀 이벤트**: **10개 이내** 권장 (초기 단계)

### 2.2 커스텀 이벤트 목록 (우선순위별)

| 우선순위 | 이벤트명 | 목적 | 구현 위치 |
|----------|----------|------|-----------|
| **P0** | `walking_started` | 산책 시작 (FAB 클릭) | MainScreen FAB / WalkingViewModel |
| **P0** | `walking_finished` | 산책 종료 (세션 저장 완료) | WalkingViewModel |
| **P0** | `login_completed` | 로그인 성공 | LoginViewModel |
| **P1** | `walking_paused` | 산책 일시정지 | WalkingViewModel |
| **P1** | `walking_resumed` | 산책 재개 | WalkingViewModel |
| **P1** | `emotion_saved` | 감정 기록 저장 | WalkingViewModel (PostEmotion → EmotionRecord) |
| **P1** | `onboarding_completed` | 온보딩 완료 | OnboardingScreen |
| **P2** | `mission_challenge_clicked` | 미션 도전 클릭 | MissionCard / HomeRoute |
| **P2** | `alarm_entered` | 알람 화면 진입 | Alarm route (screen_view로 대체 가능) |
| **P2** | `friend_follow_clicked` | 친구 팔로우/언팔로우 | FriendCard |

---

## 3. 산책 플로우별 추적 포인트

### 3.1 산책 시작 (P0)

```
[홈 FAB 클릭] → walking_started
```

- **위치**: `MainScreen` FAB `onClick` 또는 `WalkingViewModel.startWalking()` 직후
- **파라미터**: `source` = "fab" (추후 확장: "mission", "notification" 등)

### 3.2 산책 진행 중 (P1)

```
[일시정지] → walking_paused
[다시 시작] → walking_resumed
[산책 끝내기] → (finishWalking → stopWalking 완료 후) walking_finished
```

- **위치**: `WalkingViewModel.pauseWalking()`, `resumeWalking()`, `finishWalking()` → `stopWalking()` 완료 시점
- **walking_finished 파라미터**: `duration_sec`, `step_count` (선택)

### 3.3 산책 후 감정 기록 (P1)

```
[감정 선택 → 다음] → (EmotionRecord 저장 API 성공 후) emotion_saved
```

- **위치**: `WalkingViewModel` 또는 감정 저장 API 성공 콜백
- **파라미터**: `emotion_type` (선택)

### 3.4 로그인 (P0)

```
[카카오/네이버 로그인 성공] → login_completed
```

- **위치**: `LoginViewModel` 로그인 성공 시점
- **파라미터**: `method` = "kakao" | "naver"

### 3.5 온보딩 (P1)

```
[온보딩 마지막 단계 완료] → onboarding_completed
```

- **위치**: `OnboardingScreen` 완료 버튼 클릭 후

### 3.6 미션 (P2)

```
[미션 카드 "도전하기" 클릭] → mission_challenge_clicked
```

- **위치**: `MissionCard` `onChallengeClick` 또는 `MissionSection` → `MissionContent`

### 3.7 친구 (P2)

```
[팔로우/언팔로우 버튼 클릭] → friend_follow_clicked
```

- **위치**: `FriendCard` `FollowButton` `onClick`
- **파라미터**: `action` = "follow" | "unfollow"

---

## 4. 추적하지 않는 것 (명시)

| 구분 | 비추적 사유 |
|------|-------------|
| 탭 전환 | `screen_view` (Main_Home 등)로 이미 반영 |
| 뒤로가기 | 화면별 이탈은 screen_view 시퀀스로 추론 가능 |
| 설정 변경 (목표 걸음 등) | 초기에는 과도한 상세 추적 지양 |
| 캐릭터샵 아이템 클릭 | P2 이하, 초기 제외 |
| 달력/기록 상세 진입 | `screen_view` (DailyRecord)로 충분 |
| 다이얼로그 확인/취소 | 퍼널 분석에 직접 기여도 낮음 |

---

## 5. 구현 순서 제안

| 단계 | 작업 | 비고 |
|------|------|------|
| 1 | `screen_view` (기존 문서대로) | NavGraph + MainScreen 탭 |
| 2 | `walking_started`, `walking_finished` | P0 산책 핵심 |
| 3 | `login_completed` | P0 로그인 |
| 4 | `walking_paused`, `walking_resumed`, `emotion_saved` | P1 산책 플로우 |
| 5 | `onboarding_completed` | P1 온보딩 |
| 6 | `mission_challenge_clicked`, `friend_follow_clicked` | P2 선택 |

---

## 6. AnalyticsTracker 인터페이스 확장 (참고)

```kotlin
interface AnalyticsTracker {
    fun logScreenView(screen: AnalyticsScreen)

    // P0
    fun logWalkingStarted(source: String = "fab")
    fun logWalkingFinished(durationSec: Long? = null, stepCount: Int? = null)
    fun logLoginCompleted(method: String)

    // P1
    fun logWalkingPaused()
    fun logWalkingResumed()
    fun logEmotionSaved(emotionType: String? = null)
    fun logOnboardingCompleted()

    // P2
    fun logMissionChallengeClicked(missionId: String? = null)
    fun logFriendFollowClicked(action: String)  // "follow" | "unfollow"
}
```

---

## 7. 요약

- **screen_view**: 화면 진입 (기존 문서)
- **커스텀 이벤트**: 산책 관련 6개 + 로그인/온보딩 2개 + 선택 2개 = **최대 10개**
- **우선순위**: 산책 시작/종료 > 로그인 > 산책 중/후 액션 > 온보딩 > 미션/친구
