# Performance Optimization Rules

## 1. Compose 최적화
- `remember`와 `derivedStateOf`를 적절히 사용하여 불필요한 연산 방지.
- 불필요한 리컴포지션(Recomposition)이 발생하지 않도록 상태 범위를 최소화하십시오.
- 리스트 렌더링 시 `LazyColumn`, `LazyRow`를 사용하고 `key`를 지정하십시오.

## 2. 메모리 관리
- 이미지 리소스 캐싱 및 대용량 이미지 리사이징 처리.
- 메모리 누수 방지 (특히 코루틴 스코프와 뷰모델 수명 주기 준수).
- 대량의 데이터를 다룰 때는 Pagination(Paging 3 권장)을 적용하십시오.

## 3. 네트워크 및 실행 최적화
- API 응답 캐싱 및 배치 요청(Batch Request) 고려.
- 이미지 압축 전송 및 캐싱 전략 수립.
- 앱 실행 시 Cold Start 시간을 최소화하기 위해 지연 초기화(Lazy Initialization) 활용.
- 무거운 작업은 반드시 `Dispatchers.IO`에서 처리하여 UI 프리징을 방지하십시오.
