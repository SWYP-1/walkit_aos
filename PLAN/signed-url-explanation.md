# Signed URL이란? 왜 안전한가?

## Signed URL이란?

**Signed URL**은 특정 파일에 대한 **임시 접근 권한**을 부여하는 URL입니다.

### 일반 URL vs Signed URL

#### 일반 다운로드 URL (영구)
```
https://firebasestorage.googleapis.com/v0/b/myapp.appspot.com/o/cosmetic-items%2Fshoes%2Fshoes_05.png?alt=media&token=abc123
```
- **문제점**: 
  - 영구적으로 유효 (만료 시간 없음)
  - URL을 알면 누구나 접근 가능
  - URL이 노출되면 계속 사용 가능

#### Signed URL (임시)
```
https://firebasestorage.googleapis.com/v0/b/myapp.appspot.com/o/cosmetic-items%2Fshoes%2Fshoes_05.png?X-Goog-Algorithm=GOOG4-RSA-SHA256&X-Goog-Credential=...&X-Goog-Date=20240101T120000Z&X-Goog-Expires=3600&X-Goog-SignedHeaders=host&X-Goog-Signature=...
```
- **특징**:
  - 만료 시간이 포함됨 (`X-Goog-Expires=3600` = 1시간)
  - 서명(signature)이 포함되어 변조 불가능
  - 만료 후 자동으로 접근 불가

## Signed URL의 보안 메커니즘

### 1. 만료 시간 (Expiration)
```javascript
// 서버에서 Signed URL 생성
const [url] = await file.getSignedUrl({
  action: 'read',
  expires: Date.now() + 3600 * 1000  // 1시간 후 만료
});
```

**효과**:
- URL이 노출되어도 일정 시간 후 자동으로 무효화
- 예: 1시간 후 만료 → 그 이후에는 접근 불가

### 2. 서명 (Signature)
```
X-Goog-Signature=abc123def456...
```

**효과**:
- URL에 암호화된 서명 포함
- URL을 수정하면 서명이 맞지 않아 접근 불가
- 예: `shoes_05.png` → `shoes_10.png`로 변경 시도 → 실패

### 3. 서버에서만 생성 가능
```javascript
// Firebase Admin SDK 필요 (서버에서만 사용 가능)
const admin = require('firebase-admin');
const { getStorage } = require('firebase-admin/storage');

// 클라이언트에서는 생성 불가능!
```

**효과**:
- 클라이언트에서 직접 생성 불가능
- 서버에서만 생성 가능하므로 제어 가능

**⚠️ 중요**: 
- Signed URL은 **백엔드(서버)에서 생성**합니다
- Firebase Storage Rules는 Signed URL을 생성하지 않습니다
- Firebase Storage Rules는 **접근 권한을 제어**합니다
- 자세한 내용: [signed-url-generation.md](mdc:PLAN/signed-url-generation.md)

## ⚠️ Signed URL만으로는 완전히 안전하지 않음!

### 문제점

#### 1. URL 노출 시 여전히 접근 가능
```
사용자 A가 구매한 아이템의 Signed URL을 받음
→ URL을 다른 사용자 B에게 공유
→ B도 만료 시간 전까지 접근 가능
```

**⚠️ 중요**: 구매 검증이 있어도 URL이 노출되면 다른 사용자가 접근 가능합니다!
- 구매 검증: 구매하지 않은 아이템의 URL을 생성하지 않음
- 하지만 구매한 아이템의 URL이 노출되면 여전히 접근 가능
- Signed URL의 만료 시간으로 피해를 최소화할 수 있음

#### 2. 구매하지 않은 아이템의 Signed URL 생성 가능
```
악의적인 서버 관리자가
→ 구매하지 않은 아이템의 Signed URL 생성
→ 클라이언트에 제공
→ 무단 접근 가능
```

**해결**: 구매 검증이 필수입니다!

## ✅ 진짜 안전한 방법: 구매 검증 + Signed URL

### 핵심 원칙

**Signed URL은 "임시 접근 권한"을 제공하지만, "구매 검증"이 필수입니다.**

### 안전한 구현 흐름

