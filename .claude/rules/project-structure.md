# Project Structure Guide

## 1. 레이어 구조 (Clean Architecture)
```
app/
├── domain/ (Pure Kotlin)
│   ├── model/
│   ├── repository/
│   └── usecase/
├── data/ (Implementation)
│   ├── remote/
│   ├── local/
│   └── repository/
└── presentation/ (UI/Compose)
    ├── ui/
    ├── viewmodel/
    └── navigation/
```

## 2. 모듈화 규칙
- 기능별 모듈 분리 권장.
- 공통 유틸리티는 `core` 모듈로 관리.
- UI 컴포넌트는 `design-system` 모듈 활용.

## 3. 리소스 관리
- 문자열은 `strings.xml`.
- 색상은 `colors.xml` 또는 Compose Theme 객체.
- 이미지는 `drawable/` 또는 `mipmap/`.
- 테마 설정은 `Theme.kt` 및 `theme.xml`.
