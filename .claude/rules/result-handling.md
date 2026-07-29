# Result Type Handling Rules

## 1. Result 클래스 정의
타입 안전한 상태 관리를 위해 아래의 실드 인터페이스를 사용합니다.

```kotlin
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val exception: Throwable, val message: String? = null) : Result<Nothing>
    data object Loading : Result<Nothing>
}
```

## 2. 확장 함수 활용
- **map**: 성공 상태의 데이터를 변환할 때 사용.
- **onSuccess / onError**: 결과에 따른 사이드 이펙트 처리 시 사용.

## 3. 계층별 사용 패턴
- **Repository**: 모든 작업의 결과를 `Result<T>`로 래핑하여 반환.
- **ViewModel**: `Result` 상태를 `StateFlow`에 담아 UI로 전달.
- **UI**: `when` 식을 사용하여 `Loading`, `Success`, `Error` 상태를 모두 처리.

## 4. 준수 사항
- 실패 가능성이 있는 모든 작업(Network, DB)에 `Result` 사용 필수.
- `Result` 타입을 중첩해서 사용하지 마십시오 (예: `Result<Result<T>>` 금지).
- UI 레이어에서는 모든 상태를 사용자에게 적절히 피드백(인디케이터, 에러 메시지 등)해야 합니다.
