# 산책 종료 후 감정 기록 네비게이션 플랜

## 현재 구조 분석

### 기존 흐름
```
WalkingScreen (산책 중지)
  ↓ onNavigateToResult()
WalkingResultScreen
```

### 요구사항 흐름
```
WalkingScreen (산책 중지)
  ↓
WalkingFinishStep (산책 종료 확인)
  ↓ "감정 기록하기" 클릭
EmotionSelectionStep (단계 1: 감정 선택 - 슬라이더)
  ↓ "다음" 클릭
EmotionRecordStep (단계 2: 감정 기록 - 사진/텍스트)
  ↓ "다음" 클릭
WalkingResultScreen
```

## 네비게이션 구조 설계

### 1. Screen Route 정의

`NavGraph.kt`의 `Screen` sealed class에 추가:

```kotlin
sealed class Screen(val route: String) {
    // ... 기존 routes ...
    
    data object WalkingFinishStep : Screen("walking_finish_step")
    data object EmotionSelectionStep : Screen("emotion_selection_step")
    data object EmotionRecordStep : Screen("emotion_record_step")
}
```

### 2. 네비게이션 흐름

#### 2.1 WalkingScreen → WalkingFinishStep
- **트리거**: `onStopClick`에서 `viewModel.stopWalking()` 호출 후
- **액션**: `navController.navigate(Screen.WalkingFinishStep.route)`
- **백 스택**: `Walking` 화면 유지 (뒤로가기 가능)

#### 2.2 WalkingFinishStep → EmotionSelectionStep
- **트리거**: "감정 기록하기" 버튼 클릭
- **액션**: `navController.navigate(Screen.EmotionSelectionStep.route)`
- **백 스택**: `WalkingFinishStep` 유지
- **대안**: "넘어가기" 클릭 시 → `WalkingResultScreen`으로 직접 이동

#### 2.3 EmotionSelectionStep → EmotionRecordStep
- **트리거**: "다음" 버튼 클릭 (감정 선택 완료)
- **액션**: `navController.navigate(Screen.EmotionRecordStep.route)`
- **백 스택**: `EmotionSelectionStep` 유지
- **데이터 전달**: 선택된 감정 값 (슬라이더 값)을 ViewModel에 저장

#### 2.4 EmotionRecordStep → WalkingResultScreen
- **트리거**: "다음" 버튼 클릭 (사진/텍스트 입력 완료)
- **액션**: `navController.navigate(Screen.WalkingResult.route)`
- **백 스택**: 감정 기록 단계들 제거 (`popUpTo(Screen.Walking.route)`)
- **데이터 전달**: 사진 URI, 텍스트를 ViewModel에 저장

### 3. 뒤로가기 처리

#### 3.1 각 단계에서의 뒤로가기
- **WalkingFinishStep**: 뒤로가기 → `WalkingScreen` (산책 재개 가능하도록)
- **EmotionSelectionStep**: 뒤로가기 → `WalkingFinishStep`
- **EmotionRecordStep**: 뒤로가기 → `EmotionSelectionStep`
- **X 버튼**: 각 화면에서 제공, `WalkingScreen`으로 돌아가기

#### 3.2 백 스택 관리 전략

**옵션 A: 단계별 백 스택 유지 (권장)**
```kotlin
// 각 단계에서 뒤로가기 가능
WalkingScreen → WalkingFinishStep → EmotionSelectionStep → EmotionRecordStep → WalkingResultScreen
```

**옵션 B: 결과 화면 도달 시 이전 단계 제거**
```kotlin
// WalkingResultScreen 도달 시 감정 기록 단계들 제거
navController.navigate(Screen.WalkingResult.route) {
    popUpTo(Screen.Walking.route) { inclusive = false }
}
```

### 4. 데이터 관리

#### 4.1 ViewModel 상태 추가
`WalkingViewModel`에 감정 기록 관련 상태 추가:

```kotlin
// 감정 선택 단계
data class EmotionSelectionState(
    val emotionValue: Float = 0.5f, // 슬라이더 값 (0.0 ~ 1.0)
)

// 감정 기록 단계
data class EmotionRecordState(
    val photoUri: Uri? = null,
    val emotionText: String = "",
)
```

#### 4.2 데이터 전달 방식
- **ViewModel 공유**: 모든 단계에서 같은 `WalkingViewModel` 인스턴스 사용 (`hiltViewModel()`)
- **상태 저장**: 각 단계에서 입력한 데이터를 ViewModel에 저장
- **최종 전달**: `WalkingResultScreen`에서 ViewModel의 감정 데이터 사용

### 5. 화면 구현 계획

