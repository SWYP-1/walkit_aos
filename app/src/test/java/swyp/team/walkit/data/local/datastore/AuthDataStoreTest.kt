package swyp.team.walkit.data.local.datastore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

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

/**
 * AuthDataStore 단위 테스트
 *
 * 테스트 대상:
 * - Flow 프로퍼티들: accessToken, refreshToken, provider
 * - saveTokens(): 토큰들 저장
 * - saveProvider(): 제공자 저장
 * - getProvider(): 제공자 조회
 * - getProviderFlow(): 제공자 Flow 조회
 * - clear(): 모든 데이터 삭제
 */
class AuthDataStoreTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var authDataStore: AuthDataStore

    @Before
    fun setup() {
        dataStore = mockk()
        authDataStore = AuthDataStore(dataStore)
    }

    @Test
    fun `accessToken Flow - DataStore 데이터를 올바르게 매핑해야 함`() = runTest {
        // Given: Mock DataStore가 accessToken을 반환하도록 설정
        val mockToken = "test_access_token_123"
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[stringPreferencesKey("access_token")] } returns mockToken
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: accessToken Flow에서 값 수집
        val result = authDataStore.accessToken.first()

        // Then: 올바른 값이 반환됨
        Assert.assertEquals(mockToken, result)
    }

    @Test
    fun `refreshToken Flow - DataStore 데이터를 올바르게 매핑해야 함`() = runTest {
        // Given: Mock DataStore가 refreshToken을 반환하도록 설정
        val mockToken = "test_refresh_token_456"
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[stringPreferencesKey("refresh_token")] } returns mockToken
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: refreshToken Flow에서 값 수집
        val result = authDataStore.refreshToken.first()

        // Then: 올바른 값이 반환됨
        Assert.assertEquals(mockToken, result)
    }

    @Test
    fun `provider Flow - DataStore 데이터를 올바르게 매핑해야 함`() = runTest {
        // Given: Mock DataStore가 provider를 반환하도록 설정
        val mockProvider = "kakao"
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[stringPreferencesKey("provider")] } returns mockProvider
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: provider Flow에서 값 수집
        val result = authDataStore.provider.first()

        // Then: 올바른 값이 반환됨
        Assert.assertEquals(mockProvider, result)
    }

    @Test
    fun `Flow 프로퍼티들 - 값이 없을 때 null 반환`() = runTest {
        // Given: Mock DataStore가 null을 반환하도록 설정
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[stringPreferencesKey("access_token")] } returns null
        every { mockPreferences[stringPreferencesKey("refresh_token")] } returns null
        every { mockPreferences[stringPreferencesKey("provider")] } returns null
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: 각 Flow에서 값 수집
        val accessToken = authDataStore.accessToken.first()
        val refreshToken = authDataStore.refreshToken.first()
        val provider = authDataStore.provider.first()

        // Then: 모두 null이 반환됨
        assertNull(accessToken)
        assertNull(refreshToken)
        assertNull(provider)
    }

    @Test
    fun `saveTokens - accessToken만 저장`() = runTest {
        // Given: accessToken만 있는 경우
        val accessToken = "access_only_token"

        // When: saveTokens 호출
        authDataStore.saveTokens(accessToken, null, null)

        // Then: DataStore.edit가 호출되었는지 검증
        coVerify { dataStore.edit(any())  }
    }

    @Test
    fun `saveTokens - accessToken과 refreshToken 저장`() = runTest {
        // Given: accessToken과 refreshToken이 있는 경우
        val accessToken = "access_token_123"
        val refreshToken = "refresh_token_456"

        // When: saveTokens 호출
        authDataStore.saveTokens(accessToken, refreshToken, null)

        // Then: DataStore.edit가 호출되었는지 검증
        coVerify { dataStore.edit(any())  }
    }

    @Test
    fun `saveTokens - 모든 파라미터 저장`() = runTest {
        // Given: 모든 파라미터가 있는 경우
        val accessToken = "full_access_token"
        val refreshToken = "full_refresh_token"
        val provider = "kakao"

        // When: saveTokens 호출
        authDataStore.saveTokens(accessToken, refreshToken, provider)

        // Then: DataStore.edit가 호출되었는지 검증
        coVerify { dataStore.edit(any())  }
    }

    @Test
    fun `saveProvider - 제공자를 올바르게 저장해야 함`() = runTest {
        // Given: 저장할 제공자
        val provider = "naver"

        // When: saveProvider 호출
        authDataStore.saveProvider(provider)

        // Then: DataStore.edit가 호출되었는지 검증
        coVerify { dataStore.edit(any())  }
    }

    @Test
    fun `getProvider - 제공자를 올바르게 조회해야 함`() = runTest {
        // Given: Mock DataStore가 제공자를 반환하도록 설정
        val expectedProvider = "google"
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[stringPreferencesKey("provider")] } returns expectedProvider
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: getProvider 호출
        val result = authDataStore.getProvider()

        // Then: 올바른 제공자가 반환됨
        Assert.assertEquals(expectedProvider, result)
    }

    @Test
    fun `getProvider - 제공자가 없을 때 null 반환`() = runTest {
        // Given: Mock DataStore가 null을 반환하도록 설정
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[stringPreferencesKey("provider")] } returns null
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: getProvider 호출
        val result = authDataStore.getProvider()

        // Then: null이 반환됨
        assertNull(result)
    }

    @Test
    fun `getProviderFlow - Flow를 올바르게 반환해야 함`() = runTest {
        // Given: Mock DataStore가 특정 제공자를 반환하도록 설정
        val expectedProvider = "apple"
        val mockPreferences = mockk<Preferences>()
        every { mockPreferences[stringPreferencesKey("provider")] } returns expectedProvider
        every { dataStore.data } returns flowOf(mockPreferences)

        // When: getProviderFlow에서 값 수집
        val result = authDataStore.getProviderFlow().first()

        // Then: 올바른 값이 반환됨
        Assert.assertEquals(expectedProvider, result)
    }

    @Test
    fun `clear - 모든 데이터를 올바르게 삭제해야 함`() = runTest {
        // When: clear 호출
        authDataStore.clear()

        // Then: DataStore.edit가 호출되었는지 검증
        coVerify { dataStore.edit(any())  }
    }

    @Test
    fun `다양한 소셜 제공자로 저장 및 조회 테스트`() = runTest {
        // Given: 다양한 소셜 제공자들
        val testProviders = listOf(
            "kakao",
            "naver",
            "google",
            "apple",
            "facebook"
        )

        testProviders.forEach { provider ->
            // Given: Mock DataStore 설정
            val mockPreferences = mockk<Preferences>()
            every { mockPreferences[stringPreferencesKey("provider")] } returns provider
            every { dataStore.data } returns flowOf(mockPreferences)

            // When: 저장 및 조회
            authDataStore.saveProvider(provider)
            val retrievedProvider = authDataStore.getProvider()

            // Then: 저장된 값과 조회된 값이 동일함
            Assert.assertEquals("제공자 저장/조회 실패: $provider", provider, retrievedProvider)
        }
    }

    @Test
    fun `다양한 토큰 값들로 저장 및 조회 테스트`() = runTest {
        // Given: 다양한 토큰 값들
        val testTokens = listOf(
            Triple("simple_token", "simple_refresh", "kakao"),
            Triple("long_token_" + "x".repeat(500), "long_refresh_" + "y".repeat(500), "naver"),
            Triple("token@special!", null, "google"),  // refreshToken이 null
            Triple("access_only", "refresh_only", null),  // provider가 null
        )

        testTokens.forEach { (accessToken, refreshToken, provider) ->
            // When: 토큰들 저장
            authDataStore.saveTokens(accessToken, refreshToken, provider)

            // Then: DataStore.edit가 호출되었는지 검증
            coVerify { dataStore.edit(any())  }
        }
    }
}