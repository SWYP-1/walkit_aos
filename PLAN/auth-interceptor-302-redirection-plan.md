# AuthInterceptor 302 리다이렉트 및 토큰 갱신 처리 구현 계획

## 🎯 목표
AuthInterceptor에서 401 Unauthorized와 302 리다이렉트 응답을 감지하여 자동 토큰 갱신을 처리하도록 개선

## 📊 현재 상태 분석

### ✅ 기존 구현 장점
- 기본적인 인증 헤더 추가 로직 구현됨
- 302 리다이렉트 감지 로직 주석 처리 상태로 준비됨
- Timber 로깅 체계 구축됨
- TokenProvider 인터페이스 완전 구현됨

### ❌ 주요 문제점
- **401 응답 처리 로직 부재**: 현재 401 Unauthorized를 전혀 처리하지 못함
- **토큰 갱신 로직 없음**: 자동 토큰 갱신 기능이 전혀 구현되지 않음
- **동시성 제어 미흡**: 여러 요청이 동시에 인증 실패 시 중복 토큰 갱신 가능성
- **메서드명 불일치**: 문서상 `saveTokens()` vs 실제 `updateTokens()`

## 🛠️ 수정 구현 계획

### 1. 필드 및 Import 추가
```kotlin
// 추가 필드
private val lock = Any()
@Volatile
private var isRefreshing = false

// 추가 import
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
```

### 2. 인증 실패 감지 로직 개선
```kotlin
// 현재: 302와 HTML만 감지
val isAuthFailure = (response.code == 302 && response.header("Location")?.contains("/login") == true) ||
                   (response.header("Content-Type")?.contains("text/html") == true && !request.url.encodedPath.contains("/auth/"))

// 개선: 401, 302, HTML 모두 감지
val isAuthFailure = response.code == 401 ||
                   (response.code == 302 && response.header("Location")?.contains("/login") == true) ||
                   (response.header("Content-Type")?.contains("text/html") == true && !request.url.encodedPath.contains("/auth/"))
```

### 3. 토큰 갱신 로직 구현
```kotlin
if (isAuthFailure) {
    Timber.e("AuthInterceptor - 인증 실패 감지! 코드: ${response.code}, Location: ${response.header("Location")}")

    // 동시성 제어: 한 번에 하나의 토큰 갱신만 수행
    synchronized(lock) {
        if (!isRefreshing) {
            isRefreshing = true
            try {
                // 토큰 갱신 시도
                val refreshSuccess = runBlocking { refreshToken(chain) }

                if (refreshSuccess) {
                    Timber.d("AuthInterceptor - 토큰 갱신 성공, 원래 요청 재시도")
                    // 새 토큰으로 원래 요청 재시도
                    val newAccessToken = tokenProvider.getAccessToken()
                    val retryRequest = request.newBuilder()
                        .header("Authorization", "Bearer $newAccessToken")
                        .build()
                    response.close() // 기존 응답 닫기
                    return chain.proceed(retryRequest)
                } else {
                    Timber.e("AuthInterceptor - 토큰 갱신 실패")
                    response.close()
                    // TODO: 로그인 화면으로 이동하는 이벤트 발생 필요
                    return response
                }
            } finally {
                isRefreshing = false
            }
        } else {
            Timber.d("AuthInterceptor - 다른 스레드에서 토큰 갱신 중, 대기 후 재시도")
            // 다른 스레드가 갱신 중이면 잠시 대기
            Thread.sleep(100)

            // 갱신 완료된 새 토큰으로 재시도
            val newAccessToken = tokenProvider.getAccessToken()
            if (!newAccessToken.isNullOrBlank()) {
                val retryRequest = request.newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .build()
                response.close()
                return chain.proceed(retryRequest)
            }
        }
    }
}
```

