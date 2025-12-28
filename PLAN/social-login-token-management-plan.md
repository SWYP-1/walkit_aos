# 소셜 로그인 토큰 관리 플랜

## 목표
카카오/네이버 로그인 후 서버에서 받은 토큰을 모든 API 요청 헤더에 자동으로 추가

## 현재 구조 분석

### 기존 파일들
- ✅ `LoginViewModel.kt` - 카카오/네이버 로그인 처리
- ✅ `AuthApi.kt` - `loginWithKakao`, `loginWithNaver` API 정의
- ✅ `SocialLoginRequest.kt` - 요청 DTO
- ✅ `AuthTokenResponse.kt` - 응답 DTO
- ✅ `AuthDataStore.kt` - 토큰 저장/조회용 DataStore
- ✅ `NetworkModule.kt` - OkHttpClient, Retrofit 제공

### 현재 플로우
1. 사용자가 카카오/네이버 로그인 버튼 클릭
2. `LoginViewModel.loginWithKakaoTalk()` 또는 `loginWithNaver()` 호출
3. 소셜 로그인 SDK로부터 OAuthToken 받음
4. **현재는 여기서 끝** - 서버 API 호출 없음

## 구현 플랜

### 1. 생성할 파일들

#### 1.1 `AuthInterceptor.kt` (새로 생성)
**위치**: `app/src/main/java/team/swyp/sdu/data/remote/interceptor/AuthInterceptor.kt`

**역할**: 모든 API 요청에 Authorization 헤더 추가

**구현 내용**:
```kotlin
@Singleton
class AuthInterceptor @Inject constructor(
    private val authDataStore: AuthDataStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // 토큰이 필요한 요청인지 확인 (선택적)
        // AuthApi 호출은 토큰 불필요하므로 제외 가능
        
        val token = runBlocking {
            authDataStore.accessToken.first()
        }
        
        val newRequest = if (token != null) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }
        
        return chain.proceed(newRequest)
    }
}
```

#### 1.2 `AuthRemoteDataSource.kt` (새로 생성 또는 수정)
**위치**: `app/src/main/java/team/swyp/sdu/data/remote/auth/AuthRemoteDataSource.kt`

**역할**: AuthApi 호출 및 응답 처리

**구현 내용**:
```kotlin
@Singleton
class AuthRemoteDataSource @Inject constructor(
    private val authApi: AuthApi
) {
    suspend fun loginWithKakao(accessToken: String): Result<AuthTokenResponse> {
        return try {
            val response = authApi.loginWithKakao(
                SocialLoginRequest(accessToken)
            )
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    suspend fun loginWithNaver(accessToken: String): Result<AuthTokenResponse> {
        return try {
            val response = authApi.loginWithNaver(
                SocialLoginRequest(accessToken)
            )
            Result.Success(response)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
```

### 2. 수정할 파일들

#### 2.1 `NetworkModule.kt` 수정
**변경 사항**:
- `AuthInterceptor`를 `OkHttpClient`에 추가
- `provideOkHttpClient`에 `AuthInterceptor` 파라미터 추가
- `provideRetrofit`에서 `@Named("walkit")` 사용 (서버 API용)

**수정 내용**:
```kotlin
@Provides
@Singleton
fun provideOkHttpClient(
    loggingInterceptor: HttpLoggingInterceptor,
    authInterceptor: AuthInterceptor  // 추가
): OkHttpClient =
    OkHttpClient
        .Builder()
        .addInterceptor(authInterceptor)  // 로깅 전에 추가 (순서 중요)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

@Provides
@Singleton
fun provideAuthApi(
    @Named("walkit") retrofit: Retrofit  // 수정: walkit 사용
): AuthApi = retrofit.create(AuthApi::class.java)
```

#### 2.2 `LoginViewModel.kt` 수정
**변경 사항**:
- `AuthRemoteDataSource` 주입
- `AuthDataStore` 주입
- 카카오/네이버 로그인 성공 후 서버 API 호출 추가
- 서버 응답 토큰을 `AuthDataStore`에 저장

**수정 내용**:
```kotlin
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource,  // 추가
    private val authDataStore: AuthDataStore  // 추가
) : ViewModel() {
    
    // 카카오 로그인 성공 후
    fun loginWithKakaoTalk(context: Context) {
        // ... 기존 코드 ...
        } else if (token != null) {
            Timber.i("카카오톡으로 로그인 성공 ${token.accessToken}")
            // 서버에 토큰 전송
            sendTokenToServer(token.accessToken, isKakao = true)
        }
    }
    
    // 네이버 로그인 성공 후
    fun handleNaverLoginResult(result: ActivityResult) {
        // ... 기존 코드 ...
        if (accessToken != null) {
            Timber.i("네이버 로그인 성공: $accessToken")
            // 서버에 토큰 전송
            sendTokenToServer(accessToken, isKakao = false)
        }
    }
    
    private fun sendTokenToServer(socialAccessToken: String, isKakao: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.value = LoginUiState.Loading
                
                val result = if (isKakao) {
                    authRemoteDataSource.loginWithKakao(socialAccessToken)
                } else {
                    authRemoteDataSource.loginWithNaver(socialAccessToken)
                }
                
                when (result) {
                    is Result.Success -> {
                        val tokenResponse = result.data
                        // 서버 토큰 저장
                        authDataStore.saveTokens(
                            accessToken = tokenResponse.accessToken,
                            refreshToken = tokenResponse.refreshToken
                        )
                        _isLoggedIn.value = true
                        _uiState.value = LoginUiState.Idle
                        Timber.i("서버 로그인 성공")
                    }
                    is Result.Error -> {
                        _uiState.value = LoginUiState.Error("서버 로그인 실패: ${result.exception.message}")
                        Timber.e(result.exception, "서버 로그인 실패")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("로그인 처리 중 오류 발생: ${e.message}")
                Timber.e(e, "로그인 처리 실패")
            }
        }
    }
}
```

