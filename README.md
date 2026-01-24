# 🏃‍♂️ Walkit - 스마트 산책 추적 앱

<div align="center">

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android](https://img.shields.io/badge/Android-15.0-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5.8-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com/)

**산책 감정 기록 애플리케이션**

[📱 다운로드](#-설치-및-실행) • [📚 문서](docs/README.md) • [🚀 데모](#-주요-기능)

</div>

---

## 🌟 프로젝트 소개

**Walkit**은 감정 중심의 스마트 산책 추적 애플리케이션입니다. 단순한 산책 기록을 넘어, 산책 전후의 감정 변화를 추적하고 개인화된 산책 경험을 제공합니다. GPS, 가속도계, 걸음 수 센서, 활동 인식 등 다양한 센서를 활용하여 정확한 산책 데이터를 수집하며, 캐릭터 시스템과 소셜 기능을 통해 건강한 산책 습관을 형성합니다.

### 🎯 핵심 가치

- **💝 감정 중심 헬스케어**: 산책 전후 감정 기록으로 정신 건강 관리
- **🎮 게임화된 경험**: 캐릭터 수집과 미션 시스템으로 동기 부여
- **👥 소셜 연결**: 친구들과의 기록 공유로 건강한 경쟁과 응원 문화
- **📍 정확한 추적**: GPS + 센서 융합으로 정밀한 산책 데이터 수집
- **⚡ 실시간 피드백**: 1초 간격 활동 감지로 즉각적인 사용자 경험
- **🎨 현대적인 UI/UX**: Material Design 3 기반 직관적인 인터페이스

---

## 🚀 주요 기능

### 🌟 감정 기반 산책 추적
- **산책 전 감정 기록**: 산책 시작 전 현재 감정 상태 선택
- **실시간 GPS 추적**: 고정밀 GPS 기반 경로 기록
- **센서 융합 추적**: GPS + 걸음 센서 + 가속도계 결합
- **산책 후 감정 기록**: 산책 완료 후 변화된 감정 상태 기록
- **감정 변화 분석**: 산책 전후 감정 변화를 시각적으로 확인

### 🎮 캐릭터 시스템
- **캐릭터 상점**: 다양한 캐릭터 구매 및 수집
- **드레스룸**: 캐릭터 의상 및 액세서리 커스터마이징
- **캐릭터 카테고리**: 테마별 캐릭터 분류
- **아이템 상점**: 의상, 액세서리 등 다양한 아이템 구매

### 👥 소셜 기능
- **친구 검색**: 닉네임으로 친구 찾기
- **팔로우 시스템**: 친구 팔로우 및 언팔로우
- **친구 기록 공유**: 친구들의 산책 기록 확인 및 응원
- **친구 프로필**: 친구들의 산책 통계 및 캐릭터 확인

### 🎯 미션 시스템
- **일일 미션**: 매일 새로운 미션 도전
- **카테고리별 미션**: 산책, 운동, 생활습관 등 다양한 카테고리
- **인기 미션**: 다른 사용자들이 많이 참여하는 미션 추천
- **미션 달성 보상**: 미션 완료 시 포인트 및 아이템 획득

### 📊 기록 및 통계
- **일일 산책 기록**: 날짜별 산책 기록 조회
- **산책 경로 지도**: 카카오맵 기반 경로 시각화
- **감정 변화 그래프**: 산책 전후 감정 변화 추이
- **통계 대시보드**: 주간/월간 산책 통계 및 분석

### 🎯 목표 관리
- **걸음 목표 설정**: 일일 걸음 목표 설정 및 추적
- **목표 달성률**: 목표 달성률 시각화
- **목표 달성 알림**: 목표 달성 시 축하 알림
- **목표 히스토리**: 목표 설정 및 달성 이력 관리

---

## 🛠️ 기술 스택

### Core Framework
- **Language**: Kotlin 1.9.22
- **UI Framework**: Jetpack Compose 1.5.8
- **Architecture**: MVVM + Clean Architecture
- **Dependency Injection**: Hilt 2.48

### Android Platform
- **Target SDK**: Android 15 (API 35)
- **Min SDK**: Android 7.0 (API 24)
- **Build System**: Gradle Kotlin DSL

### Data & Storage
- **Database**: Room 2.6.1
- **Async Programming**: Kotlin Coroutines 1.7.3 + Flow
- **Serialization**: Kotlin Serialization 1.6.3

### Sensors & Location
- **Location Services**: Google Play Services Location
- **Activity Recognition**: Google Play Services Activity Recognition
- **Sensors**: Android Sensor Framework (Accelerometer, Step Counter)

### Analytics & Monitoring
- **Crash Reporting**: Firebase Crashlytics
- **Analytics**: Firebase Analytics

### Development Tools
- **Linting**: Ktlint
- **Code Generation**: Kotlin Symbol Processing (KSP)
- **Testing**: JUnit 4 + Mockito

---

## 📱 설치 및 실행

### 사전 요구사항
- **Android Studio**: Iguana | 2023.2.1 이상
- **JDK**: 17 이상
- **Android SDK**: API 35 (Android 15)
- **Google Play Services**: 최신 버전

---

## 🏗️ 아키텍처

```
📱 Presentation Layer (Jetpack Compose)
    ├── 🏠 Home (메인 대시보드)
    ├── 🚶 Walking (산책 추적)
    │   ├── PreWalkEmotionSelect (산책 전 감정 선택)
    │   ├── WalkingScreen (실시간 산책)
    │   ├── PostWalkEmotionSelect (산책 후 감정 선택)
    │   └── WalkingResultScreen (산책 결과)
    ├── 👥 Friends (친구 목록/검색)
    ├── 📊 Record (기록 조회)
    │   ├── DailyRecord (일일 기록)
    │   └── FriendRecord (친구 기록)
    ├── 🎮 Character (캐릭터 시스템)
    │   ├── CharacterShop (캐릭터 상점)
    │   └── DressingRoom (드레스룸)
    ├── 🎯 Mission (미션 시스템)
    └── 👤 MyPage (개인 설정)

🔄 Domain Layer (Pure Kotlin)
    ├── 📋 Use Cases (비즈니스 로직)
    ├── 🏷️ Models (데이터 모델)
    ├── 📚 Repositories (인터페이스)
    └── 🧮 Calculator (거리/통계 계산)

💾 Data Layer (Android Framework)
    ├── 🗄️ Room Database (로컬 저장소)
    ├── 🌐 REST APIs (서버 통신)
    ├── 📡 Sensor Managers (하드웨어 센서)
    └── 🔄 Sync Workers (오프라인 동기화)
```

### 주요 컴포넌트

| 컴포넌트 | 역할 | 기술 |
|---------|------|------|
| `WalkingViewModel` | 산책 상태 및 감정 관리 | StateFlow + Shared ViewModel |
| `UserViewModel` | 사용자 정보 및 인증 | Hilt + LiveData |
| `FriendViewModel` | 친구 관리 및 검색 | StateFlow + Coroutines |
| `CharacterShopViewModel` | 캐릭터 구매 및 관리 | StateFlow + Room |
| `MissionViewModel` | 미션 진행 및 보상 | StateFlow + API |
| `RecordViewModel` | 기록 조회 및 통계 | StateFlow + Room |

---

## 📊 성능 최적화

### 배터리 효율성
- **활동 인식 최적화**: 1초 간격 실시간 감지
- **GPS 배터리 관리**: 정확도 vs 배터리 균형
- **백그라운드 제한**: Android 15 정책 준수

### 메모리 관리
- **Flow 최적화**: StateFlow + collectAsStateWithLifecycle
- **이미지 최적화**: WebP 변환 자동화
- **데이터베이스 최적화**: 효율적인 쿼리 설계

---

## 📚 문서

프로젝트의 자세한 아키텍처와 구현 방법을 확인하려면 [docs](./docs/) 디렉토리를 참고하세요:

- [🏗️ 전체 아키텍처 개요](./docs/01-architecture-overview.md)
- [🔄 데이터 흐름도](./docs/02-data-flow.md)
- [⚙️ 컴포넌트 상세 설명](./docs/03-components.md)
- [⏱️ 타이밍 다이어그램](./docs/04-timing-diagram.md)
- [❓ FAQ](./docs/05-faq.md)
- [🔋 배터리 최적화](./docs/06-battery-optimization.md)

---

## 🤝 기여 방법

### 개발 환경 설정
1. 이 리포지토리를 포크하세요
2. 기능 브랜치를 생성하세요 (`git checkout -b feature/AmazingFeature`)
3. 변경사항을 커밋하세요 (`git commit -m 'Add some AmazingFeature'`)
4. 브랜치에 푸시하세요 (`git push origin feature/AmazingFeature`)
5. Pull Request를 생성하세요

### 코드 스타일
- **Ktlint**: 코드 포맷팅 및 린팅
- **Korean Comments**: 한글 주석 사용
- **MVVM Pattern**: ViewModel을 통한 비즈니스 로직 분리
- **Clean Architecture**: 계층별 책임 분리

### 테스트
- **Unit Tests**: 모든 ViewModel 및 UseCase
- **UI Tests**: 주요 화면 컴포넌트
- **Integration Tests**: 데이터 흐름 검증

---

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참고하세요.

---

## 👥 팀 소개

**SWYP 3기 Team 1** - Android 개발팀


---