### 4. refreshToken() 메서드 구현
```kotlin
private suspend fun refreshToken(chain: Interceptor.Chain): Boolean {
    return try {
        val refreshToken = tokenProvider.getRefreshToken()

        if (refreshToken.isNullOrBlank()) {
            Timber.e("AuthInterceptor - Refresh token이 없습니다")
            tokenProvider.clearTokens()
            return false
        }

        Timber.d("AuthInterceptor - Refresh token으로 토큰 갱신 요청")

        // Refresh API 요청 생성
        val jsonBody = JSONObject().apply {
            put("refreshToken", refreshToken)
        }.toString()

        val originalRequest = chain.request()
        val baseUrl = "${originalRequest.url.scheme}://${originalRequest.url.host}"

        val refreshRequest = Request.Builder()
            .url("$baseUrl/auth/refresh")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        val refreshResponse = chain.proceed(refreshRequest)

        if (refreshResponse.isSuccessful) {
            val responseBody = refreshResponse.body?.string()
            Timber.d("AuthInterceptor - Refresh 응답: $responseBody")

            if (responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                val newAccessToken = jsonResponse.optString("accessToken")
                val newRefreshToken = jsonResponse.optString("refreshToken")

                if (newAccessToken.isNotBlank()) {
                    // 새 토큰 저장 (updateTokens 사용)
                    tokenProvider.updateTokens(newAccessToken, newRefreshToken.takeIf { it.isNotBlank() })
                    Timber.d("AuthInterceptor - 새 토큰 저장 완료")
                    refreshResponse.close()
                    return true
                } else {
                    Timber.e("AuthInterceptor - 응답에 accessToken이 없습니다")
                    tokenProvider.clearTokens()
                    refreshResponse.close()
                    return false
                }
            } else {
                Timber.e("AuthInterceptor - Refresh 응답 body가 null입니다")
                tokenProvider.clearTokens()
                refreshResponse.close()
                return false
            }
        } else {
            Timber.e("AuthInterceptor - Refresh 실패: ${refreshResponse.code}")
            tokenProvider.clearTokens()
            refreshResponse.close()
            return false
        }
    } catch (e: Exception) {
        Timber.e(e, "AuthInterceptor - 토큰 갱신 중 예외 발생")
        tokenProvider.clearTokens()
        return false
    }
}
```

### 5. HTML 응답 감지 로직 정리
```kotlin
// HTML 응답 감지 (리다이렉트를 따라간 경우)
// 토큰 갱신 로직에서 처리하므로 별도 처리 불필요
// 필요시 로그인 화면 이동 이벤트만 추가
```

## 🔧 기술적 고려사항

### 동시성 제어 전략
- `synchronized(lock)`: 한 번에 하나의 토큰 갱신만 허용
- `@Volatile isRefreshing`: 메모리 가시성 보장
- 다른 스레드 대기 후 재시도: 중복 API 호출 방지

### 에러 처리 방안
- 네트워크 실패: 토큰 클리어 후 로그인 화면 이동
- JSON 파싱 실패: 토큰 클리어
- 토큰 저장 실패: 토큰 클리어 및 로깅

### 성능 최적화
- 토큰 갱신 중 다른 요청들은 100ms 대기 후 재시도
- 성공 시 즉시 새 토큰으로 요청 재시도
- 실패 시 추가 네트워크 호출 방지

## 📋 구현 단계

### Phase 1: 기본 인프라
1. 필드 및 import 추가
2. refreshToken() 메서드 구현
3. 인증 실패 감지 로직 개선

### Phase 2: 토큰 갱신 로직
1. 동시성 제어 로직 구현
2. 토큰 갱신 성공/실패 처리
3. 요청 재시도 로직 구현

### Phase 3: 테스트 및 검증
1. 단위 테스트 작성
2. 통합 테스트 수행
3. 엣지 케이스 검증

## 🧪 테스트 시나리오

### 기본 테스트
- ✅ 401 응답 시 토큰 갱신 동작
- ✅ 302 응답 시 토큰 갱신 동작
- ✅ 토큰 갱신 성공 시 원래 요청 재시도
- ✅ 토큰 갱신 실패 시 토큰 클리어

### 동시성 테스트
- ✅ 여러 요청이 동시에 401/302 수신 시 한 번만 갱신
- ✅ 다른 스레드들은 대기 후 새 토큰으로 재시도
- ✅ 토큰 갱신 중 새 요청들은 대기

### 에러 테스트
- ✅ Refresh 토큰 없음: 토큰 클리어
- ✅ Refresh API 실패: 토큰 클리어
- ✅ 응답 파싱 실패: 토큰 클리어
- ✅ 네트워크 예외: 토큰 클리어

### 엣지 케이스
- ✅ HTML 응답 감지 (302를 따라간 경우)
- ✅ Location 헤더 없는 302 응답
- ✅ 빈 accessToken 응답
- ✅ refreshToken 갱신 없이 accessToken만 갱신

## 🚨 주의사항

### API 응답 형식 가정
```json
{
  "accessToken": "new_access_token_here",
  "refreshToken": "new_refresh_token_here" // optional
}
```

### 보안 고려사항
- Refresh 토큰은 HTTPS만 사용
- 토큰은 메모리에만 저장 (디스크 미저장)
- 실패 시 즉시 토큰 클리어

### 성능 고려사항
- 토큰 갱신은 최대 1회만 수행
- 재시도 대기 시간은 100ms로 제한
- 불필요한 API 호출 방지

## 📈 예상 이점

1. **사용자 경험 개선**: 자동 토큰 갱신으로 재로그인 불필요
2. **안정성 향상**: 401/302 응답 자동 처리
3. **동시성 문제 해결**: 중복 토큰 갱신 방지
4. **로그인 유지율 증가**: 세션 만료 시 자동 복구

---

*작성일: 2025-01-01*
*작성자: AI Assistant*
*리뷰어: TBD*
