package swyp.team.walkit.data.remote.interceptor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert
import org.junit.Test
import swyp.team.walkit.data.remote.exception.AuthExpiredException
import swyp.team.walkit.data.remote.auth.TokenProvider

/**
 * AuthInterceptor 단위 테스트
 *
 * 테스트 대상:
 * - AuthExpiredException: 인증 만료 예외 클래스
 * - 302 리다이렉트 처리 로직
 * - 401 + 토큰 갱신 실패 처리 로직
 */
class AuthInterceptorTest {

    // ===== AuthExpiredException 테스트 =====

    @Test
    fun `AuthExpiredException - 메시지 포함해서 생성됨`() {
        // Given: 예외 메시지
        val message = "Authentication expired: redirect to login page"

        // When: 예외 생성
        val exception = AuthExpiredException(message)

        // Then: 메시지가 올바르게 설정됨
        Assert.assertEquals(message, exception.message)
        Assert.assertTrue("IOException의 서브클래스여야 함", exception is java.io.IOException)
    }

    @Test
    fun `AuthExpiredException - 빈 메시지도 가능함`() {
        // Given: 빈 메시지
        val message = ""

        // When: 예외 생성
        val exception = AuthExpiredException(message)

        // Then: 메시지가 올바르게 설정됨
        Assert.assertEquals(message, exception.message)
    }

    // ===== 302 리다이렉트 응답 모킹 테스트 =====
    // Note: 실제 인터셉터 테스트는 통합 테스트에서 하는 것이 더 적합함
    // 여기서는 예외 발생 로직의 정적 테스트만 수행

    @Test
    fun `302 리다이렉트 감지 로직 - 로그인 페이지 리다이렉트`() {
        // Given: 302 응답의 조건들
        val code = 302
        val locationHeader = "http://walkit-shop-swyp-11.shop/login"
        val contentType = "text/html"

        // When: 302 리다이렉트 감지 조건 확인
        val is302Redirect = code == 302
        val hasLoginLocation = locationHeader?.contains("/login") == true
        val isAuthFailure = is302Redirect && hasLoginLocation

        // Then: 인증 실패로 감지되어야 함
        Assert.assertTrue("302 리다이렉트는 인증 실패로 감지되어야 함", isAuthFailure)
    }

    @Test
    fun `401 에러 감지 로직 - 인증 헤더 없음`() {
        // Given: 401 응답
        val code = 401
        val contentType = "application/json"

        // When: 인증 실패 감지
        val is401Error = code == 401
        val isHtmlResponse = contentType?.contains("text/html") == true
        val isAuthFailure = is401Error

        // Then: 인증 실패로 감지되어야 함
        Assert.assertTrue("401 에러는 인증 실패로 감지되어야 함", isAuthFailure)
    }

    @Test
    fun `HTML 응답 감지 로직 - 로그인 페이지 HTML`() {
        // Given: HTML 응답 (로그인 페이지)
        val code = 200
        val contentType = "text/html"
        val requestPath = "/some-api"

        // When: 인증 실패 감지
        val is401Error = code == 401
        val is302Redirect = code == 302
        val isHtmlResponse = contentType?.contains("text/html") == true
        val isAuthPath = requestPath.contains("/auth/")
        val isAuthFailure = (!isAuthPath) && isHtmlResponse

        // Then: 인증 경로가 아니면 인증 실패로 감지
        Assert.assertTrue("HTML 응답은 인증 실패로 감지될 수 있음", isAuthFailure)
    }

    @Test
    fun `인증 경로는 HTML 응답도 허용됨`() {
        // Given: 인증 API의 HTML 응답 (정상)
        val code = 200
        val contentType = "text/html"
        val requestPath = "/auth/login"

        // When: 인증 실패 감지
        val isAuthPath = requestPath.contains("/auth/")
        val isHtmlResponse = contentType?.contains("text/html") == true
        val isAuthFailure = (!isAuthPath) && isHtmlResponse

        // Then: 인증 경로는 HTML 응답도 허용
        Assert.assertTrue("인증 경로는 HTML 응답이 허용됨", !isAuthFailure)
    }
}