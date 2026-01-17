package swyp.team.walkit.data.remote.home

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert

import org.junit.Before
import org.junit.Test


import retrofit2.HttpException
import retrofit2.Response
import swyp.team.walkit.core.Result
import swyp.team.walkit.data.api.home.HomeApi
import swyp.team.walkit.data.remote.exception.AuthExpiredException
import swyp.team.walkit.data.remote.home.dto.HomeData

/**
 * HomeRemoteDataSource 단위 테스트
 *
 * 테스트 대상:
 * - getHomeData(): AuthExpiredException 처리
 * - 일반 예외 처리
 * - 정상 응답 처리
 */
class HomeRemoteDataSourceTest {

    private lateinit var homeApi: HomeApi
    private lateinit var dataSource: HomeRemoteDataSource

    @Before
    fun setup() {
        homeApi = mockk()
        dataSource = HomeRemoteDataSource(homeApi)
    }

    @Test
    fun `getHomeData - AuthExpiredException 발생 시 Result_Error 반환`() = runTest {
        // Given: AuthExpiredException이 발생하는 상황
        val lat = 37.5665
        val lon = 126.978
        val exception = AuthExpiredException("Authentication expired")

        coEvery { homeApi.getHomeData(lat, lon) } throws exception

        // When: API 호출
        val result = dataSource.getHomeData(lat, lon)

        // Then: AuthExpiredException을 특별히 처리한 Result.Error 반환
        Assert.assertTrue("결과는 Result.Error여야 함", result is Result.Error)
        val errorResult = result as Result.Error
        Assert.assertEquals("예외 객체가 동일해야 함", exception, errorResult.exception)
        Assert.assertEquals("에러 메시지가 올바르게 설정되어야 함", "로그인이 필요합니다. 다시 로그인해주세요.", errorResult.message)
    }

    @Test
    fun `getHomeData - 일반 예외 발생 시 Result_Error 반환`() = runTest {
        // Given: 일반 예외가 발생하는 상황
        val lat = 37.5665
        val lon = 126.978
        val exception = RuntimeException("Network error")

        coEvery { homeApi.getHomeData(lat, lon) } throws exception

        // When: API 호출
        val result = dataSource.getHomeData(lat, lon)

        // Then: 일반 예외를 Result.Error로 변환
        Assert.assertTrue("결과는 Result.Error여야 함", result is Result.Error)
        val errorResult = result as Result.Error
        Assert.assertEquals("예외 객체가 동일해야 함", exception, errorResult.exception)
        Assert.assertEquals("에러 메시지가 예외 메시지와 동일해야 함", "Network error", errorResult.message)
    }

    @Test
    fun `getHomeData - HttpException 발생 시 Result_Error 반환`() = runTest {
        // Given: HTTP 예외가 발생하는 상황
        val lat = 37.5665
        val lon = 126.978
        val errorResponse = Response.error<String>(
            500,
            "Internal Server Error".toResponseBody()
        )
        val exception = HttpException(errorResponse)

        coEvery { homeApi.getHomeData(lat, lon) } throws exception

        // When: API 호출
        val result = dataSource.getHomeData(lat, lon)

        // Then: HttpException을 Result.Error로 변환
        Assert.assertTrue("결과는 Result.Error여야 함", result is Result.Error)
        val errorResult = result as Result.Error
        Assert.assertEquals("예외 객체가 동일해야 함", exception, errorResult.exception)
        Assert.assertEquals("에러 메시지가 null이거나 적절한 메시지여야 함", null, errorResult.message)
    }

    @Test
    fun `getHomeData - 성공 응답 시 Result_Success 반환`() = runTest {
        // Given: 성공적인 API 응답
        val lat = 37.5665
        val lon = 126.978
        val mockHomeData = mockk<HomeData>()

        coEvery { homeApi.getHomeData(lat, lon) } returns mockHomeData

        // When: API 호출
        val result = dataSource.getHomeData(lat, lon)

        // Then: 성공 결과를 Result.Success로 반환
        Assert.assertTrue("결과는 Result.Success여야 함", result is Result.Success)
        val successResult = result as Result.Success
        Assert.assertEquals("반환된 데이터가 동일해야 함", mockHomeData, successResult.data)
    }

    @Test
    fun `getHomeData - 다양한 좌표값으로 호출 가능`() = runTest {
        // Given: 다양한 좌표값들
        val testCases = listOf(
            37.5665 to 126.978,   // 서울 시청
            35.1796 to 129.0756,  // 부산
            33.4996 to 126.5312,  // 제주
            0.0 to 0.0,           // 적도/본초자오선
            -90.0 to 180.0        // 남극/날짜변경선
        )

        testCases.forEach { (lat, lon) ->
            val mockHomeData = mockk<HomeData>()
            coEvery { homeApi.getHomeData(lat, lon) } returns mockHomeData

            // When: 각 좌표로 API 호출
            val result = dataSource.getHomeData(lat, lon)

            // Then: 모두 성공해야 함
            Assert.assertTrue("좌표 ($lat, $lon)로의 호출이 성공해야 함", result is Result.Success)
        }
    }
}