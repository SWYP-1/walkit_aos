# Test Utilities / 테스트 유틸리티

이 디렉토리에는 테스트에서 공통으로 사용할 수 있는 유틸리티 클래스들이 있습니다.

## JsonTestUtil

JSON 파일 로드 및 파싱을 위한 테스트 유틸리티 클래스입니다.

### 주요 기능

#### 1. Raw 리소스 JSON 파일 로드
```kotlin
// temp.json 파일 로드 (기본)
val locations = JsonTestUtil.loadLocationsFromTempJson()

// 다른 JSON 파일 로드
val customData = JsonTestUtil.loadLocationsFromJson("my_data")
```

#### 2. JSON 문자열 파싱
```kotlin
val jsonString = """[{"latitude":37.123,"longitude":127.123,"timestamp":1640995200000}]"""
val locations = JsonTestUtil.parseLocationsFromJsonString(jsonString)
```

#### 3. 데이터 유효성 검증
```kotlin
val locations = JsonTestUtil.loadLocationsFromTempJson()
val validationResult = JsonTestUtil.validateLocations(locations)
// 결과: "✅ 위치 데이터 검증 통과 (150개)" 또는 에러 메시지
```

### 사용 예시

#### CharacterShopViewModelTest
```kotlin
@Test
fun `temp.json에서 위치 데이터 로드 테스트`() {
    val locations = JsonTestUtil.loadLocationsFromTempJson()

    assertTrue("데이터가 있어야 함", locations.isNotEmpty())

    // 검증 함수 사용
    val result = JsonTestUtil.validateLocations(locations)
    println("검증 결과: $result")
}
```

#### WalkingViewModelTest
```kotlin
@Test
fun `위치 데이터 검증 및 활용 테스트`() {
    val locations = JsonTestUtil.loadLocationsFromTempJson()

    // WalkingViewModel 로직 검증에 활용
    locations.forEach { location ->
        assertTrue("유효한 GPS 데이터여야 함", location.latitude != 0.0)
    }
}
```

### 지원하는 데이터 타입

- `List<LocationPoint>` - 위치 데이터 리스트

### 파일 위치

- Raw 리소스: `app/src/main/res/raw/`
- 지원 형식: `temp.json`, `custom.json` 등

### 에러 처리

- 존재하지 않는 파일: 빈 리스트 반환 + 로그 출력
- JSON 파싱 실패: 빈 리스트 반환 + 에러 로그 출력
- 네트워크/파일 접근 실패: 빈 리스트 반환 + 스택트레이스 출력

### 확장 가능성

새로운 데이터 타입 지원을 위해서는:
1. `JsonTestUtil`에 새로운 함수 추가
2. 해당 데이터 클래스가 `@Serializable` 어노테이션 적용 확인
3. 적절한 Json 설정 사용

```kotlin
// 새로운 데이터 타입 지원 예시
inline fun <reified T> loadCustomData(fileName: String): List<T> {
    // ... 유사한 로직
    return json.decodeFromString<List<T>>(jsonString)
}
```


