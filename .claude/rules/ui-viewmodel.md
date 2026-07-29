# UI and ViewModel Rules (Jetpack Compose)

## 1. 비즈니스 로직 분리
- **❌ 금지**: Composable 내부에서 비즈니스 로직 직접 처리.
- **✅ 권장**: 모든 로직은 ViewModel에서 처리하고 UI는 상태를 관찰하여 렌더링.

## 2. ViewModel 조직화
- 주요 기능(Feature) 단위로 하나의 ViewModel 유지.
- 서브 기능마다 ViewModel을 쪼개지 말고, 연관된 기능은 하나의 ViewModel에서 관리하십시오.
- **Naming**: `FunctionName + ViewModel`

## 3. Screen Composable 패턴
- **Screen Route**: ViewModel 주입(`hiltViewModel()`) 및 상태 수집 전용.
- **Screen Content**: UI 렌더링 전용. 상태와 콜백을 매개변수로 받음.

### ✅ 권장 패턴
```kotlin
@Composable
fun FeatureRoute(
    viewModel: FeatureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FeatureScreen(
        uiState = uiState,
        onAction = viewModel::handleAction
    )
}
```
- **collectAsStateWithLifecycle()**: Lifecycle-aware하게 상태를 수집하여 리소스 낭비 방지 (필수).
- **Method References**: 콜백 전달 시 `viewModel::onAction` 사용 권장.

## 4. 상태 관리
- ViewModel에서 `StateFlow` 사용.
- Unidirectional Data Flow (UDF) 준수.
- `remember`와 `derivedStateOf`를 사용하여 불필요한 계산 및 리컴포지션 방지.

## 5. UI 구성 요소 및 스타일
- 재사용 컴포넌트는 `components/` 디렉토리에 위치.
- 모든 컴포넌트는 독립적이어야 하며 테마 시스템을 따라야 함.
- Accessibility (접근성)를 고려하여 `contentDescription` 필수 작성.
- **Naming**: 클래스(PascalCase), 함수/변수(camelCase), Composable(PascalCase).
- **주석**: **한국어**로 작성하며 KDoc 형식 사용.
- **길이 제한**: 함수 50줄, 파일 500줄 내외.
