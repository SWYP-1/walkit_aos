package swyp.team.walkit.data.remote.exception

import java.io.IOException

/**
 * 인증 만료 예외 - 토큰이 만료되어 로그인 페이지로 리다이렉트되는 경우
 */
class AuthExpiredException(message: String) : IOException(message)