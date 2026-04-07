# Firebase Analytics 화면 추적 설계 문서 (개선 버전)

## 1. 개요

### 1.1 목적

사용자가 기본적으로 **어떤 화면에 진입하는지** 파악하여 다음을 지원합니다.

- **사용자 행동 분석**: 자주 방문하는 화면, 이탈률이 높은 화면 파악
- **퍼널 분석**: 온보딩/로그인 → 메인 → 산책 플로우 전환율 측정
- **탭별 사용 패턴**: 홈/기록/캐릭터/마이페이지 탭 이용 현황
- **기능별 활용도**: 기능별 화면 진입 비율 측정

### 1.2 Firebase Analytics 기본 이벤트

Firebase Analytics는 `screen_view` 이벤트를 사용하여 화면 진입을 기록합니다.

| 이벤트명 | 필수 파라미터 | 설명 |
|----------|---------------|------|
| `screen_view` | `screen_name` | 화면 진입 시 수동 기록 |

> **GA4 기준**: `screen_class`는 실질적 분석 가치가 낮아 제거하고 `screen_name`만 관리합니다.

---

## 2. 자동 Screen Reporting 비활성화 (필수)

Compose + Single Activity 구조에서는 Firebase의 **자동 screen_view 수집**과 충돌할 수 있으므로 **반드시 비활성화**합니다.

- **목적**: 중복 `screen_view` 발생 방지
- **적용 위치**: `AndroidManifest.xml` (application 태그 내부)

```xml
<application ...>
    <!-- 기존 설정 -->

    <!-- Firebase 자동 화면 추적 비활성화: 수동 screen_view만 사용 -->
    <meta-data
        android:name="firebase_analytics_automatic_screen_reporting_enabled"
        android:value="false" />
</application>
```

---

## 3. Analytics Screen 타입 정의 (enum 기반)

문자열 기반 screen name 분산을 방지하기 위해 **enum 기반**으로 통합 관리합니다.

```kotlin
enum class AnalyticsScreen(val screenName: String) {
    Splash("Splash"),
    Login("Login"),
    Onboarding("Onboarding"),

    MainHome("Main_Home"),
    MainRecord("Main_Record"),
    MainCharacter("Main_Character"),
    MainMyPage("Main_MyPage"),

    Walking("Walking"),
    PostEmotionSelection("PostEmotionSelection"),
    EmotionRecord("EmotionRecord"),
    WalkingResult("WalkingResult"),

    RouteDetail("RouteDetail"),
    Friends("Friends"),
    FriendSearch("FriendSearch"),
    FriendSearchDetail("FriendSearchDetail"),

    GoalManagement("GoalManagement"),
    Mission("Mission"),
    DressingRoom("DressingRoom"),
    CharacterShop("CharacterShop"),
    UserInfoManagement("UserInfoManagement"),
    NotificationSettings("NotificationSettings"),
    Alarm("Alarm"),
    DailyRecord("DailyRecord"),

    CustomTest("CustomTest")  // 개발 전용, 프로덕션 차단
}
```

---

## 4. AnalyticsTracker 인터페이스

```kotlin
interface AnalyticsTracker {
    fun logScreenView(screen: AnalyticsScreen)
}
```

- 구현체: `FirebaseAnalyticsTracker`
- 테스트: `FakeAnalyticsTracker`로 Mock 가능

---

## 5. FirebaseAnalyticsTracker 구현체

```kotlin
class FirebaseAnalyticsTracker(
    private val firebaseAnalytics: FirebaseAnalytics
) : AnalyticsTracker {

    override fun logScreenView(screen: AnalyticsScreen) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screen.screenName)
        }
    }
}
```

> **CustomTest 프로덕션 차단**: 매핑 단계(`routeToAnalyticsScreen`)에서 처리하는 것을 권장합니다. `FirebaseAnalyticsTracker`, `MixpanelTracker` 등 여러 구현체가 있어도 **한 곳에서만 차단 로직**을 넣으면 됩니다.

---

## 6. NavGraph 중앙 집중식 추적 (중복 방지 포함)

### 6.1 핵심 보완점

1. **route pattern 기반 매핑**: Nav destination의 `route` 패턴 기준으로 매핑 (문자열 `startsWith` 제거)
2. **이전 route 저장하여 중복 방지**: 동일 route 재진입 시 `screen_view` 중복 로깅 방지
3. **previousRoute 스코프**: `remember`는 컴포저블 재구성 시 초기화될 수 있으므로, **ViewModel 또는 상위 State holder**에서 보관

### 6.2 구현 예시

```kotlin
// NavigationAnalyticsViewModel: recomposition에도 유지되는 previousRoute
@HiltViewModel
class NavigationAnalyticsViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {
    var previousRoute: String? = null
        private set

    fun onRouteChanged(routePattern: String) {
        if (previousRoute == routePattern) return
        previousRoute = routePattern

        routeToAnalyticsScreen(routePattern)?.let { screen ->
            analyticsTracker.logScreenView(screen)
        }
    }
}

@Composable
fun NavGraphWithAnalytics(
    navController: NavHostController,
    viewModel: NavigationAnalyticsViewModel = hiltViewModel()
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val routePattern = currentBackStackEntry?.destination?.route

    LaunchedEffect(routePattern) {
        routePattern?.let { viewModel.onRouteChanged(it) }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // graph 구성
    }
}
```