#### 2.3 `AuthInterceptor.kt` 개선 (토큰 동기 접근 문제 해결)
**문제**: `runBlocking` 사용은 권장되지 않음

**해결책**: `suspendCoroutine` 또는 `Flow.first()` 사용

**최종 구현**:
```kotlin
@Singleton
class AuthInterceptor @Inject constructor(
    private val authDataStore: AuthDataStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // AuthApi 호출은 토큰 불필요 (로그인 API 자체)
        if (request.url.encodedPath.contains("/auth/")) {
            return chain.proceed(request)
        }
        
        // 토큰 가져오기 (동기적으로)
        val token = runBlocking {
            authDataStore.accessToken.first()
        }
        
        val newRequest = if (!token.isNullOrBlank()) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }
        
        return chain.proceed(newRequest)
    }
}
```

**또는 더 나은 방법**: `TokenProvider` 인터페이스 사용
```kotlin
interface TokenProvider {
    fun getToken(): String?
}

@Singleton
class TokenProviderImpl @Inject constructor(
    private val authDataStore: AuthDataStore
) : TokenProvider {
    private var cachedToken: String? = null
    
    init {
        // Flow를 구독하여 토큰 캐싱
        CoroutineScope(Dispatchers.IO).launch {
            authDataStore.accessToken.collect { token ->
                cachedToken = token
            }
        }
    }
    
    override fun getToken(): String? = cachedToken
}
```

#### 2.4 `AuthDataStore.kt` 수정 (선택적)
**변경 사항**: 토큰 동기 접근을 위한 캐싱 추가 (선택적)

### 3. 추가 고려사항

#### 3.1 토큰 갱신 로직 (향후 구현)
- `AuthInterceptor`에서 401 응답 시 토큰 갱신 시도
- `RefreshTokenInterceptor` 추가 고려

#### 3.2 에러 처리
- 서버 로그인 실패 시 사용자에게 알림
- 네트워크 오류 처리

#### 3.3 로그아웃 처리
- `LoginViewModel.logout()`에서 `AuthDataStore.clear()` 호출 추가

## 구현 순서

1. ✅ **AuthInterceptor 생성**
   - 토큰을 헤더에 추가하는 인터셉터 구현
   - AuthApi 호출은 제외 처리

2. ✅ **AuthRemoteDataSource 생성**
   - AuthApi 호출 래핑
   - 에러 처리

3. ✅ **NetworkModule 수정**
   - AuthInterceptor를 OkHttpClient에 추가
   - AuthApi 제공 함수 수정

4. ✅ **LoginViewModel 수정**
   - AuthRemoteDataSource, AuthDataStore 주입
   - 소셜 로그인 성공 후 서버 API 호출
   - 서버 토큰 저장

5. ✅ **AuthDataStore 확인**
   - 토큰 저장/조회 로직 확인
   - 필요시 개선

6. ✅ **테스트**
   - 카카오 로그인 플로우 테스트
   - 네이버 로그인 플로우 테스트
   - API 요청 헤더 확인

## 파일 구조

```
app/src/main/java/team/swyp/sdu/
├── data/
│   ├── api/
│   │   └── auth/
│   │       ├── AuthApi.kt (기존)
│   │       ├── SocialLoginRequest.kt (기존)
│   │       └── AuthTokenResponse.kt (기존)
│   ├── local/
│   │   └── datastore/
│   │       └── AuthDataStore.kt (기존)
│   └── remote/
│       ├── auth/
│       │   └── AuthRemoteDataSource.kt (신규 생성)
│       └── interceptor/
│           └── AuthInterceptor.kt (신규 생성)
├── di/
│   └── NetworkModule.kt (수정)
└── presentation/
    └── viewmodel/
        └── LoginViewModel.kt (수정)
```

## 주의사항

1. **인터셉터 순서**: AuthInterceptor는 로깅 인터셉터보다 먼저 추가
2. **토큰 동기 접근**: OkHttp Interceptor는 동기 함수이므로 `runBlocking` 사용 불가피 (또는 TokenProvider 패턴 사용)
3. **AuthApi 제외**: 로그인 API 자체는 토큰 불필요하므로 제외 처리
4. **에러 처리**: 서버 로그인 실패 시 적절한 에러 메시지 표시
5. **토큰 갱신**: 향후 refresh token을 사용한 자동 갱신 로직 추가 고려








