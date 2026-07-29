# WalkIt AOS Project Guidelines

이 파일은 클로드 코드(Claude Code)의 프로젝트 메인 가이드입니다.

## 🛠 핵심 명령 (Core Commands)
- **빌드**: `./gradlew assembleDebug`
- **테스트**: `./gradlew test`
- **린트**: `./gradlew lint`
- **클린**: `./gradlew clean`

## 📖 상세 개발 규칙 (Detailed Rules)
상황에 맞는 상세 규칙은 `.claude/rules/` 디렉토리의 파일들을 참조하십시오:
- **안드로이드 API 35**: [android-api-35.md](@.claude/rules/android-api-35.md)
- **UI & ViewModel**: [ui-viewmodel.md](@.claude/rules/ui-viewmodel.md)
- **코루틴**: [coroutines.md](@.claude/rules/coroutines.md)
- **API 바인딩**: [api-binding.md](@.claude/rules/api-binding.md)
- **에러 처리**: [result-handling.md](@.claude/rules/result-handling.md)
- **동기화 전략**: [sync-strategy.md](@.claude/rules/sync-strategy.md)
- **성능 최적화**: [performance.md](@.claude/rules/performance.md)
- **보안 원칙**: [security.md](@.claude/rules/security.md)
- **의존성 관리**: [dependency-management.md](@.claude/rules/dependency-management.md)
- **프로젝트 구조**: [project-structure.md](@.claude/rules/project-structure.md)
- **Compose 가이드**: [android-composer.md](@.claude/rules/android-composer.md)

## 🚀 아키텍처 요약
- **MVVM + Clean Architecture**
- **Tech Stack**: Kotlin 2.0, Compose, Hilt, Retrofit, Room, Coroutines/Flow
- **Target API**: 35 (Android 15)

---
*모든 코드는 한국어 주석과 KDoc 형식을 준수해야 합니다.*
