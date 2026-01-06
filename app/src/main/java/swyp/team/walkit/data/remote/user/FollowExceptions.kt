package swyp.team.walkit.data.remote.user

/**
 * 팔로우 관련 예외 클래스들
 */

/**
 * 존재하지 않는 유저를 팔로우하려고 할 때 발생하는 예외
 * HTTP 404, Error Code 1001
 */
class FollowUserNotFoundException(
    message: String = "존재하지 않는 유저입니다",
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * 자기 자신에게 팔로우 신청할 때 발생하는 예외
 * HTTP 400, Error Code 2004
 */
class FollowSelfException(
    message: String = "자기 자신에게는 팔로우 신청할 수 없습니다",
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * 이미 보낸 팔로우 신청(진행중)이 있을 때 발생하는 예외
 * HTTP 409, Error Code 2002
 */
class FollowRequestAlreadyExistsException(
    message: String = "이미 팔로우 신청을 보냈습니다",
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * 이미 팔로우되어 있는 경우 발생하는 예외
 * HTTP 409, Error Code 2003
 */
class AlreadyFollowingException(
    message: String = "이미 팔로우 중입니다",
    cause: Throwable? = null,
) : Exception(message, cause)