**대안 (ViewModel 미사용 시)**: NavGraph를 호출하는 **Activity/최상위 Composable**에서 `rememberSaveable` + `mutableStateOf`로 previousRoute를 보관하고, NavGraph에 콜백으로 전달. 다만 Activity 생명주기와 분리되려면 ViewModel 권장.

### 6.3 analyticsTracker 주입 및 호출부

**Hilt를 사용하는 경우** – Activity/App 최상위에서 주입 예시:

```kotlin
// MainActivity.kt
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var analyticsTracker: AnalyticsTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppNavHost(
                analyticsTracker = analyticsTracker,
                ...
            )
        }
    }
}

// 또는 CompositionLocal 패턴
val LocalAnalyticsTracker = staticCompositionLocalOf<AnalyticsTracker> {
    error("AnalyticsTracker not provided")
}

@Composable
fun WalkItApp() {
    val analyticsTracker: AnalyticsTracker = hiltViewModel<...>().analyticsTracker
    // 또는 @Inject한 인스턴스
    CompositionLocalProvider(LocalAnalyticsTracker provides analyticsTracker) {
        NavGraph(...)
    }
}
```

**권장**: `NavigationAnalyticsViewModel`에 `analyticsTracker`를 주입하고, route 변경 시 `onRouteChanged`만 호출하면, ViewModel이 previousRoute와 로깅을 모두 담당합니다.

### 6.4 백스택 복귀(Back) 시 동작

`B → C → Back` 으로 C에서 B로 돌아올 때:

- `routePattern`이 C → B로 변경됨
- `previousRoute != routePattern` 이므로 **B의 `screen_view`가 다시 기록됨**

이는 **의도된 동작**입니다. 사용자가 B 화면을 다시 보는 순간을 반영하여, 퍼널 분석(재방문률, 이탈 후 복귀 등)에 활용할 수 있습니다.  
반대로 "Back 시 재기록하지 않음"을 원하면, `previousRoute` 대신 backstack depth나 다른 조건으로 분기할 수 있습니다.

---

## 7. route → AnalyticsScreen 매핑 (패턴 기반)

Nav destination의 **route pattern** 기준으로 매핑합니다.  
파라미터가 있는 route(`route_detail/{locationsJson}` 등)는 destination.route가 **패턴 문자열**을 반환하므로 정확 매칭이 가능합니다.

```kotlin
fun routeToAnalyticsScreen(routePattern: String): AnalyticsScreen? {
    return when (routePattern) {
        Screen.Splash.route -> AnalyticsScreen.Splash
        Screen.Login.route -> AnalyticsScreen.Login
        Screen.Onboarding.route -> AnalyticsScreen.Onboarding

        Screen.Walking.route -> AnalyticsScreen.Walking
        Screen.PostEmotionSelectionStep.route -> AnalyticsScreen.PostEmotionSelection
        Screen.EmotionRecord.route -> AnalyticsScreen.EmotionRecord
        Screen.WalkingResult.route -> AnalyticsScreen.WalkingResult

        Screen.RouteDetail.route -> AnalyticsScreen.RouteDetail
        Screen.Friends.route -> AnalyticsScreen.Friends
        Screen.FriendSearch.route -> AnalyticsScreen.FriendSearch
        Screen.FriendSearchDetail.route -> AnalyticsScreen.FriendSearchDetail

        Screen.GoalManagement.route -> AnalyticsScreen.GoalManagement
        Screen.Mission.route -> AnalyticsScreen.Mission
        Screen.DressingRoom.route -> AnalyticsScreen.DressingRoom
        Screen.CharacterShop.route -> AnalyticsScreen.CharacterShop
        Screen.UserInfoManagement.route -> AnalyticsScreen.UserInfoManagement
        Screen.NotificationSettings.route -> AnalyticsScreen.NotificationSettings
        Screen.Alarm.route -> AnalyticsScreen.Alarm
        Screen.DailyRecord.route -> AnalyticsScreen.DailyRecord

        Screen.CustomTest.route -> if (BuildConfig.DEBUG) AnalyticsScreen.CustomTest else null

        Screen.Main.route -> null  // Main은 탭에서 처리 (아래 참고)
        else -> null
    }
}
```

---

## 8. MainScreen 탭 추적 전략

### 8.1 전략 변경

- **Main 자체는 `screen_view`로 기록하지 않음**
- 실제 사용자 화면은 **탭**이므로 탭을 `screen_view` 기준으로 사용
- **퍼널 분석 시 Main 중복 노이즈 제거**

### 8.2 중복 방지 (탭 재클릭)

NavGraph와 동일하게, **동일 탭 재클릭 시** `screen_view` 중복 로깅을 방지합니다.

