# WalkIt Typography 사용 예시

## 새로운 Typography 시스템 사용법

### 기본 사용법

```kotlin
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

@Composable
fun ExampleScreen() {
    // Heading XL 사용
    Text(
        text = "큰 제목",
        style = MaterialTheme.walkItTypography.headingXL
    )
    
    // Body XL 사용
    Text(
        text = "큰 본문 텍스트",
        style = MaterialTheme.walkItTypography.bodyXL
    )
}
```

### 사용 가능한 Typography 속성

```kotlin
MaterialTheme.walkItTypography.headingXL  // 32sp, SemiBold
MaterialTheme.walkItTypography.headingL  // 28sp, SemiBold
MaterialTheme.walkItTypography.headingM   // 24sp, Medium
MaterialTheme.walkItTypography.headingS   // 22sp, Medium
MaterialTheme.walkItTypography.bodyXL     // 20sp, Medium
MaterialTheme.walkItTypography.bodyL      // 18sp, Normal
MaterialTheme.walkItTypography.bodyM      // 16sp, Normal
MaterialTheme.walkItTypography.bodyS      // 14sp, Normal
MaterialTheme.walkItTypography.captionM   // 12sp, Normal
```

### 기존 Material3 Typography와의 호환성

기존 코드는 그대로 작동합니다:

```kotlin
// 기존 방식 (여전히 작동)
Text(
    text = "제목",
    style = MaterialTheme.typography.displayLarge  // headingXL과 동일
)

// 새로운 방식
Text(
    text = "제목",
    style = MaterialTheme.walkItTypography.headingXL  // 더 명확함
)
```

### 실제 사용 예시

```kotlin
@Composable
fun MyScreen() {
    Column {
        // 큰 제목
        Text(
            text = "마이 페이지",
            style = MaterialTheme.walkItTypography.headingXL
        )
        
        // 중간 제목
        Text(
            text = "설정",
            style = MaterialTheme.walkItTypography.headingM
        )
        
        // 큰 본문
        Text(
            text = "이것은 큰 본문 텍스트입니다.",
            style = MaterialTheme.walkItTypography.bodyXL
        )
        
        // 일반 본문
        Text(
            text = "이것은 일반 본문 텍스트입니다.",
            style = MaterialTheme.walkItTypography.bodyM
        )
        
        // 작은 설명
        Text(
            text = "작은 설명 텍스트",
            style = MaterialTheme.walkItTypography.captionM
        )
    }
}
```

## 마이그레이션 가이드

### 기존 코드
```kotlin
Text(
    text = "제목",
    style = MaterialTheme.typography.displayLarge
)
```

### 권장: 새로운 방식 사용
```kotlin
Text(
    text = "제목",
    style = MaterialTheme.walkItTypography.headingXL
)
```

### 장점
- 더 명확한 의미: `headingXL`이 `displayLarge`보다 직관적
- 타입 안전성: 컴파일 타임에 속성 존재 보장
- 일관성: MaterialTheme 패턴과 동일한 사용법






