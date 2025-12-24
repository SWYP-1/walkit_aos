# Signed URL 생성 위치: 백엔드 vs Firebase Storage Rules

## ❌ 오해: Firebase Storage Rules로 Signed URL 생성?

**아니요!** Firebase Storage Rules는 Signed URL을 생성하지 않습니다.

## ✅ 정확한 설명

### Signed URL 생성 위치: **백엔드 (서버)**

Signed URL은 **Firebase Admin SDK**를 사용해서 **서버(백엔드)에서만** 생성할 수 있습니다.

### Firebase Storage Rules의 역할: **접근 권한 제어**

Firebase Storage Rules는 **클라이언트가 직접 접근할 수 있는지 여부**를 결정합니다.

## 구분 정리

### 1. Signed URL 생성 (백엔드)

```javascript
// ✅ 서버 코드 (Node.js 예시)
const admin = require('firebase-admin');
const { getStorage } = require('firebase-admin/storage');

// Firebase Admin SDK 초기화 (서버에서만 가능)
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

// Signed URL 생성
async function getSignedUrl(storagePath) {
  const bucket = getStorage().bucket();
  const file = bucket.file(storagePath);
  
  // ✅ 서버에서 Signed URL 생성
  const [url] = await file.getSignedUrl({
    action: 'read',
    expires: Date.now() + 3600 * 1000  // 1시간 후 만료
  });
  
  return url;
}
```

**특징**:
- Firebase Admin SDK 필요 (서버에서만 사용 가능)
- 서비스 계정(Service Account) 키 필요
- 클라이언트에서는 생성 불가능

### 2. Firebase Storage Rules (접근 권한 제어)

```javascript
// Firebase Storage 보안 규칙
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /cosmetic-items/{category}/{fileName} {
      // 클라이언트 직접 접근 차단
      allow read: if false;  // ❌ 클라이언트에서 직접 접근 불가
      
      // 서버 관리자만 쓰기 가능
      allow write: if request.auth != null 
                   && request.auth.token.admin == true;
    }
  }
}
```

**역할**:
- 클라이언트가 직접 Storage에 접근할 수 있는지 결정
- Signed URL은 이 규칙을 우회할 수 있음 (서버에서 생성했으므로)

## 전체 흐름

### 1. 서버에서 Signed URL 생성

```
백엔드 서버
  ↓
Firebase Admin SDK 사용
  ↓
서비스 계정 키로 인증
  ↓
Signed URL 생성
  ↓
클라이언트에 제공
```

### 2. Firebase Storage Rules로 접근 제어

```
클라이언트
  ↓
직접 Storage 접근 시도
  ↓
Firebase Storage Rules 검사
  ↓
allow read: if false
  ↓
❌ 접근 차단
```

### 3. Signed URL로 접근 (Rules 우회)

```
클라이언트
  ↓
서버에서 받은 Signed URL 사용
  ↓
Firebase Storage Rules 검사
  ↓
✅ Signed URL은 서버에서 생성했으므로 접근 허용
```

## 실제 구현 예시

### 백엔드 서버 코드 (Node.js)

```javascript
// server.js
const admin = require('firebase-admin');
const express = require('express');
const { getStorage } = require('firebase-admin/storage');

// Firebase Admin SDK 초기화
admin.initializeApp({
  credential: admin.credential.cert({
    // 서비스 계정 키 (서버에만 보관)
    projectId: "your-project-id",
    privateKey: "-----BEGIN PRIVATE KEY-----\n...",
    clientEmail: "firebase-adminsdk@your-project.iam.gserviceaccount.com"
  })
});

const app = express();

// 구매한 아이템의 Signed URL 생성 API
app.get('/api/cosmetic-items/purchased/resources', async (req, res) => {
  // 1. 사용자 인증 확인
  const userId = req.user.id;  // JWT 토큰에서 추출
  
  // 2. 구매 내역 검증
  const purchasedItems = await verifyPurchases(userId);
  
  // 3. 구매한 아이템에 대해서만 Signed URL 생성
  const resources = [];
  for (const item of purchasedItems) {
    const bucket = getStorage().bucket();
    const file = bucket.file(`cosmetic-items/${item.category}/${item.fileName}`);
    
    // ✅ 서버에서 Signed URL 생성
    const [url] = await file.getSignedUrl({
      action: 'read',
      expires: Date.now() + 3600 * 1000  // 1시간
    });
    
    resources.push({
      productId: item.productId,
      resourceUrl: url,
      expiresAt: Date.now() + 3600 * 1000
    });
  }
  
  res.json({ items: resources });
});
```

### Firebase Storage Rules

```javascript
// storage.rules
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // 기본 캐릭터는 공개
    match /cosmetic-items/base/{fileName} {
      allow read: if true;
    }
    
    // 아이템 리소스는 클라이언트 직접 접근 차단
    match /cosmetic-items/{category}/{fileName} {
      // ❌ 클라이언트에서 직접 접근 불가
      allow read: if false;
      
      // ✅ 서버 관리자만 쓰기 가능
      allow write: if request.auth != null 
                   && request.auth.token.admin == true;
    }
  }
}
```

**효과**:
- 클라이언트가 직접 Storage에 접근하려고 하면 차단됨
- 하지만 서버에서 생성한 Signed URL은 접근 가능 (서버 권한으로 생성했으므로)

## 클라이언트에서 Signed URL 사용

```kotlin
// 클라이언트 코드 (Android)
// 서버에서 받은 Signed URL 사용
val signedUrl = "https://firebasestorage.../shoes_05.png?X-Goog-Expires=3600&..."

// Coil로 이미지 로드
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(signedUrl)  // 서버에서 받은 Signed URL
        .build(),
    contentDescription = "신발"
)
```

## 요약

| 항목 | 위치 | 역할 |
|------|------|------|
| **Signed URL 생성** | 백엔드 (서버) | Firebase Admin SDK로 Signed URL 생성 |
| **Firebase Storage Rules** | Firebase Console | 클라이언트 직접 접근 권한 제어 |
| **Signed URL 사용** | 클라이언트 | 서버에서 받은 Signed URL로 이미지 로드 |

## 핵심 정리

### ✅ Signed URL 생성
- **위치**: 백엔드 (서버)
- **도구**: Firebase Admin SDK
- **권한**: 서비스 계정 키 필요
- **클라이언트**: 생성 불가능

### ✅ Firebase Storage Rules
- **위치**: Firebase Console
- **역할**: 클라이언트 직접 접근 권한 제어
- **효과**: 클라이언트 직접 접근 차단, Signed URL은 허용

### ✅ 전체 보안 구조

```
1. Firebase Storage Rules
   → 클라이언트 직접 접근 차단 (allow read: if false)

2. 백엔드 서버
   → 구매 검증 후 Signed URL 생성 (Firebase Admin SDK)

3. 클라이언트
   → 서버에서 받은 Signed URL로 이미지 로드
```

**결론**: Signed URL은 **백엔드에서 생성**하고, Firebase Storage Rules는 **접근 권한을 제어**합니다.








