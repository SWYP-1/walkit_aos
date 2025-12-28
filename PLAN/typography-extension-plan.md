# Typography 확장 계획: Heading XL, Body XL 추가

## 현재 상황

### 문제점
- Material3 Typography는 고정된 속성만 제공 (displayLarge, bodyLarge 등)
- `Heading XL` (32sp)과 `Body XL` (20sp)을 별도 속성으로 접근 불가
- 현재는 `displayLarge`에 HeadingXL, `bodyLarge`에 BodyXL을 매핑하고 있음

### 현재 구조
```kotlin
// WalkItTypography.kt
val WalkItTypography = Typography(
    displayLarge = TextStyle(...) // HeadingXL (32sp) 사용
    bodyLarge = TextStyle(...)   // BodyXL (20sp) 사용
)

// 사용 예시
MaterialTheme.typography.displayLarge  // HeadingXL
MaterialTheme.typography.bodyLarge     // BodyXL
```

## 확장 방안

### 방안 1: CompositionLocal 사용 (권장) ⭐

**장점:**
- Material3와 완전 호환
- 타입 안전성 보장
- MaterialTheme.typography와 동일한 방식으로 접근 가능
- 기존 코드 변경 최소화

**구현:**
```kotlin
// 1. 커스텀 Typography 데이터 클래스 생성
data class WalkItTypography(
    val headingXL: TextStyle,
    val headingL: TextStyle,
    val headingM: TextStyle,
    val headingS: TextStyle,
    val bodyXL: TextStyle,
    val bodyL: TextStyle,
    val bodyM: TextStyle,
    val bodyS: TextStyle,
    val captionM: TextStyle,
)

// 2. CompositionLocal 생성
val LocalWalkItTypography = compositionLocalOf<WalkItTypography> {
    error("WalkItTypography not provided")
}

// 3. MaterialTheme 확장
val MaterialTheme.walkItTypography: WalkItTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalWalkItTypography.current

// 4. Theme에서 제공
@Composable
fun WalkItTheme(content: @Composable () -> Unit) {
    val walkItTypography = WalkItTypography(...)
    
    CompositionLocalProvider(
        LocalWalkItTypography provides walkItTypography
    ) {
        MaterialTheme(
            typography = Material3Typography(walkItTypography),
            content = content
        )
    }
}
```

**사용 예시:**
```kotlin
Text(
    text = "제목",
    style = MaterialTheme.walkItTypography.headingXL
)

Text(
    text = "본문",
    style = MaterialTheme.walkItTypography.bodyXL
)
```

---

### 방안 2: Extension Property만 사용 (간단)

**장점:**
- 구현이 매우 간단
- 기존 코드와 완전 호환

**단점:**
- Material3 Typography와 분리되어 일관성 부족
- MaterialTheme.typography와 별도로 관리

**구현:**
```kotlin
// WalkItTypography.kt에 추가
object WalkItTypography {
    val headingXL = TextStyle(...)
    val headingL = TextStyle(...)
    val bodyXL = TextStyle(...)
    // ...
}

// MaterialTheme 확장
val MaterialTheme.walkItTypography: WalkItTypography
    @Composable
    @ReadOnlyComposable
    get() = WalkItTypography
```

**사용 예시:**
```kotlin
Text(
    text = "제목",
    style = MaterialTheme.walkItTypography.headingXL
)
```

---

### 방안 3: Material3 Typography 확장 (하이브리드)

**장점:**
- Material3 Typography를 그대로 사용하면서 확장
- 기존 MaterialTheme.typography와 함께 사용 가능

**구현:**
```kotlin
// 커스텀 Typography 확장
val Typography.headingXL: TextStyle
    get() = displayLarge  // 또는 별도 정의

val Typography.bodyXL: TextStyle
    get() = bodyLarge  // 또는 별도 정의
```

**사용 예시:**
```kotlin
Text(
    text = "제목",
    style = MaterialTheme.typography.headingXL
)
```

---

## 권장 방안: 방안 1 (CompositionLocal)