#### 5.1 EmotionSelectionStep (단계 1)
**요구사항**:
- 진행률 표시기 (3단계 중 1단계)
- "산책 종료" 제목
- "산책 후 감정 기록하기" 부제목
- 감정 선택 슬라이더 (캐릭터 표정 변화)
- "다음" 버튼
- X 버튼 (닫기)

**구현 위치**: `app/src/main/java/team/swyp/sdu/ui/walking/EmotionSelectionStep.kt`

#### 5.2 EmotionRecordStep (단계 2)
**요구사항**:
- 진행률 표시기 (3단계 중 2단계)
- "산책 후 나의 마음은 어떤가요?" 섹션 (접을 수 있음, 완료 표시)
- "산책을 하며 느꼈던 감정을 기록해보세요!" 섹션
  - 사진 입력 영역
  - 텍스트 입력 영역
- "다음" 버튼
- X 버튼 (닫기)

**구현 위치**: `app/src/main/java/team/swyp/sdu/ui/walking/EmotionRecordStep.kt`

### 6. 네비게이션 그래프 구성

```kotlin
// NavGraph.kt에 추가할 composable들

composable(Screen.WalkingFinishStep.route) {
    WalkingFinishStep(
        onClose = {
            navController.navigate(Screen.Main.route) {
                popUpTo(Screen.Walking.route) { inclusive = false }
            }
        },
        onSkip = {
            navController.navigate(Screen.WalkingResult.route) {
                popUpTo(Screen.Walking.route) { inclusive = false }
            }
        },
        onRecordEmotion = {
            navController.navigate(Screen.EmotionSelectionStep.route)
        },
    )
}

composable(Screen.EmotionSelectionStep.route) {
    val viewModel: WalkingViewModel = hiltViewModel()
    
    EmotionSelectionStep(
        viewModel = viewModel,
        onNext = {
            navController.navigate(Screen.EmotionRecordStep.route)
        },
        onClose = {
            navController.navigate(Screen.Main.route) {
                popUpTo(Screen.Walking.route) { inclusive = false }
            }
        },
    )
}

composable(Screen.EmotionRecordStep.route) {
    val viewModel: WalkingViewModel = hiltViewModel()
    
    EmotionRecordStep(
        viewModel = viewModel,
        onNext = {
            navController.navigate(Screen.WalkingResult.route) {
                popUpTo(Screen.Walking.route) { inclusive = false }
            }
        },
        onClose = {
            navController.navigate(Screen.Main.route) {
                popUpTo(Screen.Walking.route) { inclusive = false }
            }
        },
    )
}
```

### 7. WalkingScreen 수정 사항

```kotlin
// WalkingScreen.kt의 onStopClick 수정
onStopClick = {
    viewModel.stopWalking()
    // 기존: onNavigateToResult()
    // 변경: WalkingFinishStep으로 이동
    navController.navigate(Screen.WalkingFinishStep.route)
}
```

### 8. 진행률 표시기 구현

두 단계 화면 모두에 공통 컴포넌트로 구현:

```kotlin
@Composable
fun EmotionProgressIndicator(
    currentStep: Int, // 1 or 2
    totalSteps: Int = 3
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            val stepNumber = index + 1
            Box(
                modifier = Modifier
                    .width(if (stepNumber == currentStep) 40.dp else 20.dp)
                    .height(4.dp)
                    .background(
                        color = if (stepNumber <= currentStep) 
                            Color(0xFF2E2E2E) 
                        else 
                            Color(0xFFE5E5E5),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            if (stepNumber < totalSteps) {
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}
```

## 구현 순서

1. ✅ **Screen Route 추가** (`NavGraph.kt`)
2. ✅ **EmotionSelectionStep 화면 구현**
3. ✅ **EmotionRecordStep 화면 구현**
4. ✅ **네비게이션 그래프에 composable 추가**
5. ✅ **WalkingScreen의 onStopClick 수정**
6. ✅ **WalkingFinishStep 네비게이션 연결**
7. ✅ **ViewModel에 감정 데이터 상태 추가**
8. ✅ **진행률 표시기 컴포넌트 구현**

## 주의사항

1. **ViewModel 상태 관리**: 모든 단계에서 같은 ViewModel 인스턴스 사용
2. **뒤로가기 처리**: 각 단계에서 적절한 백 스택 관리
3. **데이터 유지**: 사용자가 뒤로가기를 해도 입력한 데이터 유지
4. **에지 케이스**: "넘어가기" 선택 시 감정 기록 단계 건너뛰기
5. **X 버튼**: 모든 단계에서 제공, Main 화면으로 돌아가기










