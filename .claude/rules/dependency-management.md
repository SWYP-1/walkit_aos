# Dependency Management Guide

## 1. 버전 관리 (Version Catalog)
- `libs.versions.toml`을 사용하여 모든 라이브러리 버전을 통합 관리하십시오.
- 정기적인 종속성 업데이트를 수행하되, 호환성 테스트를 반드시 병행하십시오.

## 2. 핵심 라이브러리 스펙
- **Kotlin**: 2.0.0+
- **Compose**: 1.5.0+
- **Coroutines**: 1.7.0+
- **Hilt**: 2.48+

## 3. 모듈 의존성 규칙
- 순환 참조(Circular Dependency) 금지.
- 의존성 주입은 Hilt를 기본으로 사용.
- 테스트용 의존성은 `testImplementation`, `androidTestImplementation`에 격리.

## 4. 보안 설정
- ProGuard/R8 규칙을 적용하여 코드 난독화 수행.
- 민감한 설정은 `BuildConfig`를 통해 관리하고 소스 코드 노출 방지.
