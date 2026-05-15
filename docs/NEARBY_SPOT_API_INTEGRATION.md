# Design Document: 주변 장소 조회 API 연동

## 1. 개요

지도 탭(InteractiveMap)에서 사용자 주변의 추천 장소(카페, 공원 등)를 조회하는
`GET /spots/nearby` API를 연동합니다. 현재 ViewModel에는 하드코딩된 더미 마커가 있으며,
이를 실제 서버 데이터로 교체하는 것이 목표입니다.

---

## 2. API 명세

### Endpoint
```
GET /spots/nearby
```

### Query Parameters

| 파라미터 | 타입   | 필수 | 설명                          | 예시       |
|---------|--------|------|-------------------------------|------------|
| query   | String | ✅   | 검색 키워드                    | 카페       |
| x       | Double | ✅   | 중심 경도 (longitude)          | 127.027583 |
| y       | Double | ✅   | 중심 위도 (latitude)           | 37.497942  |
| radius  | Int    | ✅   | 검색 반경 (미터 단위, 최대 20000) | 1000      |
| size    | Int    | ✅   | 결과 개수 (최대 15)            | 15         |
| sort    | String | ✅   | 정렬 기준 (`distance` / `accuracy`) | distance |

### Response (200 OK)
```json
[
  {
    "placeName": "string",
    "addressName": "string",
    "roadAddressName": "string",
    "distance": "string",
    "placeUrl": "string",
    "blogReviewCount": 0,
    "blogReviewLink": "string",
    "thumbnailUrl": "string",
    "x": "string",
    "y": "string"
  }
]
```

| 필드              | 타입   | 설명                     |
|------------------|--------|--------------------------|
| placeName        | String | 장소명                   |
| addressName      | String | 지번 주소                |
| roadAddressName  | String | 도로명 주소              |
| distance         | String | 중심점으로부터 거리 (미터) |
| placeUrl         | String | 카카오맵 상세 URL        |
| blogReviewCount  | Int    | 블로그 리뷰 수           |
| blogReviewLink   | String | 블로그 리뷰 링크         |
| thumbnailUrl     | String | 대표 이미지 URL          |
| x                | String | 경도 (longitude)         |
| y                | String | 위도 (latitude)          |

---

## 3. 생성/수정 파일 목록

### 3.1 신규 생성

| 파일 경로 | 설명 |
|-----------|------|
| `data/api/spot/SpotApi.kt` | Retrofit 인터페이스 |
| `data/remote/spot/dto/NearbySpotDto.kt` | 응답 DTO |
| `data/remote/spot/SpotRemoteDataSource.kt` | 원격 데이터 소스 |
| `domain/model/NearbySpot.kt` | 도메인 모델 |
| `domain/repository/SpotRepository.kt` | Repository 인터페이스 |
| `data/repository/SpotRepositoryImpl.kt` | Repository 구현체 |
| `di/SpotModule.kt` | Hilt 모듈 (Repository 바인딩) |

### 3.2 수정

| 파일 경로 | 변경 내용 |
|-----------|-----------|
| `ui/interactivemap/InteractiveMapViewModel.kt` | Repository 주입, API 호출로 마커 교체 |
| `ui/interactivemap/InteractiveMapUiState.kt` (또는 동일 파일 내) | `isLoading`, `errorMessage` 필드 추가 |
| `data/model/MapMarker.kt` | `NearbySpot` → `MapMarker` 매핑 함수 추가 (또는 별도 mapper 생성) |
| `di/NetworkModule.kt` | `SpotApi` provide 추가 |

---

## 4. 구현 상세

### 4.1 DTO (`NearbySpotDto.kt`)
- `@Serializable` + `@SerialName` 사용 (기존 프로젝트 패턴 준수)
- 모든 필드 Nullable + 기본값 설정

### 4.2 RemoteDataSource (`SpotRemoteDataSource.kt`)
- `Result<List<NearbySpotDto>>` 반환
- `try-catch` 로 `AuthExpiredException` 과 일반 Throwable 분리 처리
- Timber 로그 포함

### 4.3 Repository
- `SpotRepository` 인터페이스: `getNearbySpots(...): Result<List<NearbySpot>>`
- `SpotRepositoryImpl`: DTO → 도메인 모델 매핑 처리

### 4.4 ViewModel (`InteractiveMapViewModel.kt`)
- `SpotRepository` 생성자 주입
- `viewModelScope.launch` + `Dispatchers.IO` 에서 호출
- UiState: `isLoading`, `spots: List<NearbySpot>`, `errorMessage: String?`
- 기존 하드코딩 더미 마커 제거

---

## 5. UiState 설계

```kotlin
data class InteractiveMapUiState(
    val isLoading: Boolean = false,
    val spots: List<NearbySpot> = emptyList(),
    val selectedSpot: NearbySpot? = null,
    val showBottomSheet: Boolean = false,
    val errorMessage: String? = null
)
```

---

## 6. 구현 순서

1. `NearbySpotDto.kt` 생성
2. `SpotApi.kt` 생성
3. `SpotRemoteDataSource.kt` 생성
4. `NearbySpot.kt` (도메인 모델) 생성
5. `SpotRepository.kt` 인터페이스 생성
6. `SpotRepositoryImpl.kt` 구현체 생성
7. `SpotModule.kt` Hilt 모듈 생성
8. `NetworkModule.kt` 에 `SpotApi` 추가
9. `InteractiveMapViewModel.kt` 업데이트
10. 빌드 확인 (`./gradlew assembleDebug`)

---

## 7. 미결 사항

- 검색 `query` 기본값 설정 여부 (예: "카페", "공원" 등 카테고리 필터 UI)
- 지도 이동 시 자동 재조회 여부 (카메라 이동 후 새 좌표로 재요청)
- `radius` / `size` 값을 UI에서 조정 가능하게 할지 여부
- `thumbnailUrl` 이미지 로딩 라이브러리 (Coil 사용 여부 확인 필요)
