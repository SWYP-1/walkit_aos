# API and DTO Binding Rules

## 1. DTO (Data Transfer Object) 작성
- **파일명**: `{Domain}Data.kt` 또는 `{Domain}ListData.kt`
- **규칙**:
  - 서버 필드: `snake_case`
  - 클라이언트 필드: `camelCase` (반드시 매핑 필요)
  - Boolean: `is`, `has` 접두사 사용.
  - Y/N 값: String 타입으로 처리 ("Y"/"N").

### 필수 구조
```kotlin
@Parcelize
data class ExampleData(
    @SerializedName("server_field")
    val clientField: Type? = defaultValue
) : Parcelable
```
- **@SerializedName**: 모든 API 응답 필드에 **필수** 적용.
- **@Parcelize**: 안드로이드 컴포넌트 간 데이터 전달을 위해 **필수** 적용.
- **Nullability**: 필수 필드는 Non-null, 선택적 필드는 Nullable과 기본값 설정.

## 2. API 인터페이스
- 기존에 선언된 API 인터페이스가 있다면 해당 파일에 함수 추가.
- 적절한 인터페이스가 없을 때만 신규 생성.
- HTTP 메소드 및 엔드포인트는 API 문서와 정확히 일치해야 함.

## 3. Repository 및 ViewModel 통합
- Repository 레이어에서 `Result` 타입을 사용하여 데이터를 래핑.
- ViewModel은 Repository를 호출하여 데이터를 저장하고 UI 상태를 업데이트.
- 기존 ViewModel이 비슷한 기능을 수행한다면 재사용 권장.

## 4. 제약 사항
- Import 시 기존 프로젝트에서 사용 중인 버전을 확인하십시오.
- 에러 핸들링과 로딩 상태 처리를 반드시 포함하십시오.
- 복잡한 비즈니스 로직이 포함된 경우 DTO 또는 Repository에 주석(KDoc)을 작성하십시오.
