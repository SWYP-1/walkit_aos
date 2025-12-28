# 서버 응답 구조 - 이미지 URL 처리 가이드

## 현재 상황

현재 `WalkApi.saveWalk()` 메서드는 `Response<Unit>`을 반환합니다. 이는 서버가 응답 본문에 데이터를 반환하지 않는다는 의미입니다.

```kotlin
@POST("/walk/save")
suspend fun saveWalk(
    @Part("data") data: RequestBody,
    @Part image: MultipartBody.Part?
): Response<Unit>  // ← 현재는 Unit (응답 본문 없음)
```

## 문제점

설계 문서에 따르면 서버 동기화 후 `serverImageUrl`을 저장해야 하지만, 현재 서버 응답 구조에서는 이미지 URL을 받을 수 없습니다.

## 해결 방법

### 옵션 1: 서버 API 응답 구조 변경 (권장)

서버가 이미지 URL을 응답 본문에 포함하도록 API를 변경합니다.

#### 1-1. 응답 DTO 생성

```kotlin
// app/src/main/java/team/swyp/sdu/data/remote/walking/dto/WalkSaveResponse.kt
package team.swyp.sdu.data.remote.walking.dto

import kotlinx.serialization.Serializable

/**
 * 산책 저장 API 응답
 */
@Serializable
data class WalkSaveResponse(
    val id: String,
    val imageUrl: String?,  // 서버에 업로드된 이미지 URL
    val createdAt: String
)
```

#### 1-2. WalkApi 수정

```kotlin
// app/src/main/java/team/swyp/sdu/data/api/walking/WalkApi.kt
@Multipart
@POST("/walk/save")
suspend fun saveWalk(
    @Part("data") data: RequestBody,
    @Part image: MultipartBody.Part?
): Response<WalkSaveResponse>  // ← Unit → WalkSaveResponse로 변경
```

#### 1-3. Repository 수정

```kotlin
// app/src/main/java/team/swyp/sdu/data/repository/WalkingSessionRepository.kt
when (result) {
    is Result.Success -> {
        val response = result.data
        if (response.isSuccessful) {
            // 서버 응답에서 imageUrl 추출
            val responseBody = response.body()
            val serverImageUrl = responseBody?.imageUrl
            
            val updatedEntity = entity.copy(
                serverImageUrl = serverImageUrl // 서버에서 받은 URL 저장
                // localImagePath는 유지 (오프라인 지원)
            )
            walkingSessionDao.update(updatedEntity)
            
            // 동기화 성공
            walkingSessionDao.updateSyncState(localId, SyncState.SYNCED)
            Timber.d("서버 동기화 성공: localId=$localId, serverImageUrl=$serverImageUrl")
        } else {
            throw Exception(response.message() ?: "서버 동기화 실패")
        }
    }
    // ...
}
```

### 옵션 2: 현재 구조 유지 (임시 해결책)

서버 API를 변경할 수 없는 경우, 다음과 같은 방법을 사용할 수 있습니다:

#### 2-1. 이미지 업로드 후 별도 API 호출

이미지를 먼저 업로드하고, 업로드된 이미지 URL을 받은 후 산책 데이터를 저장합니다.

```kotlin
// 1. 이미지 업로드
val imageUploadResponse = imageUploadApi.uploadImage(imageFile)
val serverImageUrl = imageUploadResponse.imageUrl

// 2. 산책 데이터 저장 (imageUrl 포함)
val sessionWithImageUrl = session.copy(serverImageUrl = serverImageUrl)
walkApi.saveWalk(sessionWithImageUrl, null) // 이미지는 이미 업로드됨
```

#### 2-2. 서버 응답 헤더에서 URL 추출

일부 서버는 응답 헤더에 이미지 URL을 포함합니다.

```kotlin
val response = result.data
val serverImageUrl = response.headers()["X-Image-Url"] // 서버가 헤더에 포함하는 경우
```

#### 2-3. 로컬 경로를 서버 URL로 변환 (개발/테스트용)

개발 단계에서는 로컬 경로를 서버 URL 형식으로 변환하여 사용할 수 있습니다.

```kotlin
// 개발/테스트용: 로컬 경로를 서버 URL 형식으로 변환
val serverImageUrl = entity.localImagePath?.let { localPath ->
    // 예: "https://api.example.com/images/${sessionId}.jpg"
    // 실제로는 서버에서 반환된 URL을 사용해야 함
    "https://api.example.com/images/${session.id}.jpg"
}
```

## 권장 사항

**옵션 1 (서버 API 응답 구조 변경)**을 강력히 권장합니다.

### 이유:
1. **명확성**: 서버가 업로드된 이미지의 URL을 명확하게 반환
2. **안정성**: 클라이언트가 서버의 실제 이미지 URL을 사용
3. **확장성**: 향후 다른 필드도 응답에 포함 가능
4. **표준**: REST API 설계 관례에 부합

### 구현 순서:
1. 백엔드 팀과 협의하여 응답 구조 변경
2. `WalkSaveResponse` DTO 생성
3. `WalkApi.saveWalk()` 반환 타입 변경
4. `WalkingSessionRepository.syncSessionToServer()` 수정
5. 테스트 및 검증

## 현재 코드 상태

현재 `syncSessionToServer()` 메서드에는 TODO 주석이 남아있습니다:

```kotlin
// TODO: 서버 응답 구조에 맞게 imageUrl 추출 필요
// 현재는 Response<Unit>이므로 서버 응답 구조 변경 필요
val serverImageUrl: String? = null // TODO: 서버 응답에서 imageUrl 추출
```

서버 API가 변경되면 이 부분을 수정하여 실제 응답에서 `imageUrl`을 추출하도록 업데이트해야 합니다.

## 참고사항

- 서버 API 변경 전까지는 `serverImageUrl`이 `null`로 저장됩니다.
- 이미지 로딩 시 `getImageUri()` 메서드가 로컬 파일을 우선 사용하므로, 오프라인 환경에서는 문제없이 동작합니다.
- 다중 기기 동기화 시에는 서버 URL이 필요하므로, 서버 API 변경이 완료되면 해당 기능이 정상 작동합니다.







