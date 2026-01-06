package swyp.team.walkit.domain.exception

sealed class FollowerException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/** 404, 1001 */
class FollowNotFoundException(
    cause: Throwable? = null,
) : FollowerException(
    message = "존재하지 않는 사용자입니다.",
    cause = cause,
)

/** 404, 2001 */
class NotFollowingException(
    cause: Throwable? = null,
) : FollowerException(
    message = "팔로우 관계가 아닙니다.",
    cause = cause,
)
