# Google Play Billing 초기화 패턴 비교

## 현재 구현 패턴

### 구조
```
Application (EntryPoint로 BillingManager 초기화)
  └─> BillingManager.initialize()
  
ViewModel (BillingManager 직접 주입)
  └─> billingManager.setPurchasesUpdatedListener()
  └─> billingManager.launchPurchaseFlow()
```

### 장점
- ✅ Application에서 초기화하여 앱 시작 시 바로 연결
- ✅ EntryPoint는 Hilt의 표준 패턴
- ✅ 작동은 정상적으로 함

### 단점
- ❌ ViewModel이 BillingManager를 직접 의존 (Repository 패턴 위반)
- ❌ 리스너 관리가 ViewModel에 있음 (책임 분리 부족)
- ❌ EntryPoint 사용이 복잡해 보일 수 있음

---

## 권장 패턴 (상업 앱에서 많이 사용)

### 구조
```
Application (EntryPoint로 BillingManager 초기화)
  └─> BillingManager.initialize()
  
Repository (BillingManager 캡슐화)
  └─> setPurchasesUpdatedListener() - 콜백을 Repository에서 관리
  └─> startPurchaseFlow() - 구매 흐름 시작
  
ViewModel (Repository만 주입)
  └─> repository.startPurchaseFlow()
  └─> repository의 Flow를 구독하여 상태 업데이트
```

### 장점
- ✅ Repository 패턴 준수 (ViewModel은 Repository만 의존)
- ✅ 책임 분리 명확 (구매 로직은 Repository에)
- ✅ 테스트 용이 (Repository만 Mock하면 됨)
- ✅ 다른 ViewModel에서도 재사용 가능

### 단점
- ⚠️ Repository 코드가 조금 더 복잡해짐

---

## 실제 상업 앱 사례

### 1. 대부분의 앱: Application에서 초기화
```kotlin
// Application.kt
override fun onCreate() {
    super.onCreate()
    billingManager.initialize() // 또는 EntryPoint 사용
}
```

### 2. Repository 패턴 사용 (권장)
- ViewModel은 Repository만 주입받음
- BillingManager는 Repository 내부에서만 사용
- 구매 이벤트는 Flow로 노출

### 3. EntryPoint 사용
- Hilt를 사용하는 앱에서 Application 초기화 시 일반적
- `@EntryPoint`는 Hilt의 공식 패턴

---

## 결론

### 현재 패턴
- **작동은 하지만 아키텍처적으로 완벽하지 않음**
- EntryPoint 사용은 문제 없음 (Hilt 표준)
- ViewModel에서 BillingManager 직접 사용은 개선 필요

### 권장 개선
1. **Repository에서 BillingManager 캡슐화**
2. **구매 이벤트를 Flow로 노출**
3. **ViewModel은 Repository만 의존**

### 선택지
- **현재 패턴 유지**: 작동은 하지만 아키텍처가 완벽하지 않음
- **Repository 중심으로 리팩토링**: 더 나은 아키텍처, 약간의 코드 수정 필요

---

## 참고 자료

- [Google Play Billing Best Practices](https://developer.android.com/google/play/billing/best_practices)
- [Hilt EntryPoint Documentation](https://dagger.dev/hilt/entry-points.html)
- [Android Architecture Guide](https://developer.android.com/topic/architecture)












