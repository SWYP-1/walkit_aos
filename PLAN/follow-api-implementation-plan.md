# 팔로우 API 구현 계획

## API 스펙
- **Endpoint**: `POST /follows/nickname/{nickname}`
- **Path Parameter**: `nickname` (String)

## 에러 코드 처리

| HTTP Status | Error Code | 설명 | Exception |
|-------------|------------|------|-----------|
| 404 | 1001 | 존재하지 않는 유저 조회 | `FollowUserNotFoundException` |
| 400 | 2004 | 자기 자신에게 팔로우 신청 | `FollowSelfException` |
| 409 | 2002 | 이미 보낸 팔로우 신청(진행중) | `FollowRequestAlreadyExistsException` |
| 409 | 2003 | 이미 팔로우되어 있는 경우 | `AlreadyFollowingException` |

## 구현 단계

### 1. API 인터페이스 추가
**파일**: `app/src/main/java/team/swyp/sdu/data/api/user/UserApi.kt`
- `followUserByNickname(nickname: String): Response<Unit>` 메서드 추가

### 2. 커스텀 Exception 클래스 생성
**파일**: `app/src/main/java/team/swyp/sdu/data/remote/user/FollowExceptions.kt` (새 파일)
- `FollowUserNotFoundException` (404, 1001)
- `FollowSelfException` (400, 2004)
- `FollowRequestAlreadyExistsException` (409, 2002)
- `AlreadyFollowingException` (409, 2003)

### 3. RemoteDataSource에 팔로우 메서드 추가
**파일**: `app/src/main/java/team/swyp/sdu/data/remote/user/UserRemoteDataSource.kt`
- `followUserByNickname(nickname: String)` 메서드 추가
- HTTP 상태 코드 및 에러 코드 파싱하여 적절한 Exception throw
- 성공 시 로그 기록

### 4. ViewModel에 팔로우 메서드 추가
**파일**: `app/src/main/java/team/swyp/sdu/ui/friend/FriendViewModel.kt`
- `followUser(nickname: String)` 메서드 추가
- 각 Exception에 대한 적절한 에러 메시지 처리
- 성공 시 검색 결과 상태 업데이트 (팔로우 상태 변경)

### 5. UI 연결
**파일**: `app/src/main/java/team/swyp/sdu/ui/friend/FriendSearchScreen.kt`
- `onFollowClick` 콜백에서 `viewModel.followUser(nickname)` 호출
- 성공/실패에 따른 사용자 피드백 (Toast 또는 Snackbar)

## 에러 메시지

| Exception | 사용자 메시지 |
|-----------|--------------|
| `FollowUserNotFoundException` | "존재하지 않는 유저입니다" |
| `FollowSelfException` | "자기 자신에게는 팔로우 신청할 수 없습니다" |
| `FollowRequestAlreadyExistsException` | "이미 팔로우 신청을 보냈습니다" |
| `AlreadyFollowingException` | "이미 팔로우 중입니다" |

## 성공 처리
- 팔로우 성공 시 검색 결과의 `followStatus`를 `FollowStatus.PENDING`으로 업데이트
- UI에 성공 메시지 표시 (선택사항)

## 추가 고려사항
- 팔로우 성공 후 검색 결과를 다시 조회할지, 아니면 로컬 상태만 업데이트할지 결정 필요
- 로딩 상태 관리 (팔로우 요청 중 버튼 비활성화)