### 이유
1. **타입 안전성**: 컴파일 타임에 속성 존재 보장
2. **일관성**: MaterialTheme 패턴과 동일한 사용법
3. **확장성**: 향후 추가 속성 확장 용이
4. **테스트 용이**: CompositionLocal을 mock 가능

### 구현 단계

#### 1단계: 커스텀 Typography 데이터 클래스 생성
```kotlin
// ui/theme/WalkItTypography.kt
data class WalkItTypography(
    val headingXL: TextStyle,
    val headingL: TextStyle,
    val headingM: TextStyle,
    val headingS: TextStyle,
    val bodyXL: TextStyle,
    val bodyL: TextStyle,
    val bodyM: TextStyle,
    val bodyS: TextStyle,
    val captionM: TextStyle,
)
```

#### 2단계: CompositionLocal 생성
```kotlin
// ui/theme/Theme.kt
val LocalWalkItTypography = compositionLocalOf<WalkItTypography> {
    error("WalkItTypography not provided")
}
```

#### 3단계: MaterialTheme 확장
```kotlin
// ui/theme/Theme.kt
val MaterialTheme.walkItTypography: WalkItTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalWalkItTypography.current
```

#### 4단계: Theme에서 제공
```kotlin
@Composable
fun WalkItTheme(content: @Composable () -> Unit) {
    val walkItTypography = WalkItTypography(
        headingXL = TextStyle(...),
        headingL = TextStyle(...),
        // ...
    )
    
    CompositionLocalProvider(
        LocalWalkItTypography provides walkItTypography
    ) {
        MaterialTheme(
            typography = createMaterial3Typography(walkItTypography),
            content = content
        )
    }
}

// Material3 Typography 생성 헬퍼
private fun createMaterial3Typography(
    walkIt: WalkItTypography
): Typography {
    return Typography(
        displayLarge = walkIt.headingXL,
        displayMedium = walkIt.headingL,
        displaySmall = walkIt.headingM,
        headlineSmall = walkIt.headingS,
        bodyLarge = walkIt.bodyXL,
        bodyMedium = walkIt.bodyL,
        bodySmall = walkIt.bodyM,
        labelSmall = walkIt.captionM,
    )
}
```

#### 5단계: 기존 코드 마이그레이션 (선택사항)
```kotlin
// 기존
MaterialTheme.typography.displayLarge

// 새로운 방식 (둘 다 가능)
MaterialTheme.typography.displayLarge  // 여전히 작동
MaterialTheme.walkItTypography.headingXL  // 새로운 방식
```

---

## 마이그레이션 전략

### 단계별 접근
1. **Phase 1**: 새로운 Typography 시스템 구축 (기존 코드 유지)
2. **Phase 2**: 새 코드는 `walkItTypography` 사용
3. **Phase 3**: 기존 코드 점진적 마이그레이션 (선택사항)

### 호환성 보장
- 기존 `MaterialTheme.typography.*` 코드는 계속 작동
- 새로운 `MaterialTheme.walkItTypography.*` 사용 가능
- 두 방식 모두 지원하여 점진적 마이그레이션 가능

---

## 파일 구조

```
ui/theme/
├── Font.kt              # 폰트 정의 (기존)
├── TypeScale.kt         # 폰트 크기 정의 (기존)
├── WalkItTypography.kt # 커스텀 Typography 데이터 클래스 (수정)
└── Theme.kt            # CompositionLocal 및 Theme 정의 (수정)
```

---

## 예상 작업 시간
- 구현: 1-2시간
- 테스트: 30분
- 문서화: 30분
- **총: 2-3시간**

---

## 체크리스트

- [ ] WalkItTypography 데이터 클래스 생성
- [ ] CompositionLocal 생성
- [ ] MaterialTheme 확장 함수 추가
- [ ] Theme에서 CompositionLocal 제공
- [ ] Material3 Typography 매핑 함수 생성
- [ ] 기존 코드 테스트
- [ ] 새 Typography 사용 예시 추가
- [ ] 문서 업데이트








