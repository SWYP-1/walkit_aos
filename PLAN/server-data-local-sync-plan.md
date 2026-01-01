# 재설치 시 서버 데이터 동기화 로직 구현 계획

## 현재 상황 분석

### API 응답 구조
- `/pages/home` 엔드포인트에서 응답을 받음
- 응답에는 `walkResponseDto`라는 산책 기록 데이터가 포함됨
- 현재 HomeViewModel에서는 이 데이터를 사용하지 않고 있음

### Room 상태
- HomeScreen에서 Room을 통해 산책 기록을 검색하지만 결과가 비어있음
- 재설치 시 로컬 데이터가 초기화되므로 서버 동기화가 필요함

### 현재 HomeViewModel 구조
- `loadHomeData()`: API 호출 및 UI 상태 업데이트
- `loadWalkingSessionsFromRoom()`: Room에서 독립적으로 데이터 로드
- API와 Room이 분리되어 동기화되지 않음

## 변경 계획

### 1. API 응답 모델 확장

#### 예상 변경사항
- `HomeData` 모델에 `walkResponseDto` 필드 추가
- `WalkResponseDto` 데이터 클래스 생성 (API 응답 매핑용)
- `WalkResponseDto`를 `WalkingSession`으로 변환하는 매퍼 생성

#### 필요한 파일들
- `domain/model/HomeData.kt`: walkResponseDto 필드 추가
- `data/model/WalkResponseDto.kt`: 새로운 DTO 클래스
- `data/mapper/WalkResponseDtoMapper.kt`: DTO ↔ Domain 변환 로직

### 2. HomeViewModel 동기화 로직 추가

#### 예상 변경사항
- `loadHomeData()` 메서드에서 API 응답 처리 후 Room 저장 로직 추가
- 재설치 감지 로직 구현 (Room 데이터가 비어있으면 서버 동기화 수행)
- 동기화 상태 관리 (동기화 중/완료/실패 상태)

#### 새로운 메서드들
- `syncWalkRecordsFromApi(walkResponseDto: WalkResponseDto)`: API 데이터를 Room에 저장
- `isFirstInstall()`: 재설치 여부 판단 로직
- `performInitialSync(homeData: HomeData)`: 초기 동기화 수행

#### 상태 관리 확장
- 동기화 상태를 위한 새로운 UiState 추가
- 동기화 진행률 표시를 위한 Progress 상태

### 3. HomeScreen UI 업데이트

#### 예상 변경사항
- 동기화 중 상태 표시 (로딩 화면 또는 진행률 바)
- 동기화 완료/실패 시 사용자 피드백
- 재설치 시 자동 동기화 안내 메시지

#### 새로운 UI 상태들
- SyncInProgress: 동기화 진행 중
- SyncCompleted: 동기화 완료
- SyncFailed: 동기화 실패

### 4. Repository 계층 변경

#### WalkingSessionRepository 확장
- `saveWalkRecordsFromApi(walkRecords: List<WalkingSession>)`: API 데이터를 일괄 저장
- `clearAllSessions()`: 재설치 시 기존 데이터 클리어 (선택적)
- `getSyncStatus()`: 동기화 상태 확인

#### HomeRepository 변경
- `walkResponseDto`를 응답에 포함하도록 API 인터페이스 수정
- DTO 변환 로직 추가

### 5. 동기화 전략 및 에러 처리

#### 동기화 시점
- 앱 설치 후 첫 실행 시
- Room 데이터가 비어있을 때
- 수동 동기화 기능 (설정에서 제공)

#### 에러 처리 전략
- 네트워크 실패: 재시도 로직 + 사용자 안내
- 데이터 변환 실패: 로깅 + 부분 성공 처리
- 저장 실패: 롤백 또는 부분 저장

#### 데이터 일관성
- 서버 데이터를 우선으로 Room 덮어쓰기
- 충돌 시 마지막 수정 시간 기준으로 판단
- 동기화 완료 시점 기록

### 6. 성능 및 UX 고려사항

#### 성능 최적화
- 대량 데이터일 경우 페이징 처리
- 백그라운드 스레드에서 동기화 수행
- 진행률 표시로 사용자 이탈 방지

#### 사용자 경험
- 동기화 중 앱 사용 제한 또는 부분 기능 허용
- 동기화 완료 시 성공 피드백
- 실패 시 재시도 옵션 제공

### 7. 테스트 계획

#### 단위 테스트
- DTO 변환 로직 테스트
- 동기화 상태 관리 테스트
- 에러 처리 시나리오 테스트

#### 통합 테스트
- API → Room 저장 플로우 테스트
- 재설치 시 동기화 테스트
- 네트워크 실패 시나리오 테스트

### 8. 마이그레이션 고려사항

#### 기존 데이터 처리
- 기존 Room 데이터와 API 데이터 비교
- 충돌 시 해결 전략 수립
- 점진적 마이그레이션 지원

#### 하위 호환성
- 구버전 앱과의 데이터 호환성
- 선택적 동기화 기능 제공

## 구현 순서

1. **모델 및 매퍼 생성** (DTO, Domain 변환 로직)
2. **Repository 계층 확장** (저장 로직 추가)
3. **ViewModel 동기화 로직 구현** (상태 관리 및 동기화 플로우)
4. **UI 상태 및 피드백 추가** (사용자 경험 개선)
5. **에러 처리 및 재시도 로직** (안정성 확보)
6. **테스트 코드 작성** (품질 검증)

## 관련 규칙 및 가이드라인

### 프로젝트 구조 규칙 준수
- [project-structure.mdc](../../.cursor/rules/project-structure.mdc) - 계층별 구조 유지
- [api-binding-rules.mdc](../../.cursor/rules/api-binding-rules.mdc) - API 데이터 바인딩 규칙
- [result-handling-rules.mdc](../../.cursor/rules/result-handling-rules.mdc) - Result 타입 사용

### 코루틴 및 비동기 처리
- [coroutine-rules.mdc](../../.cursor/rules/coroutine-rules.mdc) - viewModelScope 사용
- 백그라운드 작업은 IO 디스패처에서 수행

### UI/UX 고려사항
- [ui-viewmodel-rules.mdc](../../.cursor/rules/ui-viewmodel-rules.mdc) - 상태 관리 및 UI 업데이트
- 사용자에게 진행상황 명확히 표시

### 데이터 일관성
- [sync-strategy-rules.mdc](../../.cursor/rules/sync-strategy-rules.mdc) - 오프라인-first 전략
- 서버 데이터를 신뢰할 수 있는 소스로 간주

