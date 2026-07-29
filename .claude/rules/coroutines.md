# Android Coroutine Best Practices

## 1. 아키텍처 원칙
- 비즈니스 로직은 `LaunchedEffect`가 아닌 **ViewModel**에 위치시켜야 합니다.
- `LaunchedEffect`는 단순 트리거 및 사이드 이펙트용으로만 사용하십시오.
- **Scopes**: 
  - ViewModel: `viewModelScope`
  - Activity/Fragment: `lifecycleScope`
  - **GlobalScope 사용 금지**.

## 2. Dispatchers
- **Network/DB/File**: `Dispatchers.IO`
- **CPU 연산 (Sorting, Parsing 등)**: `Dispatchers.Default`
- **UI 업데이트**: `Dispatchers.Main`
- suspend 함수 내에서 `withContext`를 사용하여 적절한 디스패처로 명시적 전환 필수.

## 3. 예외 처리
- 모든 코루틴 작업은 `try-catch` 또는 `CoroutineExceptionHandler`를 통해 예외를 관리하십시오.
- 예외가 처리되지 않은 채로 방치되지 않도록 주의하십시오.

## 4. Structured Concurrency
- 병렬 작업 시 `coroutineScope` 또는 `supervisorScope` 사용.
- 여러 작업의 결과를 기다릴 때 `async/await` 사용.
- 코루틴 완료를 기다리기 위해 `delay()`를 사용하는 안티패턴을 피하십시오. (대신 콜백, 리스너 또는 `suspendCoroutine` 사용)

## 5. Flow 사용 규칙
- ViewModel의 UI 상태는 `StateFlow`로 관리.
- UI 레이어에서 `repeatOnLifecycle(Lifecycle.State.STARTED)`를 사용하여 Flow 수집.
- 백그라운드 작업이 필요한 Flow에는 `flowOn(Dispatchers.IO)` 적용.

## 6. 안티패턴 주의
- **❌ 금지**: `runBlocking`을 메인 스레드에서 호출.
- **❌ 금지**: `Thread.sleep()` 사용. 코루틴 내에서는 반드시 `delay()` 사용.
- **❌ 금지**: 비동기 작업 완료를 기다리기 위해 임의의 `delay(500)` 등을 사용.
