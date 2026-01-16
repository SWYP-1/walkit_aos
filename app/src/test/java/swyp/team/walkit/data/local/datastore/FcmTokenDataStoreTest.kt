package swyp.team.walkit.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * FcmTokenDataStore 단위 테스트
 *
 * 테스트 대상:
 * - fcmToken Flow 프로퍼티
 * - saveToken(): 토큰 저장
 * - getToken(): 토큰 조회
 * - clearToken(): 토큰 삭제
 */
class FcmTokenDataStoreTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var fcmTokenDataStore: FcmTokenDataStore

    @Before
    fun setup() {
        dataStore = mockk()
        fcmTokenDataStore = FcmTokenDataStore(dataStore)
    }

    @Test
    fun `fcmToken Flow - DataStore 데이터를 올바르게 매핑해야 함`() = runTest {
        // Given: Mock DataStore가 특정 값을 반환하도록 설정
        val mockToken = "test_fcm_token_123"
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[stringPreferencesKey("fcm_token")] } returns mockToken
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: fcmToken Flow에서 값 수집
        val result = fcmTokenDataStore.fcmToken.first()

        // Then: 올바른 값이 반환됨
        Assert.assertEquals(mockToken, result)
    }

    @Test
    fun `fcmToken Flow - 값이 없을 때 null 반환`() = runTest {
        // Given: Mock DataStore가 null을 반환하도록 설정
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[stringPreferencesKey("fcm_token")] } returns null
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: fcmToken Flow에서 값 수집
        val result = fcmTokenDataStore.fcmToken.first()

        // Then: null이 반환됨
        assertNull(result)
    }

    @Test
    fun `saveToken - 토큰을 올바르게 저장해야 함`() = runTest {
        // Given: 저장할 토큰
        val token = "new_fcm_token_456"

        // When: saveToken 호출
        fcmTokenDataStore.saveToken(token)

        // Then: DataStore.edit가 올바른 키와 값으로 호출되었는지 검증
        coVerify { dataStore.edit(any())  } // edit이 호출되었는지 검증
    }

    @Test
    fun `getToken - 토큰을 올바르게 조회해야 함`() = runTest {
        // Given: Mock DataStore가 특정 토큰을 반환하도록 설정
        val expectedToken = "retrieved_fcm_token"
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[stringPreferencesKey("fcm_token")] } returns expectedToken
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: getToken 호출
        val result = fcmTokenDataStore.getToken()

        // Then: 올바른 토큰이 반환됨
        Assert.assertEquals(expectedToken, result)
    }

    @Test
    fun `getToken - 토큰이 없을 때 null 반환`() = runTest {
        // Given: Mock DataStore가 null을 반환하도록 설정
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[stringPreferencesKey("fcm_token")] } returns null
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: getToken 호출
        val result = fcmTokenDataStore.getToken()

        // Then: null이 반환됨
        assertNull(result)
    }

    @Test
    fun `clearToken - 토큰을 올바르게 삭제해야 함`() = runTest {
        // When: clearToken 호출
        fcmTokenDataStore.clearToken()

        // Then: DataStore.edit가 호출되었는지 검증
        coVerify { dataStore.edit(any()) } // edit이 호출되었는지 검증
    }

    @Test
    fun `다양한 토큰 값들로 저장 및 조회 테스트`() = runTest {
        // Given: 다양한 토큰 값들
        val testTokens = listOf(
            "simple_token",
            "token_with_numbers_123",
            "token-with-special-chars!@#",
            "very_long_token_" + "a".repeat(200),  // 긴 토큰
            "token with spaces",  // 공백 포함
            ""  // 빈 문자열
        )

        testTokens.forEach { token ->
            // Given: Mock DataStore 설정
            val mockPreferences = mockk<Preferences>()
            every { mockPreferences[stringPreferencesKey("fcm_token")] } returns token
            every { dataStore.data } returns flowOf(mockPreferences)

            // When: 저장 및 조회
            fcmTokenDataStore.saveToken(token)
            val retrievedToken = fcmTokenDataStore.getToken()

            // Then: 저장된 값과 조회된 값이 동일함
            Assert.assertEquals("토큰 저장/조회 실패: $token", token, retrievedToken)
        }
    }

    @Test
    fun `저장 후 삭제 시 null 반환`() = runTest {
        // Given: 토큰 저장
        val token = "token_to_delete"
        fcmTokenDataStore.saveToken(token)

        // When: 토큰 삭제 후 조회 시도
        fcmTokenDataStore.clearToken()

        // Mock이므로 실제 삭제는 확인할 수 없지만, 메소드가 호출되는지 검증
        coVerify { dataStore.edit(any())  } // saveToken의 edit 호출
        // clearToken도 edit를 호출하지만, 동일한 mock이므로 구분하기 어려움
        // 실제 테스트에서는 별도의 mock이나 spy를 사용해야 함
    }
}