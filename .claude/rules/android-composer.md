# Android Jetpack Compose Entry Point

이 문서는 프로젝트 전체의 Jetpack Compose 도입 및 구조에 대한 가이드입니다.

## 1. 기본 원칙
- 기존 프로젝트 아키텍처와의 일관성 유지.
- Material Design 3(M3) 가이드라인 및 컴포넌트 준수.
- Unidirectional Data Flow(UDF)를 통한 상태 관리.

## 2. 구현 가이드라인
- 비동기 작업은 Coroutine/Flow 사용.
- 의존성 주입은 Hilt 사용.
- 화면 전환은 Compose Navigation 사용.

## 3. 테스트 가이드라인
- ViewModel 및 UseCase에 대한 유닛 테스트 작성.
- Compose Testing Framework을 사용한 UI 테스트 수행.
- 테스트 시 가짜 저장소(Fake Repository) 활용.
