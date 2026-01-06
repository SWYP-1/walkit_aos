package swyp.team.walkit.data.remote.friend

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import swyp.team.walkit.data.api.follower.FollowerApi
import swyp.team.walkit.data.remote.user.AlreadyFollowingException
import swyp.team.walkit.data.remote.user.FollowRequestAlreadyExistsException
import swyp.team.walkit.data.remote.user.FollowSelfException
import swyp.team.walkit.data.remote.user.FollowUserNotFoundException
import swyp.team.walkit.domain.exception.NotFollowingException
import swyp.team.walkit.domain.exception.FollowNotFoundException
import swyp.team.walkit.domain.model.Friend
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 친구 목록 원격 데이터 소스
 */
@Singleton
class FollowRemoteDataSource @Inject constructor(
    private val followerApi: FollowerApi
) {

    /**
     * 친구 목록 조회
     */
    suspend fun getFriends(): List<Friend> = withContext(Dispatchers.IO) {
        try {
            val response = followerApi.getFriends()
            response.map { dto ->
                Friend(
                    id = dto.userId.toString(),
                    nickname = dto.nickname ?: "게스트", // null이면 "게스트"로 설정
                    avatarUrl = dto.userImageUrl
                )
            }
        } catch (t: Throwable) {
            Timber.e(t, "친구 목록 조회 실패")
            throw t
        }
    }

    /**
     * 팔로우 요청 수락
     *
     * @param nickname 팔로우 요청을 수락할 사용자의 닉네임
     * @throws Exception API 호출 실패 시 예외 발생
     */
    suspend fun acceptFollowRequest(nickname: String) {
        try {
            val response = followerApi.acceptFollowRequest(nickname)
            if (response.isSuccessful) {
                Timber.d("팔로우 요청 수락 성공: $nickname")
            } else {
                val errorMessage = response.errorBody()?.string() ?: "팔로우 요청 수락 실패"
                Timber.e("팔로우 요청 수락 실패: $errorMessage (코드: ${response.code()})")
                throw Exception("팔로우 요청 수락 실패: ${response.code()}")
            }
        } catch (t: Throwable) {
            Timber.e(t, "팔로우 요청 수락 실패: $nickname")
            throw t
        }
    }

    /**
     * 팔로우 요청 거절/삭제
     *
     * @param nickname 팔로우 요청을 거절할 사용자의 닉네임
     * @throws Exception API 호출 실패 시 예외 발생
     */
    suspend fun rejectFollowRequest(nickname: String) {
        try {
            val response = followerApi.rejectFollowRequest(nickname)
            if (response.isSuccessful) {
                Timber.d("팔로우 요청 거절 성공: $nickname")
            } else {
                val errorMessage = response.errorBody()?.string() ?: "팔로우 요청 거절 실패"
                Timber.e("팔로우 요청 거절 실패: $errorMessage (코드: ${response.code()})")
                throw Exception("팔로우 요청 거절 실패: ${response.code()}")
            }
        } catch (t: Throwable) {
            Timber.e(t, "팔로우 요청 거절 실패: $nickname")
            throw t
        }
    }

    /**
     * 닉네임으로 사용자 팔로우
     *
     * @param nickname 팔로우할 사용자의 닉네임
     * @throws FollowUserNotFoundException 존재하지 않는 유저를 팔로우하려고 할 때 (404, 1001)
     * @throws FollowSelfException 자기 자신에게 팔로우 신청할 때 (400, 2004)
     * @throws FollowRequestAlreadyExistsException 이미 보낸 팔로우 신청이 있을 때 (409, 2002)
     * @throws AlreadyFollowingException 이미 팔로우되어 있는 경우 (409, 2003)
     */
    suspend fun followUserByNickname(nickname: String) {
        try {
            val response = followerApi.followUserByNickname(nickname)
            if (response.isSuccessful) {
                Timber.d("팔로우 성공: $nickname")
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                val errorCode = extractErrorCode(errorBody)
                val httpCode = response.code()

                when {
                    // 404 또는 1001: 존재하지 않는 유저
                    httpCode == 404 || errorCode == 1001 -> {
                        Timber.e("존재하지 않는 유저: $nickname (HTTP $httpCode, Code $errorCode)")
                        throw FollowUserNotFoundException("존재하지 않는 유저입니다", HttpException(response))
                    }
                    // 400 또는 2004: 자기 자신에게 팔로우
                    httpCode == 400 && errorCode == 2004 -> {
                        Timber.e("자기 자신에게 팔로우: $nickname (HTTP $httpCode, Code $errorCode)")
                        throw FollowSelfException("자기 자신에게는 팔로우 신청할 수 없습니다", HttpException(response))
                    }
                    // 409 또는 2002: 이미 보낸 팔로우 신청
                    httpCode == 409 && errorCode == 2002 -> {
                        Timber.e("이미 보낸 팔로우 신청: $nickname (HTTP $httpCode, Code $errorCode)")
                        throw FollowRequestAlreadyExistsException("이미 팔로우 신청을 보냈습니다", HttpException(response))
                    }
                    // 409 또는 2003: 이미 팔로우됨
                    httpCode == 409 && errorCode == 2003 -> {
                        Timber.e("이미 팔로우 중: $nickname (HTTP $httpCode, Code $errorCode)")
                        throw AlreadyFollowingException("이미 팔로우 중입니다", HttpException(response))
                    }
                    else -> {
                        Timber.e("팔로우 실패: $nickname (HTTP $httpCode, Code $errorCode)")
                        throw HttpException(response)
                    }
                }
            }
        } catch (e: FollowUserNotFoundException) {
            throw e
        } catch (e: FollowSelfException) {
            throw e
        } catch (e: FollowRequestAlreadyExistsException) {
            throw e
        } catch (e: AlreadyFollowingException) {
            throw e
        } catch (e: HttpException) {
            // HttpException이지만 위에서 처리되지 않은 경우
            Timber.e(e, "팔로우 HTTP 오류: $nickname (${e.code()})")
            throw e
        } catch (t: Throwable) {
            Timber.e(t, "팔로우 실패: $nickname")
            throw t
        }
    }

    /**
     * 친구 차단(삭제)하기
     *
     * @param nickname 팔로우할 사용자의 닉네임
     */
    suspend fun blockUser(nickname: String) {
        val response = followerApi.blockUser(nickname)
        if (!response.isSuccessful) {
            val response = followerApi.blockUser(nickname)

            if (!response.isSuccessful) {
                val errorCode = extractErrorCode(response.errorBody()?.string().orEmpty())

                throw when {
                    response.code() == 404 && errorCode == 1001 -> FollowNotFoundException()
                    response.code() == 404 && errorCode == 2001 -> NotFollowingException()
                    else -> HttpException(response)
                }
            }

        }
    }
}

/**
 * 에러 응답 본문에서 에러 코드 추출
 * JSON 형식: {"code": 1001, ...} 또는 단순 문자열
 */
private fun extractErrorCode(errorBody: String): Int? {
    return try {
        // JSON 형식에서 code 필드 추출 시도
        val codePattern = "\"code\"\\s*:\\s*(\\d+)".toRegex()
        codePattern.find(errorBody)?.groupValues?.get(1)?.toInt()
    } catch (t: Throwable) {
        null
    }
}

