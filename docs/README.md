# 산책 추적 시스템 아키텍처 문서

이 문서는 산책 추적 앱의 전체 시스템 아키텍처와 각 컴포넌트의 동작 원리를 설명합니다.

## 📚 문서 목차

1. [전체 아키텍처 개요](./01-architecture-overview.md)
   - 시스템 전체 구조
   - 주요 컴포넌트 소개
   - 컴포넌트 간 관계

2. [데이터 흐름도](./02-data-flow.md)
   - 산책 시작부터 종료까지의 전체 흐름
   - 각 단계별 상세 설명

3. [컴포넌트 상세 설명](./03-components.md)
   - WalkingViewModel
   - ActivityRecognitionManager
   - StepCounterManager
   - AccelerometerManager
   - LocationTrackingService
   - WalkingScreen (UI)
   - WalkingResultScreen (UI)
   - LocationTestData (Utils)
   - WalkingSessionRepository (Data)

4. [타이밍 다이어그램](./04-timing-diagram.md)
   - 활동 상태 변경 시 지연 시간 분석
   - 위치 업데이트 타이밍
   - 실시간 반영 과정

5. [FAQ](./05-faq.md)
   - 자주 묻는 질문과 답변

6. [배터리 최적화](./06-battery-optimization.md)
   - 배터리 최적화 전략
   - 경쟁 앱 비교 (Strava, Nike Run Club)
   - 구현된 최적화 기능

## 🎯 이 문서를 읽기 전에

- **Android 기본 지식**: Activity, Service, BroadcastReceiver 개념 이해
- **Kotlin Coroutines**: Flow, StateFlow 기본 개념
- **Android Jetpack Compose**: 기본 UI 구성 개념

## 🚀 빠른 시작

1. 먼저 [전체 아키텍처 개요](./01-architecture-overview.md)를 읽어 전체 구조를 파악하세요.
2. [데이터 흐름도](./02-data-flow.md)로 실제 동작 과정을 이해하세요.
3. 각 컴포넌트의 상세 설명을 필요에 따라 참고하세요.