```
1. 사용자가 아이템 구매
   ↓
2. 서버에 구매 검증 요청
   (productId + purchaseToken)
   ↓
3. 서버에서 Google Play API로 구매 검증
   ✅ 검증 성공: 구매 내역 DB에 저장
   ❌ 검증 실패: 구매 취소
   ↓
4. 구매 검증 성공한 아이템에 대해서만 Signed URL 생성
   ✅ shoes_05 구매함 → Signed URL 생성
   ❌ shoes_10 구매 안 함 → Signed URL 생성 안 함
   ↓
5. 구매한 아이템의 Signed URL만 클라이언트에 제공
   ↓
6. 클라이언트에서 Signed URL로 이미지 로드
```

### 보안 레이어

#### 레이어 1: 구매 검증 (필수)
```javascript
// 서버 코드
async function getPurchasedItemUrls(userId, productIds) {
  // 1. 사용자의 구매 내역 검증
  const purchasedItems = await verifyPurchases(userId, productIds);
  
  // 2. 구매한 아이템에 대해서만 Signed URL 생성
  const urls = {};
  for (const item of purchasedItems) {
    // ✅ 구매 검증 완료된 아이템만
    if (item.isPurchased) {
      urls[item.productId] = await getSignedUrl(item.resourcePath);
    }
  }
  
  return urls;
}
```

#### 레이어 2: Signed URL (추가 보안)
```javascript
// 만료 시간 설정으로 추가 보안
const [url] = await file.getSignedUrl({
  action: 'read',
  expires: Date.now() + 3600 * 1000  // 1시간
});
```

#### 레이어 3: Firebase Storage 보안 규칙
```javascript
// Firebase Storage 보안 규칙
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /cosmetic-items/{category}/{fileName} {
      // 클라이언트 직접 접근 차단
      allow read: if false;  // Signed URL만 허용
      
      // 서버 관리자만 쓰기 가능
      allow write: if request.auth != null 
                   && request.auth.token.admin == true;
    }
  }
}
```

## 실제 보안 효과

### ✅ 구매 검증으로 해결되는 문제
- 구매하지 않은 아이템 접근 불가
- 서버에서 구매 내역 확인 후 URL 생성

### ✅ Signed URL로 해결되는 문제
- URL 노출 시에도 만료 시간 후 자동 무효화
- URL 변조 불가능 (서명 검증)

### ✅ Firebase Storage 보안 규칙으로 해결되는 문제
- 클라이언트에서 직접 Storage 접근 불가
- Signed URL을 통해서만 접근 가능

## 결론

### Signed URL만으로는 부족합니다!

**진짜 안전한 방법**:
1. ✅ **구매 검증** (필수) - 구매한 아이템에 대해서만 URL 생성
2. ✅ **Signed URL** (추가 보안) - 만료 시간으로 일시적 접근
3. ✅ **Firebase Storage 보안 규칙** - 직접 접근 차단

### ⚠️ 구매 검증이 있어도 URL 노출은 위험합니다!

**핵심 정리**:
- 구매 검증: 구매하지 않은 아이템의 URL을 아예 생성하지 않음
- Signed URL: 구매한 아이템의 URL이 노출되어도 만료 시간 후 무효화
- **둘 다 필요합니다!**

**자세한 분석**: [url-exposure-analysis.md](mdc:PLAN/url-exposure-analysis.md) 참고

### 최종 아키텍처

```
클라이언트
  ↓ (구매 요청)
Google Play
  ↓ (구매 완료)
클라이언트
  ↓ (구매 검증 요청: productId + purchaseToken)
서버 API
  ↓ (Google Play API로 구매 검증)
Google Play API
  ↓ (구매 검증 완료)
서버 API
  ↓ (구매한 아이템에 대해서만 Signed URL 생성)
Firebase Storage
  ↓ (Signed URL 반환, 만료 시간 포함)
서버 API
  ↓ (구매한 아이템 + Signed URL)
클라이언트
  ↓ (Signed URL로 이미지 로드)
```

**핵심**: Signed URL은 "임시 접근 권한"을 제공하지만, **구매 검증이 필수**입니다!