```kotlin
// MainScreen 내부
val previousTabIndex = remember { mutableStateOf<Int?>(null) }

LaunchedEffect(currentTabIndex) {
    if (previousTabIndex.value != currentTabIndex) {
        previousTabIndex.value = currentTabIndex

        val screen = when (currentTabIndex) {
            0 -> AnalyticsScreen.MainHome
            1 -> AnalyticsScreen.MainRecord
            2 -> AnalyticsScreen.MainCharacter
            3 -> AnalyticsScreen.MainMyPage
            else -> return@LaunchedEffect
        }

        analyticsTracker.logScreenView(screen)
    }
}
```

> Main route 진입 시 NavGraph에서는 `null`을 반환하여 로깅하지 않고, MainScreen에서 탭별로만 `screen_view`를 기록합니다.

---

## 9. 퍼널 분석을 위한 추가 권장 이벤트

`screen_view`만으로는 퍼널 분석이 부족하므로, 아래 이벤트를 추가로 정의하는 것을 권장합니다.  
별도 문서(`FIREBASE_ANALYTICS_FUNNEL_EVENTS.md`)로 분리하거나, 파라미터 명세를 확장할 수 있습니다.

| 이벤트명 | 목적 | 파라미터 (권장) | 구현 위치 |
|----------|------|-----------------|-----------|
| `walking_started` | 산책 시작 | `duration_sec` (예정) | WalkingViewModel, 산책 시작 시점 |
| `emotion_saved` | 산책 후 감정 기록 저장 | `emotion_type` (선택) | WalkingViewModel, 감정 저장 API 성공 후 |
| `login_completed` | 로그인 완료 | `method` (kakao/naver) | LoginViewModel, 로그인 성공 시점 |
| `onboarding_completed` | 온보딩 완료 | (없음) | OnboardingScreen, 완료 버튼 클릭 후 |

---

## 10. 아키텍처 및 DI

### 10.1 의존성 구조

```
NavGraph (or App)
    → AnalyticsTracker (interface)
        → FirebaseAnalyticsTracker (구현체, FirebaseAnalytics 주입)
```

### 10.2 DI 모듈

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {
    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics =
        FirebaseAnalytics.getInstance(context)

    @Provides
    @Singleton
    fun provideAnalyticsTracker(firebaseAnalytics: FirebaseAnalytics): AnalyticsTracker =
        FirebaseAnalyticsTracker(firebaseAnalytics)
}
```

---

## 11. 구현 우선순위

| 단계 | 작업 | 비고 |
|------|------|------|
| 1 | AndroidManifest에 `firebase_analytics_automatic_screen_reporting_enabled=false` 추가 | 필수 선행 |
| 2 | `AnalyticsScreen` enum 및 `AnalyticsTracker` 인터페이스 정의 | 타입 안전 구조 |
| 3 | `FirebaseAnalyticsTracker` 구현 (CustomTest 프로덕션 차단 포함) | DI 모듈 포함 |
| 4 | NavGraph에서 route 변경 감지 + 중복 방지 + `logScreenView` 호출 | 핵심 |
| 5 | MainScreen에서 탭 변경 시 `logScreenView` 호출 | 탭 단일 기준 |

---

## 12. Firebase Console 확인 방법

### 12.1 이벤트 로그

1. Firebase Console → 프로젝트 → Analytics → 이벤트
2. `screen_view` 이벤트 선택
3. `screen_name` 기준으로 파라미터 필터링

### 12.2 DebugView (실시간 검증)

```bash
adb shell setprop debug.firebase.analytics.app swyp.team.walkit
```

---

## 13. 검증 체크리스트

- [ ] 자동 screen_view 수집 비활성화 (AndroidManifest) 적용
- [ ] Splash → Login/Main 진입 시 `Splash` 1회 기록
- [ ] 동일 화면 재진입 시 `screen_view` 중복 미발생
- [ ] Main 진입 시 `Main` 자체는 기록하지 않고, 탭만 `Main_Home` 등으로 기록
- [ ] 탭 전환 시 `Main_Home`, `Main_Record` 등 기록
- [ ] **탭 재클릭 시 중복 로깅 미발생** 확인
- [ ] Walking, Mission 등 서브 화면 진입 시 해당 화면명 기록
- [ ] **백스택 복귀(Back) 시 이전 화면 `screen_view` 재기록 여부 확인** (의도: 재기록됨)
- [ ] CustomTest: Debug 빌드에서만 기록, Release에서 차단
- [ ] previousRoute: ViewModel 등 recomposition에 안전한 스코프에서 보관

---

## 14. 개인정보 및 보안

- `screen_view`에 사용자 식별자, 개인정보를 포함하지 않음
- Firebase Analytics는 기본적으로 익명 식별자 사용
- 추가 파라미터 전송 시 개인정보 처리 방침 및 내부 검토 필요

---

## 15. 참고 자료

- [Firebase Analytics 이벤트 로깅](https://firebase.google.com/docs/analytics/events)
- [screen_view 이벤트](https://support.google.com/analytics/answer/9267738)
- [Firebase 자동 화면 추적](https://firebase.google.com/docs/analytics/screen-reporting)
- [Firebase DebugView](https://firebase.google.com/docs/analytics/debugview)
